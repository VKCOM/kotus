package com.vk.kotus

import com.vk.kotus.internal.ModuleDescriptor

interface KotusConfiguration {
    fun replacing(module: String, stub: String? = null)
    fun focusing(module: String)
}

internal class KotusConfigurationImpl : KotusConfiguration {
    val focusingModules: MutableSet<ModuleDescriptor> = mutableSetOf()
    val replacingWithStubModules: MutableMap<ModuleDescriptor, ModuleDescriptor?> = mutableMapOf()

    override fun replacing(module: String, stub: String?) {
        replacingWithStubModules[ModuleDescriptor(module)] = stub?.let(::ModuleDescriptor)
    }
    
    override fun focusing(module: String) {
        focusingModules += ModuleDescriptor(module)
    }
} 