package com.vk.kotus

interface KotusRules {
    fun projectRegex(regex: String, result: (MatchResult) -> String)
    fun modulesByAlias(alias: String, modules: Set<String>)
    fun modulesByRegex(regex: String, modules: Set<String>)
}

internal class KotusRulesImpl : KotusRules {

    val projectRegexes: MutableMap<String, (MatchResult) -> String> = mutableMapOf()
    val aliasedModules: MutableMap<String, Set<String>> = mutableMapOf()
    val modulesByRegex: MutableList<Pair<String, Set<String>>> = mutableListOf()
    
    override fun projectRegex(regex: String, result: (MatchResult) -> String) {
        projectRegexes[regex] = result
    }

    override fun modulesByAlias(alias: String, modules: Set<String>) {
        aliasedModules[alias] = modules
    }

    override fun modulesByRegex(regex: String, modules: Set<String>) {
        modulesByRegex.add(regex to modules)
    }
}