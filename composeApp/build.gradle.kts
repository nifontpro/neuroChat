import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.convention.cmp.application)
    alias(libs.plugins.convention.buildkonfig)
    alias(libs.plugins.compose.hot.reload)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(projects.core.data)
            implementation(projects.core.presentation)
            implementation(projects.core.designsystem)
            implementation(projects.feature.chat.presentation)

            implementation(libs.jetbrains.compose.runtime)
            implementation(libs.jetbrains.compose.foundation)
            implementation(libs.jetbrains.compose.material3)
            implementation(libs.jetbrains.compose.ui)
            implementation(libs.jetbrains.compose.resources)
            implementation(libs.jetbrains.compose.preview)

            implementation(libs.jetbrains.compose.viewmodel)
            implementation(libs.jetbrains.lifecycle.viewmodel)
            implementation(libs.jetbrains.lifecycle.compose)

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.datastore)
            implementation(libs.datastore.preferences)
        }
        val desktopMain by getting
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

compose.desktop {
    application {
        mainClass = "ru.nb.neurochat.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ru.nb.neurochat"
            packageVersion = "1.0.0"
        }
    }
}
