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

    implementation("com.google.inject:guice:4.2.2") {
        exclude("org.checkerframework", "checker-compat-qual")
        exclude("com.google.guava", "guava")
    }

    implementation("org.jetbrains.kotlin:kotlin-reflect:${properties["kotlin_version"]}")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

intellij {
    version.set(properties["idea_version"] as String)
    type.set("IC")
    pluginName.set("${properties["plugin_name"]}")
    sandboxDir.set("idea-sandbox")
    plugins.set(listOf("java"))
}

val mvnDescription = """
Help for developing plugins for JetBrains products.
KotlinAnAction:provide ActionContext(support inject guice) for actionPerformed
"""

val publishProps = loadProperties(project.rootDir.path + ("/script/publish.properties"))

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(kotlin.sourceSets["main"].kotlin.srcDirs)
}
val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            groupId = "com.itangcent"
            artifactId = "guice-action"
            version = "${project.version}"

            from(components["kotlin"])
            artifact(sourcesJar.get())
            artifact(javadocJar.get())

            pom {
                name.set("Intellij Kotlin(guice-action)")
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