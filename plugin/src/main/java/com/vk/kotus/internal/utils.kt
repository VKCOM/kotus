package com.vk.kotus.internal

import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlScalar
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.initialization.Settings

private val camelRegex = "([a-z])([A-Z]+)".toRegex()

internal fun String.convertToKebabCase(): String {
    return this.replace(camelRegex, "$1-$2")
        .lowercase()
}

internal fun Collection<String>.toModuleDescriptor(): List<ModuleDescriptor> = map {
    val config = it.split(";", limit = 3)
    ModuleDescriptor(config.first(), config.getOrNull(1).orEmpty(), config.getOrNull(2).orEmpty())
}

internal fun ProjectDescriptor.toModuleDescriptor(settings: Settings) =
    ModuleDescriptor(path, projectDir.relativeTo(settings.rootDir).path, buildFileName)

internal fun YamlList?.toStringSet(): Set<String> {
    return this?.items?.filterIsInstance<YamlScalar>()?.map { it.content }.orEmpty().toSet()
}