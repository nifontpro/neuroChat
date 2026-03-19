package ru.nb.neurochat.convention

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

internal fun Project.configureAndroidTarget() {
	extensions.configure<KotlinMultiplatformExtension> {
		androidTarget {
			@OptIn(ExperimentalKotlinGradlePluginApi::class)
			compilerOptions {
				jvmTarget.set(JvmTarget.JVM_17)
			}
		}
	}
}

internal fun Project.configureAndroidLibraryTarget() {
   dependencies {
	   "coreLibraryDesugaring"(libs.findLibrary("android-desugarJdkLibs").get())
   }
}
