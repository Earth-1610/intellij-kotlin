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

    implementation("com.google.inject:guice:4.2.2") {
        exclude("org.checkerframework", "checker-compat-qual")
        exclude("com.google.guava", "guava")
    }

    implementation("org.jetbrains.kotlin:kotlin-reflect:1.8.0")

    // https://mvnrepository.com/artifact/org.jsoup/jsoup
    implementation("org.jsoup:jsoup:1.12.1")

    // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.2")

    // https://mvnrepository.com/artifact/org.xerial/sqlite-jdbc
    implementation("org.xerial:sqlite-jdbc:3.34.0")

    testImplementation(project(":intellij-kotlin:intellij-idea-test"))

    // https://search.maven.org/artifact/org.mockito.kotlin/mockito-kotlin/3.2.0/jar
    testImplementation("org.mockito.kotlin:mockito-kotlin:3.2.0")

    // https://mvnrepository.com/artifact/org.mockito/mockito-inline
    testImplementation("org.mockito:mockito-inline:3.11.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    testImplementation("org.junit.jupiter:junit-jupiter-params:${properties["junit_version"]}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${properties["junit_version"]}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${properties["junit_version"]}")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.7.1")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.8.0")

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
