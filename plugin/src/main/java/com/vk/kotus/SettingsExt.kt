package com.vk.kotus

import com.vk.kotus.internal.ModuleDescriptor
import com.vk.kotus.internal.ModuleReplacement
import com.vk.kotus.internal.kotusConfiguration
import com.vk.kotus.internal.kotusInternal
import org.gradle.api.initialization.Settings

fun Settings.kotusIncludeWithStub(
    module: String, stubModule: String,
    moduleProjectDir: String = "", stubModuleProjectDir: String = "",
    moduleBuildFileName: String = "", stubBuildFileName: String = ""
) {
    kotusConfiguration.modulesStubReplacement[module] = ModuleReplacement(
        ModuleDescriptor(module, moduleProjectDir, moduleBuildFileName),
        ModuleDescriptor(stubModule, stubModuleProjectDir, stubBuildFileName)
    )
}

fun Settings.kotusInclude(module: String, projectDir: String = "", buildFileName: String = "") {
    kotusConfiguration.modules[module] = ModuleDescriptor(module, projectDir, buildFileName)
}

fun Settings.alwaysInclude(module: String, projectDir: String = "", buildFileName: String = "") {
    kotusConfiguration.alwaysInclude[module] = ModuleDescriptor(module, projectDir, buildFileName)
}

fun Settings.kotusPocus() = kotusInternal()