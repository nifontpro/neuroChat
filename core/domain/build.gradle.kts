plugins {
    id("neurochat.kmp-library")
}

android {
    namespace = "ru.nb.neurochat.core.domain"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
