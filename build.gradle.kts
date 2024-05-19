import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.*

val coroutinesVersion = "1.7.3"
val jupiterVersion = "5.10.1"
val logbackVersion = "1.4.14"
val kotlinLoginVersion = "3.0.5"
val slf4jApiVersoion = "2.0.11"
val gsonVersion = "2.11.0"
val httpClientVersion = "5.3.1"

    plugins {
    kotlin("jvm") version "1.9.22"
    id("org.jetbrains.dokka") version "1.9.10"
    application
    id("com.adarshr.test-logger") version "4.0.0"
    `maven-publish`
    id("com.jfrog.bintray") version "1.8.5"
}
group = "com.github.barakb"
version = "1.0.5"

repositories {
    gradlePluginPortal()
    mavenCentral()
    jcenter()
    mavenLocal()
}

dependencies {
    testImplementation(kotlin("test-junit5"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.apache.httpcomponents.client5:httpclient5:$httpClientVersion")
    implementation("com.google.code.gson:gson:$gsonVersion")
    implementation("org.slf4j:slf4j-api:$slf4jApiVersoion")
    implementation("io.github.microutils:kotlin-logging:$kotlinLoginVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.9.22")
    dokkaJavadocPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.9.10")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$jupiterVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$jupiterVersion")
    testRuntimeOnly("ch.qos.logback:logback-classic:$logbackVersion")
    implementation(kotlin("stdlib-jdk8"))
}
tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        @Suppress("SpellCheckingInspection")
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.RequiresOptIn"
    }
}

tasks.test {
    useJUnitPlatform()
}

testlogger {
    showStandardStreams = true
}

application {
    applicationDefaultJvmArgs = listOf("-Dkotlinx.coroutines.debug")
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}


val artifactName = project.name
val artifactGroup = project.group.toString()
val artifactVersion = project.version.toString()

val pomUrl = "https://github.com/barakb/mini-rest-client"
val pomScmUrl = "https://github.com/barakb/mini-rest-client"
val pomIssueUrl = "https://github.com/barakb/mini-rest-client/issues"
val pomDesc = "A Kotlin Mimimal REST client"

val githubRepo = "barakb/mini-rest-client"
val githubReadme = "Readme.md"

val pomLicenseName = "The Apache Software License, Version 2.0"
val pomLicenseUrl = "http://www.apache.org/licenses/LICENSE-2.0.txt"
val pomLicenseDist = "repo"

val pomDeveloperId = "barakb"
val pomDeveloperName = "Barak Bar Orion"

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    archiveClassifier.set("javadoc")
    from(tasks.dokkaJavadoc)
}

publishing {
    publications {
        create<MavenPublication>("mini-rest-client") {
            groupId = artifactGroup
            artifactId = artifactName
            version = artifactVersion
            from(components["java"])

            artifact(sourcesJar)
            artifact(dokkaJar)

            pom.withXml {
                asNode().apply {
                    appendNode("description", pomDesc)
                    appendNode("name", rootProject.name)
                    appendNode("url", pomUrl)
                    appendNode("licenses").appendNode("license").apply {
                        appendNode("name", pomLicenseName)
                        appendNode("url", pomLicenseUrl)
                        appendNode("distribution", pomLicenseDist)
                    }
                    appendNode("developers").appendNode("developer").apply {
                        appendNode("id", pomDeveloperId)
                        appendNode("name", pomDeveloperName)
                    }
                    appendNode("scm").apply {
                        appendNode("url", pomScmUrl)
                    }
                }
            }
        }
    }
}


bintray {
    user = project.findProperty("bintrayUser").toString()
    key = project.findProperty("bintrayKey").toString()
    publish = true

    setPublications("mini-rest-client")

    pkg.apply {
        repo = "maven"
        name = artifactName
        userOrg = "barakb"
        githubRepo = "barakb/mini-rest-client"
        vcsUrl = pomScmUrl
        description = "A Kotlin Mimimal REST client"
        setLabels("kotlin", "REST")
        setLicenses("Apache-2.0")
        desc = description
        websiteUrl = pomUrl
        issueTrackerUrl = pomIssueUrl
        githubReleaseNotesFile = githubReadme
        version.apply {
            name = artifactVersion
            desc = pomDesc
            released = Date().toString()
            vcsTag = artifactVersion
            gpg.sign = true
            mavenCentralSync.apply {
                sync = true
                user = project.findProperty("sonatypeUser").toString()
                password = project.findProperty("sonatypePassword").toString()
            }
        }
    }
}