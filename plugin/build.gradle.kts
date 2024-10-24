plugins {
    `kotlin-dsl`
    id("com.vanniktech.maven.publish") version "0.21.0"
}

group = "com.vk.kotus"
version = "1.1"

mavenPublishing {
    pomFromGradleProperties()
}

gradlePlugin {
    plugins {
        register("kotus") {
            id = "com.vk.kotus"
            implementationClass = "com.vk.kotus.KotusPlugin"
        }
    }
}

dependencies {
    implementation(gradleApi())
    implementation("com.charleskorn.kaml:kaml:0.50.0")
}


kotlin {
    jvmToolchain(17)
}