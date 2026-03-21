import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import ru.nb.neurochat.convention.configureAndroidLibraryTarget
import ru.nb.neurochat.convention.libs

class CmpLibraryConventionPlugin : Plugin<Project> {

	override fun apply(target: Project) {
		with(target) {
			with(pluginManager) {
				apply("ru.nb.neurochat.convention.kmp.library")
				apply("org.jetbrains.kotlin.plugin.compose")
				apply("org.jetbrains.compose")
			}

			configureAndroidLibraryTarget()

			dependencies {
				"commonMainImplementation"(libs.findLibrary("jetbrains-compose-ui").get())
				"commonMainImplementation"(libs.findLibrary("jetbrains-compose-foundation").get())
				"commonMainImplementation"(libs.findLibrary("jetbrains-compose-material3").get())
				"commonMainImplementation"(libs.findLibrary("jetbrains-compose-material-icons-core").get())
				"commonMainImplementation"(libs.findLibrary("jetbrains-compose-resources").get())

				"androidMainImplementation"(libs.findLibrary("androidx-compose-ui-tooling").get())
			}
		}
	}
}
