package fuzs.multiloader.classtweaker

import fuzs.multiloader.extension.commonProject
import fuzs.multiloader.extension.mod
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import java.io.File

val TRANSITIVE_CLASS_TWEAKER_ACCESS_LEVELS: Map<String, String> = mapOf(
    "transitive-accessible" to "public",
    "transitive-extendable" to "public-f",
    "transitive-mutable" to "public-f",
    "transitive-inject-interface" to "",
    "transitive-extend-enum" to ""
)
val CLASS_TWEAKER_ACCESS_LEVELS: Map<String, String> = mapOf(
    "accessible" to "public",
    "extendable" to "public-f",
    "mutable" to "public-f",
    "inject-interface" to "",
    "extend-enum" to ""
) + TRANSITIVE_CLASS_TWEAKER_ACCESS_LEVELS
private const val COLUMN_SEPARATOR: String = " "
private val CLASS_TWEAKER_HEADER: String = listOf("classTweaker", "v2", "official").joinToString(COLUMN_SEPARATOR)

val Project.classTweakerFile: File
    get() = project.commonProject.file("src/main/resources/${mod.id}.classtweaker")

val Project.generatedClassTweakerFile: Provider<RegularFile>
    get() = project.layout.buildDirectory.file("generated/resources/${mod.id}.classtweaker")

val Project.generatedAccessTransformerFile: Provider<RegularFile>
    get() = project.layout.buildDirectory.file("generated/resources/META-INF/accesstransformer.cfg")

val Project.generatedTransitiveAccessTransformerFile: Provider<RegularFile>
    get() = project.layout.buildDirectory.file("generated/accesstransformers/${SourceSet.MAIN_SOURCE_SET_NAME}/accesstransformer.cfg")

fun generateClassTweakerFile(inputFile: File, outputFile: File) {
    generateClassTweakerFile(inputFile, outputFile) { lines ->
        lines.map { line ->
            line.replace(Regex("\\s+"), COLUMN_SEPARATOR)
        }
            .toMutableList().also { lines ->
                lines.addFirst(CLASS_TWEAKER_HEADER)
            }
    }
}

fun generateAccessTransformerFile(
    inputFile: File,
    outputFile: File,
    accessLevels: Map<String, String> = CLASS_TWEAKER_ACCESS_LEVELS
) {
    generateClassTweakerFile(inputFile, outputFile, accessLevels) { lines ->
        lines.mapNotNull { line ->
            val entry = line.split(Regex("\\s+"))

            if (entry.size < 3) error("Invalid entry: $line")

            val modifier = accessLevels[entry[0]] ?: error("Invalid entry: $line")

            if (modifier.isBlank()) {
                return@mapNotNull null
            }

            val type = entry[1]
            val owner = entry[2].replace('/', '.')

            when (type) {
                "class" -> {
                    check(entry.size == 3) { "Invalid entry: $line" }
                    listOf(modifier, owner)
                }

                "field" -> {
                    check(entry.size == 5) { "Invalid entry: $line" }
                    val name = entry[3]
                    listOf(modifier, owner, name)
                }

                "method" -> {
                    check(entry.size == 5) { "Invalid entry: $line" }
                    val name = entry[3]
                    val desc = entry[4]
                    listOf(modifier, owner, "$name$desc")
                }

                else -> {
                    error("Invalid entry: $line")
                }
            }
        }
            .map { lines ->
                lines.joinToString(COLUMN_SEPARATOR)
            }
            .toList()
    }
}

private fun generateClassTweakerFile(
    inputFile: File,
    outputFile: File,
    accessLevels: Map<String, String> = CLASS_TWEAKER_ACCESS_LEVELS,
    transformer: (Sequence<String>) -> List<String>
) {
    val lines = inputFile.readLines()
        .asSequence()
        .map { line ->
            val index = line.indexOf('#')
            if (index != -1) line.substring(0, index) else line
        }
        .map(String::trim)
        .filter(String::isNotEmpty)
        .filter { line ->
            accessLevels.keys.any(line::startsWith)
        }

    val text = transformer.invoke(lines).joinToString("\n")
    outputFile.parentFile.mkdirs()
    outputFile.writeText(text)
}
