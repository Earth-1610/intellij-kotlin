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
    implementation(project(":intellij-kotlin:intellij-idea"))

    // https://search.maven.org/artifact/org.mockito.kotlin/mockito-kotlin/3.2.0/jar
    implementation("org.mockito.kotlin:mockito-kotlin:4.1.0")

    implementation("com.google.inject:guice:4.2.2") {
        exclude("org.checkerframework", "checker-compat-qual")
        exclude("com.google.guava", "guava")
    }

    // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api
    compileOnly("org.junit.jupiter:junit-jupiter-api:5.7.0")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

intellij {
    version.set("2021.2.1")
    type.set("IC")
    pluginName.set("${properties["plugin_name"]}")
    sandboxDir.set("idea-sandbox")
    plugins.set(listOf("java"))
}
