import com.codingfeline.buildkonfig.compiler.FieldSpec
import com.codingfeline.buildkonfig.gradle.BuildKonfigExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import ru.nb.neurochat.convention.pathToPackageName
import java.util.Properties

class BuildKonfigConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.codingfeline.buildkonfig")
            }

            extensions.configure<BuildKonfigExtension> {
                packageName = target.pathToPackageName()
                defaultConfigs {
                    val litellmFile = rootProject.file("litellm.properties")
                    val props = Properties()
                    if (litellmFile.exists()) litellmFile.inputStream().use(props::load)

                    buildConfigField(
                        FieldSpec.Type.STRING, "API_KEY",
                        props.getProperty("litellm.apiKey", "")
                    )
                    buildConfigField(
                        FieldSpec.Type.STRING, "BASE_URL",
                        props.getProperty("litellm.baseUrl", "")
                    )
                    buildConfigField(
                        FieldSpec.Type.STRING, "MODEL",
                        props.getProperty("litellm.model", "gpt-4o")
                    )
                    buildConfigField(
                        FieldSpec.Type.STRING, "TIMEOUT_SECONDS",
                        props.getProperty("litellm.timeoutSeconds", "300")
                    )
                }
            }
        }
    }
}
