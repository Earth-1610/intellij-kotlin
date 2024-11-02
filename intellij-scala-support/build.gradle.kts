import org.jetbrains.kotlin.konan.properties.loadProperties

plugins {
    id("org.jetbrains.intellij") version "1.13.2"
}

group = "com.itangcent"
version = "1.8.2"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":intellij-kotlin:commons"))
    implementation(project(":intellij-kotlin:guice-action"))
    implementation(project(":intellij-kotlin:intellij-jvm"))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}


intellij {
    version.set("2021.2.1")
    type.set("IC")
    pluginName.set("${properties["plugin_name"]}")
    sandboxDir.set("idea-sandbox")
    plugins.set(listOf("java", "org.intellij.scala:2021.2.22"))
}
