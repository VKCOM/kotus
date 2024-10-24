package com.vk.kotus.internal

internal data class ModuleDescriptor(
    val name: String,
    val projectDir: String = "",
    val buildFileName: String = ""
)
