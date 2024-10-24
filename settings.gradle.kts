pluginManagement {
    includeBuild("./plugin")

    dependencyResolutionManagement {
        repositories {
            maven("https://jitpack.io")
        }
    }
}

plugins {
    id("com.vk.kotus") version "1.0"
}

kotus {
    enabled.set(true) // true by default
    verbose.set(true) // print logs to stdout. true by default
    manualRun.set(false) // Call kotusPocus at the end of the settings file. false by default

    // You can register the rules configuration file. By default, the plugin searches for kotusRules.yaml in the root directory of the project.
    rulesPath.set(rootDir.resolve("kotusRules.yaml").path)
    // Here you can add additional rules that will be merged with the rules from the file, if it exists
    rules {
        // If you have a special way to add project dependencies in build.gradle
        // by default it supports declarations like project("name") or projects.name when TYPESAFE_PROJECT_ACCESSORS feature is enabled
//         projectRegex("") { matchResult -> "" }

        // If you need to sync some modules under some conditions. For example, If you add some project dependencies by convention plugin
        modulesByAlias("kotlin", setOf(":modules:three")) // if kotlin is used
        modulesByRegex(".*", setOf(":modules:four")) // always include
    }

    // You can register the configuration file. By default, the plugin searches for kotusConfiguration.yaml in the root directory of the project.
    configurationPath.set(rootDir.resolve("kotusConfiguration.yaml").path)
    // Here you can add additional configurations that will be merged with the rules from the file, if it exists
    configuration {
        focusing(":modules:one") // Which modules you want to sync
        replacing(":modules:three", ":modules:three-stub") // If you want to replace one module with other
    }
}

dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":modules")
include(":modules:one")
include(":modules:two")
include(":modules:three")
include(":modules:three-stub")
include(":modules:four")
include(":modules:five")

rootProject.name = "kotus"

//kotusPocus() // when manualRun is true