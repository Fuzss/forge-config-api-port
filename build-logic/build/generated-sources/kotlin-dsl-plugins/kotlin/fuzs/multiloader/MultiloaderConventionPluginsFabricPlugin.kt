package fuzs.multiloader


/**
 * Precompiled [multiloader-convention-plugins-fabric.gradle.kts][fuzs.multiloader.Multiloader_convention_plugins_fabric_gradle] script plugin.
 *
 * @see fuzs.multiloader.Multiloader_convention_plugins_fabric_gradle
 */
public
class MultiloaderConventionPluginsFabricPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("fuzs.multiloader.Multiloader_convention_plugins_fabric_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
