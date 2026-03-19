import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import ru.nb.neurochat.convention.applyHierarchyTemplate
import ru.nb.neurochat.convention.configureAndroidLibraryTarget
import ru.nb.neurochat.convention.configureDesktopTarget
import ru.nb.neurochat.convention.configureIosTargets
import ru.nb.neurochat.convention.libs
import ru.nb.neurochat.convention.pathToPackageName

class CmpApplicationConventionPlugin : Plugin<Project> {

	override fun apply(target: Project) {
		with(target) {

			with(pluginManager) {
				apply("com.android.kotlin.multiplatform.library")
				apply("org.jetbrains.kotlin.multiplatform")
				apply("org.jetbrains.compose")
				apply("org.jetbrains.kotlin.plugin.compose")
				apply("org.jetbrains.kotlin.plugin.serialization")
			}

			configureAndroidLibraryTarget()
			configureIosTargets()
			configureDesktopTarget()

			extensions.configure<KotlinMultiplatformExtension> {
				extensions.configure<KotlinMultiplatformAndroidLibraryExtension> {
					minSdk = 26
					compileSdk = 36
					namespace = pathToPackageName()
				}

				applyHierarchyTemplate()

				compilerOptions {
					freeCompilerArgs.add("-Xexpect-actual-classes")
					freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
				}
			}

			dependencies {
				"androidMainImplementation"(libs.findLibrary("androidx-compose-ui-tooling").get())
			}
		}
	}
}
