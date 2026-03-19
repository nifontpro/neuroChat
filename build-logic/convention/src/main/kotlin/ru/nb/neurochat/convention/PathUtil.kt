package ru.nb.neurochat.convention

import org.gradle.api.Project
import java.util.Locale

fun Project.pathToPackageName(): String {
	val relativePackageName = path
		.replace(':', '.')
		.lowercase()

	return "ru.nb.neurochat$relativePackageName"
}

fun Project.pathToResourcePrefix(): String {
	return path
		.replace(':', '_')
		.lowercase()
		.drop(1) + "_"
}

fun Project.pathToFrameworkName(): String {
	val parts = this.path.split(":", "-", "_", " ")
	return parts.joinToString("") { part ->
		part.replaceFirstChar {
			it.titlecase(Locale.ROOT)
		}
	}
}
