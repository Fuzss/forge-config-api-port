package fuzs.multiloader


/**
 * Precompiled [multiloader-convention-plugins-neoforge.gradle.kts][fuzs.multiloader.Multiloader_convention_plugins_neoforge_gradle] script plugin.
 *
 * @see fuzs.multiloader.Multiloader_convention_plugins_neoforge_gradle
 */
public
class MultiloaderConventionPluginsNeoforgePlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("fuzs.multiloader.Multiloader_convention_plugins_neoforge_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
