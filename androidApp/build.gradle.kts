plugins {
    alias(libs.plugins.convention.android.application.compose)
}

dependencies {
    implementation(projects.composeApp)
    implementation(projects.core.data)

    implementation(libs.androidx.activity.compose)
    implementation(libs.core.splashscreen)
    implementation(libs.koin.android)
    implementation(libs.datastore)
    implementation(libs.datastore.preferences)
}
