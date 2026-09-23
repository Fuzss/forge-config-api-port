package fuzs.multiloader.vendoredsources

import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipFile

enum class SyncMode { GENERATED, OWNED, LOCAL }

data class ManifestEntry(val mode: SyncMode, val source: String?, val path: String)

/**
 * A single upstream source that vendored files are copied from.
 *
 * @param name        unique name used by manifest entries to reference this source
 * @param version     upstream version, recorded in the lock file
 * @param packageRoot dot-form package root of the source (e.g. `net.neoforged.neoforge`)
 * @param relocateTo  dot-form package the source is relocated to, or `null` to keep the original package
 * @param sourcesJar  the upstream sources jar to copy files from
 */
data class SourceSpec(
    val name: String,
    val version: String,
    val packageRoot: String,
    val relocateTo: String?,
    val sourcesJar: File,
) {
    val packageRootPath: String = packageRoot.replace('.', '/')

    /**
     * {@return the path a source file is written to, relative to the output source directory}
     */
    fun outputPath(sourcePath: String): String {
        val subPath = sourcePath.removePrefix("$packageRootPath/")
        return if (relocateTo != null) "${relocateTo.replace('.', '/')}/$subPath" else sourcePath
    }
}

object VendoredSources {
    fun parseManifest(file: File): List<ManifestEntry> {
        return file.readLines()
            .map { it.substringBefore('#').trim() }
            .filter { it.isNotEmpty() }
            .map { line ->
                val parts = line.split(Regex("\\s+"))
                when (parts[0].lowercase()) {
                    "generated" -> {
                        require(parts.size == 3) { "Invalid manifest line (expected 'generated <source> <path>'): '$line'" }
                        ManifestEntry(SyncMode.GENERATED, parts[1], parts[2])
                    }

                    "owned" -> {
                        require(parts.size == 3) { "Invalid manifest line (expected 'owned <source> <path>'): '$line'" }
                        ManifestEntry(SyncMode.OWNED, parts[1], parts[2])
                    }

                    "local" -> {
                        require(parts.size == 2) { "Invalid manifest line (expected 'local <path>'): '$line'" }
                        ManifestEntry(SyncMode.LOCAL, null, parts[1])
                    }

                    else -> error("Unknown sync mode '${parts[0]}' in: '$line'")
                }
            }
    }

    fun extractSource(sourcesJar: File, packageRootPath: String, targetDir: File) {
        require(sourcesJar.isFile) { "Vendored sources jar not found: $sourcesJar" }
        val prefix = "$packageRootPath/"
        targetDir.mkdirs()
        ZipFile(sourcesJar).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.isDirectory || !entry.name.startsWith(prefix)) continue
                val out = File(targetDir, entry.name)
                out.parentFile.mkdirs()
                zip.getInputStream(entry).use { input -> out.outputStream().use { input.copyTo(it) } }
            }
        }
    }

    /**
     * Builds the `old FQN -> new FQN` map for every relocated class in the manifest. The map spans all sources, so
     * references between sources (e.g. a `neoforge` file importing an `fml` class) are rewritten as well. Classes
     * that are not part of the manifest (e.g. shared classes left in place) are never touched.
     */
    fun relocationMap(entries: List<ManifestEntry>, sourcesByName: Map<String, SourceSpec>): Map<String, String> {
        val map = linkedMapOf<String, String>()
        for (entry in entries) {
            val source = entry.source?.let { sourcesByName[it] } ?: continue
            if (source.relocateTo == null) continue
            map[fqn(entry.path)] = fqn(source.outputPath(entry.path))
        }
        return map
    }

    private fun fqn(path: String): String = path.removeSuffix(".java").replace('/', '.')

    /**
     * Rewrites a file's own `package` declaration from its upstream package to its relocated package.
     */
    fun relocatePackage(text: String, sourcePath: String, outputPath: String): String {
        val oldPackage = sourcePath.substringBeforeLast('/').replace('/', '.')
        val newPackage = outputPath.substringBeforeLast('/').replace('/', '.')
        if (oldPackage == newPackage) return text
        return text.replace("package $oldPackage;", "package $newPackage;")
    }

    fun applyRelocation(text: String, map: Map<String, String>): String {
        var result = text
        for ((old, new) in map.entries.sortedByDescending { it.key.length }) {
            result = replaceToken(result, old, new)
        }
        return result
    }

    fun containsToken(text: String, token: String): Boolean {
        var index = 0
        while (true) {
            val found = text.indexOf(token, index)
            if (found < 0) return false
            val end = found + token.length
            if (hasTokenBoundaries(text, found, end)) return true
            index = end
        }
    }

    private fun replaceToken(text: String, old: String, new: String): String {
        var index = 0
        val result = StringBuilder(text.length)
        while (true) {
            val found = text.indexOf(old, index)
            if (found < 0) {
                result.append(text, index, text.length)
                return result.toString()
            }
            val end = found + old.length
            result.append(text, index, found)
            if (hasTokenBoundaries(text, found, end)) result.append(new) else result.append(text, found, end)
            index = end
        }
    }

    private fun hasTokenBoundaries(text: String, start: Int, end: Int): Boolean {
        val before = start == 0 || !isIdentifierPart(text[start - 1])
        val after = end >= text.length || !isIdentifierPart(text[end])
        return before && after
    }

    private fun isIdentifierPart(c: Char): Boolean = c.isLetterOrDigit() || c == '_' || c == '$'

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

object VendoredSourcesLock {
    private const val VERSION_PREFIX = "version."

