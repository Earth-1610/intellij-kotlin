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

    implementation("com.google.inject:guice:4.2.2") {
        exclude("org.checkerframework", "checker-compat-qual")
        exclude("com.google.guava", "guava")
    }

    implementation("org.jetbrains.kotlin:kotlin-reflect:1.8.0")
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

val mvnDescription = """
Help for developing plugins for JetBrains products.
KotlinAnAction:provide ActionContext(support inject guice) for actionPerformed
"""

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(kotlin.sourceSets["main"].kotlin.srcDirs)
}
val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}