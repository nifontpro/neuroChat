import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	`kotlin-dsl`
}

group = "ru.nb.neurochat.convention.buildlogic"

dependencies {
	compileOnly(libs.android.gradlePlugin)
	compileOnly(libs.android.tools.common)
	compileOnly(libs.kotlin.gradlePlugin)
	compileOnly(libs.compose.gradlePlugin)
	compileOnly(libs.ksp.gradlePlugin)
	implementation(libs.buildkonfig.gradlePlugin)
	implementation(libs.buildkonfig.compiler)
}

java {
	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
	compilerOptions {
		jvmTarget = JvmTarget.JVM_17
	}
}

tasks {
	validatePlugins {
		enableStricterValidation = true
		failOnWarning = true
	}
}

gradlePlugin {
	plugins {
		register("androidApplication") {
			id = "ru.nb.neurochat.convention.android.application"
			implementationClass = "AndroidApplicationConventionPlugin"
		}
		register("androidComposeApplication") {
			id = "ru.nb.neurochat.convention.android.application.compose"
			implementationClass = "AndroidApplicationComposeConventionPlugin"
		}
		register("cmpApplication") {
			id = "ru.nb.neurochat.convention.cmp.application"
			implementationClass = "CmpApplicationConventionPlugin"
		}
		register("kmpLibrary") {
			id = "ru.nb.neurochat.convention.kmp.library"
			implementationClass = "KmpLibraryConventionPlugin"
		}
		register("cmpLibrary") {
			id = "ru.nb.neurochat.convention.cmp.library"
			implementationClass = "CmpLibraryConventionPlugin"
		}
		register("cmpFeature") {
			id = "ru.nb.neurochat.convention.cmp.feature"
			implementationClass = "CmpFeatureConventionPlugin"
		}
		register("buildKonfig") {
			id = "ru.nb.neurochat.convention.buildkonfig"
			implementationClass = "BuildKonfigConventionPlugin"
		}
	}
}
