import org.jetbrains.kotlin.konan.properties.loadProperties

plugins {
    id("org.jetbrains.intellij") version "1.13.2"
}

group = "com.itangcent"
version = properties["project_version"]!!

repositories {
    mavenCentral()
}

dependencies {

    implementation(project(":commons"))
    implementation(project(":guice-action"))
    implementation(project(":intellij-jvm"))

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

    testImplementation(project(":intellij-idea-test"))

    // https://search.maven.org/artifact/org.mockito.kotlin/mockito-kotlin/3.2.0/jar
    testImplementation("org.mockito.kotlin:mockito-kotlin:3.2.0")

    // https://mvnrepository.com/artifact/org.mockito/mockito-inline
    testImplementation("org.mockito:mockito-inline:3.11.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    testImplementation("org.junit.jupiter:junit-jupiter-params:${properties["junit_version"]}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${properties["junit_version"]}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${properties["junit_version"]}")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:1.8.0")

}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.register("sourcesJar", Jar::class) {
    archiveClassifier.set("sources")
    from(kotlin.sourceSets["main"].kotlin.srcDirs)
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
config:
psi: some tool methods of psi
util:
"""

val publishProps = loadProperties(project.rootDir.path + ("/script/publish.properties"))


publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "com.itangcent"
            artifactId = "intellij-idea"
            version = "${project.version}"

            from(components["kotlin"])
            artifact(tasks.getByName("sourcesJar"))
//            artifact(sour)
//            artifact sourcesJar
            pom {
                name.set("Intellij Kotlin(intellij-idea)")
                description.set(mvnDescription)
                url.set("https://github.com/Earth-1610/intellij-kotlin")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("tangcent")
                        name.set("Tangcent")
                        email.set("pentatengcent@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/Earth-1610/intellij-kotlin")
                    developerConnection.set("scm:git:https://github.com/Earth-1610/intellij-kotlin")
                    url.set("https://github.com/Earth-1610/intellij-kotlin")
                }
            }
        }
    }

    repositories {
        maven {
            val releasesRepoUrl = "https://oss.sonatype.org/service/local/staging/deploy/maven2"
            val snapshotsRepoUrl = "https://oss.sonatype.org/content/repositories/snapshots"
            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)

            credentials {
                username = "${publishProps["sonatypeUsername"]}"
                password = "${publishProps["sonatypePassword"]}"
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"]).map {
        it.signatory(signatories.getDefaultSignatory(project))
    }
}