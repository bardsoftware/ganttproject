import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinVersion: String by project

plugins {
    id("application")
    id("org.jetbrains.kotlin.jvm") version "1.7.0"
    id("maven-publish")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.0"
}

application {
    mainClass.set("cloud.ganttproject.colloboque.DevServerKt")
    applicationDefaultJvmArgs = listOf("-Dorg.jooq.no-logo=true")
}

repositories {
    google()
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("biz.ganttproject:biz.ganttproject.core:22.+")
    implementation("biz.ganttproject:ganttproject:22.+")
    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("com.google.guava:guava:31.1-jre")

    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.postgresql:postgresql:42.3.6")

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.13.3")
    implementation(kotlin("stdlib", kotlinVersion))
    implementation(kotlin("reflect", version = kotlinVersion))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")
    implementation("org.jooq:jooq:3.16.6")
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("com.github.ajalt.clikt:clikt:3.5.0")
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation("org.nanohttpd:nanohttpd-websocket:2.3.1")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")

    implementation(files("lib/eclipsito.jar"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("com.h2database:h2:2.1.212")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
}

tasks.getByName<KotlinCompile>("compileKotlin") {
    kotlinOptions {
        jvmTarget = "11"
    }
}

tasks.register<Copy>("copyDbScript") {
    from("$rootDir/../ganttproject/src/main/resources/resources/sql/init-project-database.sql")
    into("src/test/resources/sql")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
    }
    dependsOn("copyDbScript")
}


group = "cloud.ganttproject"   // Generated output GroupId
version = "22-SNAPSHOT" // Version in generated output


publishing {
    repositories {
        maven {
            name = "ganttproject-maven-repository-internal"
            url = uri("gcs://ganttproject-maven-repository/internal")
        }
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/bardsoftware/ganttproject")
            credentials {
                username = project.findProperty("gpr.user")?.toString() ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key")?.toString() ?: System.getenv("TOKEN")
            }
        }
    }
    publications {
        register("libcolloboque", MavenPublication::class) {
            artifactId = "colloboque"
            from(components["java"])
        }
    }
}
