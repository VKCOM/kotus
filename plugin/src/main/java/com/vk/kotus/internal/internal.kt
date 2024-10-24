@file:Suppress("NestedBlockDepth") // expected

package com.vk.kotus.internal

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlScalar
import com.charleskorn.kaml.yamlList
import com.charleskorn.kaml.yamlMap
import com.vk.kotus.KotusConfigurationImpl
import com.vk.kotus.KotusExtension
import com.vk.kotus.KotusRulesImpl
import org.gradle.api.initialization.ProjectDescriptor
import org.gradle.api.initialization.Settings
import org.gradle.api.internal.FeaturePreviews
import org.gradle.api.provider.Property
import org.gradle.initialization.DefaultProjectDescriptorRegistry
import org.gradle.initialization.DefaultSettings
import org.gradle.internal.buildoption.FeatureFlags
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.support.serviceOf
import java.io.File

internal fun Settings.kotusInternal() {
    val extension = extensions.getByType<KotusExtension>()

    val settings = this as? DefaultSettings ?: return
    val projectDescriptorRegistry =
        settings.projectDescriptorRegistry as? DefaultProjectDescriptorRegistry ?: return

    val projects = projectDescriptorRegistry.allProjects.sortedWith(ProjectDescriptorComparator())
    val rootProject = projectDescriptorRegistry.rootProject?.apply { children().clear() }

    projects.reversed().forEach { project ->
        if (project.path != rootProject?.path) {
            projectDescriptorRegistry.removeProject(project.path)
        }
    }

    modifyKotus {
        modules += projects.associateBy({ it.path }, { it.toModuleDescriptor(this@kotusInternal) })

        isEnabled = extension.enabled.getOrElse(true)
        isVerbose = extension.verbose.getOrElse(true)

        if (isEnabled) {
            if (serviceOf<FeatureFlags>().isEnabled(FeaturePreviews.Feature.TYPESAFE_PROJECT_ACCESSORS)) {
                projectRegexes += typeSafeAccessorProjectRegexes
            }
            parseRules(extension.rulesPath, extension.rulesImpl)
            parseConfiguration(extension.configurationPath, extension.configurationImpl)

            projectRegexes += extension.rulesImpl.projectRegexes
            aliasedModules += extension.rulesImpl.aliasedModules
            modulesByRegex += extension.rulesImpl.modulesByRegex

            focusEnabled = focusEnabled || extension.configurationImpl.focusEnabled
            replaceEnabled = replaceEnabled || extension.configurationImpl.replaceEnabled
            focusModules += extension.configurationImpl.focusModules
            replaceModules += extension.configurationImpl.replaceModules
        }
    }

    applyConfiguration()
}

private fun Settings.parseRules(path: Property<String>, rulesImpl: KotusRulesImpl) {
    val rulesFile = searchRulesFile(path) ?: run {
        kotusConfiguration.log { "Can't found kotus rules file" }
        return
    }

    val aliasedModules: MutableMap<String, Set<String>> = mutableMapOf()
    val modulesByRegex: MutableList<Pair<String, Set<String>>> = mutableListOf()

    val yaml = Yaml.default.parseToYamlNode(rulesFile.inputStream()).yamlMap

    yaml.entries.forEach { (type, nodes) ->
        when (type.content) {
            "aliases" -> nodes.yamlList.items.forEach { aliasItem ->
                val name = aliasItem.yamlMap.get<YamlScalar>("alias")?.content
                val modules = when (val modulesNode = aliasItem.yamlMap.get<YamlNode>("modules")) {
                    is YamlScalar -> setOf(modulesNode.content)
                    is YamlList -> modulesNode.toStringSet()
                    else -> emptySet()
                }

                if (!name.isNullOrBlank() && modules.isNotEmpty()) aliasedModules[name] = modules
            }

            "regexes" -> nodes.yamlList.items.forEach { regexItem ->
                val regex = regexItem.yamlMap.get<YamlScalar>("regex")?.content
                val modules = regexItem.yamlMap.get<YamlList>("modules").toStringSet()

                if (!regex.isNullOrBlank() && modules.isNotEmpty()) modulesByRegex += regex to modules
            }
        }
    }

    rulesImpl.aliasedModules += aliasedModules
    rulesImpl.modulesByRegex += modulesByRegex
}

