package com.vk.kotus.internal

import org.gradle.api.initialization.Settings
import java.util.concurrent.ConcurrentHashMap

internal val defaultProjectRegexes = mutableMapOf<String, (MatchResult) -> String>(
    "project\\(\"([^\"]+)\"" to { matches -> matches.groupValues[1] },
    "project\\(\'([^\']+)\'" to { matches -> matches.groupValues[1] },
)

val typeSafeAccessorProjectRegexes = mutableMapOf<String, (MatchResult) -> String>(
    "\\(projects\\.([^\"]+)\\)" to { matches -> ":" + matches.groupValues[1].replace(".", ":").convertToKebabCase() },
)

private val kotusConfigurations = ConcurrentHashMap<Settings, KotusConfiguration>()

internal val Settings.kotusConfiguration
    get() = kotusConfigurations.getOrPut(this, ::KotusConfiguration)

internal fun Settings.modifyKotus(block: KotusConfiguration.() -> Unit) {
    block(kotusConfiguration)
}

internal class KotusConfiguration(
    var isEnabled: Boolean = true,
    var isVerbose: Boolean = true,

    // data
    val modules: MutableMap<String, ModuleDescriptor> = mutableMapOf(),
    val alwaysInclude: MutableMap<String, ModuleDescriptor> = mutableMapOf(),
    val modulesStubReplacement: MutableMap<String, ModuleReplacement> = mutableMapOf(),

    // rules
    val projectRegexes: MutableMap<String, (MatchResult) -> String> = defaultProjectRegexes,
    val aliasedModules: MutableMap<String, Set<String>> = mutableMapOf(),
    val modulesByRegex: MutableList<Pair<String, Set<String>>> = mutableListOf(),

    // configuration
    val focusingModules: MutableSet<ModuleDescriptor> = mutableSetOf(),
    val replacingWithStubModules: MutableMap<ModuleDescriptor, ModuleDescriptor?> = mutableMapOf(),
)
