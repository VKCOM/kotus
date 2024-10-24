package com.vk.kotus.internal

import org.gradle.api.initialization.ProjectDescriptor

internal class ProjectDescriptorComparator : Comparator<ProjectDescriptor> {

    private val pathComparator = PathComparator()

    override fun compare(first: ProjectDescriptor, second: ProjectDescriptor): Int =
        pathComparator.compare(first.path, second.path)
}

internal class ModulesDescriptorComparator : Comparator<ModuleDescriptor> {

    private val pathComparator = PathComparator()

    override fun compare(first: ModuleDescriptor, second: ModuleDescriptor): Int =
        pathComparator.compare(first.name, second.name)
}

internal class PathComparator : Comparator<String> {

    override fun compare(first: String, second: String): Int {

        val firstColons = first.count { it == ':' }
        val secondColons = second.count { it == ':' }

        val firstResult = firstColons.compareTo(secondColons)
        if (firstResult != 0) return firstResult

        return first.compareTo(second)
    }
}