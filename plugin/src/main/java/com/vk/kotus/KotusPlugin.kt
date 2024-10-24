package com.vk.kotus

import com.vk.kotus.internal.kotusInternal
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.initialization.Settings
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.create

class KotusPlugin : Plugin<Settings> {

    override fun apply(target: Settings) {
        val extension = target.extensions.create<KotusExtension>("kotus")

        target.gradle.settingsEvaluated {
            if (!extension.manualRun.getOrElse(false)) {
                kotusInternal()
            }
        }
    }
}

abstract class KotusExtension {

    internal var rulesImpl: KotusRulesImpl = KotusRulesImpl()
    internal var configurationImpl: KotusConfigurationImpl = KotusConfigurationImpl()

    abstract val manualRun: Property<Boolean>
    abstract val enabled: Property<Boolean>
    abstract val verbose: Property<Boolean>

    abstract val rulesPath: Property<String>
    abstract val configurationPath: Property<String>

    fun rules(action: Action<KotusRules>) {
        action.execute(rulesImpl)
    }

    fun configuration(action: Action<KotusConfiguration>) {
        action.execute(configurationImpl)
    }
}