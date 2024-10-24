plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":modules:two"))
    implementation(projects.modules.five)
}