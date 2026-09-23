package fuzs.multiloader.vendoredsources

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

private fun generator(execOps: ExecOperations): VendoredSourcesGenerator = VendoredSourcesGenerator { dir, command ->
    val output = ByteArrayOutputStream()
    val exit = execOps.exec {
        commandLine(command)
        workingDir = dir
        isIgnoreExitValue = true
        standardOutput = output
        errorOutput = output
    }.exitValue
    ExecOutcome(exit, output.toString(Charsets.UTF_8))
}

abstract class SyncVendoredSourcesTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val manifestFile: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val patchesDir: DirectoryProperty

    @get:Internal
    abstract val outputDir: DirectoryProperty

    @get:Internal
    abstract val workDir: DirectoryProperty

    @get:Internal
    abstract val lockFile: RegularFileProperty

    @get:Internal
    abstract val sources: ListProperty<SourceSpec>

    @get:Inject
    abstract val execOps: ExecOperations

    @TaskAction
    fun sync() {
        val sourceSpecs = sources.get()
        val lock = lockFile.get().asFile
        val previousOwned = VendoredSourcesLock.ownedHashes(VendoredSourcesLock.read(lock))
        val reportDir = File(workDir.get().asFile, "owned-upstream")
        logger.lifecycle("Vendored sources: ${sourceSpecs.joinToString(", ") { "${it.name}@${it.version}" }}")

        val result = generator(execOps).generate(
            manifestFile.get().asFile,
            patchesDir.get().asFile,
            outputDir.get().asFile,
            workDir.get().asFile,
            sourceSpecs,
            reportDir = reportDir,
        )

        val changed = result.ownedHashes
            .filter { (path, hash) -> previousOwned[path] != null && previousOwned[path] != hash }
            .keys

        val pathWidth = result.outcomes.maxOfOrNull { it.path.length } ?: 0
        val sourceWidth = result.outcomes.mapNotNull { it.source?.length }.maxOrNull() ?: 0
        for (outcome in result.outcomes) {
            val tag = when (outcome.mode) {
                SyncMode.GENERATED -> if (outcome.patched) "[patched]" else "[verbatim]"
                SyncMode.OWNED -> if (outcome.path in changed) "[changed]" else "[unchanged]"
                SyncMode.LOCAL -> "[local]"
            }
            logger.lifecycle(
                "  %-9s %s %s %s".format(
                    outcome.mode.name.lowercase(),
                    (outcome.source ?: "-").padEnd(sourceWidth),
                    outcome.path.padEnd(pathWidth),
                    tag,
                )
            )
            if (outcome.patchOutput.isNotBlank()) {
                outcome.patchOutput.trim().lines().forEach { logger.info("      $it") }
            }
        }

        val generated = result.outcomes.count { it.mode == SyncMode.GENERATED }
        val patched = result.outcomes.count { it.mode == SyncMode.GENERATED && it.patched }
        val owned = result.outcomes.count { it.mode == SyncMode.OWNED }
        val local = result.outcomes.count { it.mode == SyncMode.LOCAL }
        logger.lifecycle("$generated generated ($patched patched, ${generated - patched} verbatim), $owned owned, $local local")

        if (changed.isNotEmpty()) {
            logger.warn("Upstream sources changed for 'owned' files; review and port manually:")
            changed.sorted().forEach { path -> logger.warn("  - $path (see ${File(reportDir, path)})") }
        }

        VendoredSourcesLock.write(lock, sourceSpecs.associate { it.name to it.version }, result.ownedHashes)
        logger.lifecycle("Vendored sources synced.")
    }
}

abstract class CheckVendoredSourcesTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val manifestFile: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val patchesDir: DirectoryProperty

    @get:Internal
    abstract val committedDir: DirectoryProperty

    @get:Internal
    abstract val workDir: DirectoryProperty

    @get:Internal
    abstract val lockFile: RegularFileProperty

    @get:Internal
    abstract val sources: ListProperty<SourceSpec>

    @get:Inject
    abstract val execOps: ExecOperations

    @TaskAction
    fun check() {
        val sourceSpecs = sources.get()
        val sourcesByName = sourceSpecs.associateBy { it.name }
        val work = workDir.get().asFile
        work.deleteRecursively()
        val generatedDir = File(work, "output")
        val reportDir = File(work, "owned-upstream")

        val result = generator(execOps).generate(
            manifestFile.get().asFile,
            patchesDir.get().asFile,
            generatedDir,
            File(work, "work"),
            sourceSpecs,
            reportDir = reportDir,
        )

        val problems = mutableListOf<String>()

        for (entry in VendoredSources.parseManifest(manifestFile.get().asFile)) {
            if (entry.mode != SyncMode.GENERATED) continue
            val source = sourcesByName.getValue(requireNotNull(entry.source) { "Missing source for ${entry.path}" })
            val rel = source.outputPath(entry.path)
            val produced = File(generatedDir, rel)
            val committed = File(committedDir.get().asFile, rel)
            if (!committed.isFile || produced.readText() != committed.readText()) {
                problems += "generated source is out of date: ${entry.path}"
            }
        }

        val lock = VendoredSourcesLock.read(lockFile.get().asFile)
        if (lock.isEmpty()) {
            problems += "lock file '${lockFile.get().asFile}' is missing"
        } else {
            val lockedVersions = VendoredSourcesLock.versions(lock)
            sourceSpecs.forEach { source ->
                val locked = lockedVersions[source.name]
                if (locked != source.version) {
                    problems += "lock file targets ${source.name} $locked but ${source.version} is configured"
                }
            }
            VendoredSourcesLock.ownedHashes(lock).forEach { (path, hash) ->
                if (result.ownedHashes[path] != hash) {
                    problems += "owned source changed upstream: $path (review ${File(reportDir, path)})"
                }
            }
            result.ownedHashes.forEach { (path, _) ->
                if (!lock.containsKey(path)) problems += "owned source missing from lock file: $path"
            }
        }

        if (problems.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Vendored sources are out of sync:")
                    problems.forEach { appendLine("  - $it") }
                    appendLine()
                    appendLine("Run the 'syncVendoredSources' task and review the result.")
                }
            )
        }
        logger.lifecycle("Vendored sources are in sync.")
    }
}