private fun Settings.searchRulesFile(path: Property<String>): File? =
    (path.orNull?.let { File(it) } ?: File(rootDir, "kotusRules.yaml"))
        .takeIf { it.exists() }

private fun Settings.parseConfiguration(path: Property<String>, configurationImpl: KotusConfigurationImpl) {
    val configurationFile = searchConfigurationFile(path) ?: run {
        kotusConfiguration.log { "Can't found kotus configuration file" }
        return
    }

    val focusingModules: MutableSet<ModuleDescriptor> = mutableSetOf()
    val replacingWithStubModules: MutableMap<ModuleDescriptor, ModuleDescriptor?> = mutableMapOf()
    var focusingEnabled = false
    var replacingEnabled = false

    val yaml = Yaml.default.parseToYamlNode(configurationFile.inputStream()).yamlMap

    yaml.entries.forEach { (type, nodes) ->
        when (type.content) {
            "focus" -> {
                focusingEnabled = nodes.yamlMap.get<YamlScalar>("enabled")?.toBoolean()  == true
                if (focusingEnabled) {
                    val modules = when (val modulesNode = nodes.yamlMap.get<YamlNode>("modules")) {
                        is YamlScalar -> setOf(modulesNode.content)
                        is YamlList -> modulesNode.toStringSet()
                        else -> emptySet()
                    }
                    modules.toModuleDescriptor().let(focusingModules::addAll)
                }
            }
            "replace" -> {
                replacingEnabled = nodes.yamlMap.get<YamlScalar>("enabled")?.toBoolean() == true
                if (replacingEnabled) {
                    when (val modulesNode = nodes.yamlMap.get<YamlNode>("modules")) {
                        is YamlScalar -> replacingWithStubModules[ModuleDescriptor(modulesNode.content)] = null
                        is YamlList -> modulesNode.items.forEach { item ->
                            when (item) {
                                is YamlMap -> item.entries.forEach { (module, stub) ->
                                    replacingWithStubModules[ModuleDescriptor(module.content)] = ModuleDescriptor((stub as YamlScalar).content)
                                }

                                is YamlScalar -> replacingWithStubModules[ModuleDescriptor(item.content)] = null
                                else -> {}
                            }
                        }
                        else -> {}
                    }
                }

            }
        }
    }

    configurationImpl.focusEnabled = configurationImpl.focusEnabled || focusingEnabled
    configurationImpl.replaceEnabled = configurationImpl.replaceEnabled || replacingEnabled
    configurationImpl.focusModules += focusingModules
    configurationImpl.replaceModules += replacingWithStubModules

}

private fun Settings.searchConfigurationFile(path: Property<String>): File? =
    (path.orNull?.let { File(it) } ?: File(rootDir, "kotusConfiguration.yaml"))
        .takeIf { it.exists() }

internal fun Settings.applyConfiguration() {
    val configuration = kotusConfiguration

    if (configuration.isEnabled && (configuration.focusEnabled || configuration.replaceEnabled)) {

        val alreadyIncluded = mutableSetOf(":")
        configureFocusing(alreadyIncluded)
        configureReplacing(alreadyIncluded)
        configuration.log { "Synced for ${this.rootProject.name} projects count: ${alreadyIncluded.size}" }
    } else {

        val realModules = configuration.modulesStubReplacement.values.map { it.module }
        val alwaysInclude = configuration.alwaysInclude.values
        val modules = configuration.modules.values
        includeModules(modules + alwaysInclude + realModules)
    }
}

internal fun Settings.includeModules(modules: List<ModuleDescriptor>) {
    modules
        .asSequence()
        .distinct()
        .sortedWith(ModulesDescriptorComparator())
        .forEach(::internalInclude)
}

