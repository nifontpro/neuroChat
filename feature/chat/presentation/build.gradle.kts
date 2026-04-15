plugins {
    alias(libs.plugins.convention.cmp.feature)
}

compose.resources {
    packageOfResClass = "ru.nb.neurochat.chat.presentation.generated.resources"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(projects.core.data)

            implementation(libs.material3.adaptive)
            implementation(libs.material3.adaptive.layout)
            implementation(libs.material3.adaptive.navigation)

            implementation(libs.jetbrains.compose.preview)
        }
    }
}
