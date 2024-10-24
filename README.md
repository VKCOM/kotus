# Kotus

[![](https://jitpack.io/v/vkcom/kotus.svg)](https://jitpack.io/#vkcom/kotus)

The plugin allows to sync only required modules in a Gradle project. Inspired by [focus](https://github.com/dropbox/focus), but implemented differently.

### Quick Start

#### Apply in `settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.vk.kotus") {
                useModule("com.github.vkcom:kotus:${requested.version}")
            }
        }
    }
}

plugins {
    id("com.vk.kotus") version "1.1"
}

kotus {
    configuration {
        focusing(":module:name")
    }
}
// or before gradle 8.8
extensions.configure<KotusExtension> {
    configuration {
        focusing(":module:name")
    }
}
```

### How to configure

#### 1. Apply plugin in `settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.vk.kotus") {
                useModule("com.github.vkcom:kotus:${requested.version}")
            }
        }
    }
}

plugins {
    id("com.vk.kotus") version "1.1"
}
```


#### 2. Configure plugin

There are two ways to configure the plugin: via the DSL or via yaml configuration files.

**2.1 DSL way in `settings.gradle.kts`**:
```kotlin
extensions.configure<KotusExtension> {
// since gradle 8.8
// kotus {
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
        replacing(":modules:three") // Will use default stub registered via kotusIncludeWithStub
    }
}
```

**2.2 Using yaml files**:
`kotusRules.yaml`
```yaml
aliases:
  #  if kotlin is used
  - alias: "kotlin"
    modules:
      - ":modules:three"
  - alias: "compose"
    modules: ":modules:three"
regexes:
  #  always include
  - regex: ".*"
    modules:
      - ":modules:four"
```

`kotusConfiguration.yaml` (Don't forget to add this file to .gitignore)
```yaml
focusing:
  - ":modules:one"
replacing:
  - ":modules:three": ":modules:three-stub"
  #  or. but don't forget to register default stub via kotusIncludeWithStub  
  - ":modules:three"
```

You can choose or use them both, but prefer DSL for these several settings and yaml to other ones:
```kotlin
kotus {
    enabled.set(true)
    verbose.set(false)
//    manualRun.set(false)
//    rulesPath.set(rootDir.resolve("kotusRules.yaml").path)
//    configurationPath.set(rootDir.resolve("kotusConfiguration.yaml").path)
    rules {
//        projectRegex("") { matchResult -> "" }
    }
}
```

**3. Extra functions**

Also, you can use these method instead of `include`:
- `kotusInclude` - like `include`, but has more arguments for configuration
- `alwaysInclude` - kotus will include this module and its dependencies even if focus is enabled for another module
- `kotusIncludeWithStub` - like `kotusInclude`, but you can also define a default stub module that will replace the original one