private fun Settings.configureReplacing(alreadyIncluded: MutableSet<String>) {
    val configuration = kotusConfiguration

    if (configuration.replaceEnabled && configuration.replaceModules.isNotEmpty()) {
        val modulesStack =
            configuration.replaceModules.keys.sortedWith(ModulesDescriptorComparator()).toMutableList()
        val usedReplacements = mutableSetOf<ModuleReplacement>()
        while (modulesStack.isNotEmpty()) {
            val module = modulesStack.removeFirst()

            for (moduleName in module.name.generateModuleVariants()) {
                val replacement = configuration.replaceModules[module] ?: configuration.modulesStubReplacement[moduleName]?.stubModule
                if (replacement != null && replacement.name !in alreadyIncluded) {
                    internalInclude(replacement)
                    alreadyIncluded += moduleName
                    modulesStack += findProject(replacement.name)?.projectDependencies(configuration).orEmpty()
                }

                if (module.name !in alreadyIncluded) {
                    internalInclude(module)
                    alreadyIncluded += moduleName
                    modulesStack += findProject(moduleName)?.projectDependencies(configuration).orEmpty()
                }

                if (replacement != null) {
                    usedReplacements += ModuleReplacement(module, replacement)
                }
            }
        }

        if (usedReplacements.isNotEmpty()) {
            gradle.beforeProject {
                allprojects {
                    configurations.all {
                        resolutionStrategy.dependencySubstitution {
                            usedReplacements.forEach { replacement ->
                                substitute(project(replacement.module.name))
                                    .using(project(replacement.stubModule.name))
                            }
                        }
                    }
                }
            }
        }
        configuration.log { "Replaced for ${this.rootProject.name} projects count: ${usedReplacements.size}" }
    }
}

private fun Settings.configureFocusing(alreadyIncluded: MutableSet<String>) {
    val configuration = kotusConfiguration
    val modulesDescriptorComparator = ModulesDescriptorComparator()
    val focusingModules = configuration.focusModules.sortedWith(modulesDescriptorComparator)

    if ((configuration.focusEnabled && focusingModules.isNotEmpty()) || configuration.alwaysInclude.isNotEmpty()) {

        val modulesStack = focusingModules.toMutableList()
        modulesStack += configuration.alwaysInclude.values

        while (modulesStack.isNotEmpty()) {
            val module = modulesStack.removeFirst()
            if (module.name in alreadyIncluded) continue

            for (moduleName in module.name.generateModuleVariants()) {
                val descriptor = configuration.modules[moduleName] ?: configuration.alwaysInclude[moduleName]
                if (descriptor != null) {
                    internalInclude(descriptor)
                    alreadyIncluded += moduleName
                    modulesStack += findProject(moduleName)?.projectDependencies(configuration).orEmpty()
                }
            }
        }
    }
}

internal fun Settings.internalInclude(module: ModuleDescriptor) {
    include(module.name)
    if (module.projectDir.isNotEmpty()) {
        project(module.name).projectDir = File(rootDir, module.projectDir)
    }
    if (module.buildFileName.isNotEmpty()) {
        project(module.name).buildFileName = module.buildFileName
    }
}

internal fun ProjectDescriptor.projectDependenciesNames(configuration: KotusConfiguration): Set<String> {
    var file = File(projectDir, buildFileName)
    if (!file.exists() && buildFileName == "build.gradle") {
        file = File(projectDir, "build.gradle.kts")
    }
    if (!file.exists()) return emptySet()

    return file.bufferedReader().useLines { sequence ->
        sequence.flatMap { line ->
            val projectDeps = configuration.projectRegexes.flatMap { (regex, matcher) ->
                regex.toRegex().findAll(line).map(matcher)
            }
            val conditionalDeps = configuration.aliasedModules.filterKeys(line::contains).values.flatten()
            val depsByPlugins = configuration.modulesByRegex.mapNotNull { (regex, modules) ->
                if (regex.toRegex().containsMatchIn(line)) modules else null
            }.flatten()
            conditionalDeps + projectDeps + depsByPlugins
        }.toSet()
    }
}

private fun Set<String>.retrieveModuleDescriptors(configuration: KotusConfiguration): Set<ModuleDescriptor> =
    mapNotNull { configuration.modules[it] }.toSet()

private fun ProjectDescriptor.projectDependencies(configuration: KotusConfiguration): Set<ModuleDescriptor> =
    projectDependenciesNames(configuration).retrieveModuleDescriptors(configuration)

private fun String.generateModuleVariants(): Set<String> {
    val variants = mutableSetOf<String>()

    val names = split(":")
    for (index in 1..names.size) {
        variants += names.take(index).joinToString(":")
    }
    return variants
}

private fun KotusConfiguration.log(msg: () -> String) {
    if (isVerbose) {
        println("[Kotus] -> ${msg()} ")
    }
}