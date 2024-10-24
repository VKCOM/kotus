package com.vk.kotus

import com.vk.kotus.internal.ModuleDescriptor
import org.gradle.api.Action
import org.gradle.kotlin.dsl.invoke

interface KotusConfiguration {

    fun replace(action: Action<KotusReplace>)
    fun focus(action: Action<KotusFocus>)
}

interface KotusFocus {
    var enabled: Boolean
    fun module(module: String)
}

interface KotusReplace {
    var enabled: Boolean
    fun module(module: String, stub: String? = null)
}

internal class KotusConfigurationImpl : KotusConfiguration {
    private val kotusFocus = KotusFocusImpl()
    private val kotusReplace = KotusReplaceImpl()

    var focusEnabled: Boolean
        get() = kotusFocus.enabled
        set(value) {
            kotusFocus.enabled = value
        }
    var replaceEnabled: Boolean
        get() = kotusReplace.enabled
        set(value) {
            kotusReplace.enabled = value
        }

    val focusModules: MutableSet<ModuleDescriptor> get() = kotusFocus.modules
    val replaceModules: MutableMap<ModuleDescriptor, ModuleDescriptor?> get() = kotusReplace.modules

    override fun replace(action: Action<KotusReplace>) = action(kotusReplace)
    override fun focus(action: Action<KotusFocus>) = action(kotusFocus)
}

internal class KotusFocusImpl : KotusFocus {
    override var enabled: Boolean = false
    val modules: MutableSet<ModuleDescriptor> = mutableSetOf()

    override fun module(module: String) {
        modules += ModuleDescriptor(module)
    }
}

internal class KotusReplaceImpl : KotusReplace {
    override var enabled: Boolean = false
    val modules: MutableMap<ModuleDescriptor, ModuleDescriptor?> = mutableMapOf()

    override fun module(module: String, stub: String?) {
        modules[ModuleDescriptor(module)] = stub?.let(::ModuleDescriptor)
    }
}