    fun read(file: File): Map<String, String> {
        if (!file.isFile) return emptyMap()
        return file.readLines()
            .map { it.substringBefore('#').trim() }
            .filter { it.contains('=') }
            .associate { val index = it.indexOf('='); it.substring(0, index).trim() to it.substring(index + 1).trim() }
    }

    fun versions(entries: Map<String, String>): Map<String, String> =
        entries.filterKeys { it.startsWith(VERSION_PREFIX) }.mapKeys { it.key.removePrefix(VERSION_PREFIX) }

    fun ownedHashes(entries: Map<String, String>): Map<String, String> =
        entries.filterKeys { !it.startsWith(VERSION_PREFIX) }

    fun write(file: File, versions: Map<String, String>, ownedHashes: Map<String, String>) {
        file.parentFile.mkdirs()
        file.writeText(buildString {
            appendLine("# Generated by the vendored-sources sync tasks. Do not edit.")
            appendLine("# 'version.*' pins each source; all other entries are upstream hashes of 'owned' files.")
            versions.toSortedMap().forEach { (source, version) -> appendLine("$VERSION_PREFIX$source=$version") }
            ownedHashes.toSortedMap().forEach { (path, hash) -> appendLine("$path=$hash") }
        })
    }
}

data class ExecOutcome(val exitValue: Int, val output: String)

class VendoredSourcesGenerator(private val exec: (File, List<String>) -> ExecOutcome) {
    data class FileOutcome(
        val path: String,
        val source: String?,
        val mode: SyncMode,
        val patched: Boolean,
        val patchOutput: String = "",
    )

    data class Result(val outcomes: List<FileOutcome>, val ownedHashes: Map<String, String>)

    fun generate(
        manifestFile: File,
        patchesDir: File,
        outputDir: File,
        workDir: File,
        sources: List<SourceSpec>,
        reportDir: File?,
    ): Result {
        val sourcesByName = sources.associateBy { it.name }
        val entries = VendoredSources.parseManifest(manifestFile)
        entries.forEach { entry ->
            entry.source?.let { name ->
                require(sourcesByName.containsKey(name)) {
                    "Unknown source '$name' for manifest entry '${entry.path}'"
                }
            }
        }

        val relocation = VendoredSources.relocationMap(entries, sourcesByName)

        // Only extract sources that are actually referenced by the manifest.
        val usedSourceNames = entries.mapNotNull { it.source }.toSet()
        val upstreamDirs = sources.filter { it.name in usedSourceNames }.associate { source ->
            val dir = File(workDir, "upstream/${source.name}")
            dir.deleteRecursively()
            VendoredSources.extractSource(source.sourcesJar, source.packageRootPath, dir)
            source.name to dir
        }
        val stagingDir = File(workDir, "staging")
        stagingDir.deleteRecursively()

        val ownedHashes = sortedMapOf<String, String>()
        val outcomes = mutableListOf<FileOutcome>()
        for (entry in entries) {
            when (entry.mode) {
                SyncMode.LOCAL -> outcomes += FileOutcome(entry.path, null, entry.mode, patched = false)

                SyncMode.OWNED -> {
                    val source = sourcesByName.getValue(requireNotNull(entry.source))
                    val upstream = File(upstreamDirs.getValue(source.name), entry.path)
                    require(upstream.isFile) {
                        "Missing upstream file for owned entry '${entry.path}' in source '${source.name}'"
                    }
                    ownedHashes[entry.path] = VendoredSources.sha256(upstream)
                    reportDir?.let { dir ->
                        val dest = File(dir, entry.path)
                        dest.parentFile.mkdirs()
                        upstream.copyTo(dest, overwrite = true)
                    }
                    outcomes += FileOutcome(entry.path, source.name, entry.mode, patched = false)
                }

                SyncMode.GENERATED -> {
                    val source = sourcesByName.getValue(requireNotNull(entry.source))
                    val upstream = File(upstreamDirs.getValue(source.name), entry.path)
                    require(upstream.isFile) {
                        "Missing upstream file for generated entry '${entry.path}' in source '${source.name}'"
                    }
                    val staged = File(stagingDir, entry.path)
                    staged.parentFile.mkdirs()
                    upstream.copyTo(staged, overwrite = true)

                    val patch = File(patchesDir, "${entry.path}.patch")
                    var patched = false
                    var patchOutput = ""
                    if (patch.isFile) {
                        patched = true
                        val outcome = exec(stagingDir, listOf("patch", "-p1", "-i", patch.absolutePath))
                        patchOutput = outcome.output
                        check(outcome.exitValue == 0) {
                            "Failed to apply patch '${patch.name}' (patch exit code ${outcome.exitValue}).\n" +
                                patchOutput.trim() + "\n" +
                                "Resolve the conflict and update the patch."
                        }
                    }

                    val outputPath = source.outputPath(entry.path)
                    val relocated = VendoredSources.applyRelocation(
                        VendoredSources.relocatePackage(staged.readText(), entry.path, outputPath),
                        relocation,
                    )
                    if (source.relocateTo != null) {
                        relocation.keys.firstOrNull { VendoredSources.containsToken(relocated, it) }?.let { stale ->
                            error(
                                "Generated file '${entry.path}' still references relocated class '$stale'. " +
                                    "The patch must remove or replace every reference to it."
                            )
                        }
                    }

                    val out = File(outputDir, outputPath)
                    out.parentFile.mkdirs()
                    out.writeText(relocated)
                    outcomes += FileOutcome(entry.path, source.name, entry.mode, patched, patchOutput)
                }
            }
        }
        return Result(outcomes, ownedHashes)
    }
}
