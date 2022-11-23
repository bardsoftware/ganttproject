import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinVersion: String by project

plugins {
    id("application")
    id("org.jetbrains.kotlin.jvm") version "1.7.21"
    id("maven-publish")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.21"
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
    implementation("org.postgresql:postgresql:42.5.0")

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.14.0")
    implementation(kotlin("stdlib", version = kotlinVersion))
    implementation(kotlin("reflect", version = kotlinVersion))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jooq:jooq:3.17.5")
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("com.github.ajalt.clikt:clikt:3.5.0")
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation("org.nanohttpd:nanohttpd-websocket:2.3.1")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    implementation(files("lib/eclipsito.jar"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testImplementation("com.h2database:h2:2.1.214")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")
}

tasks.getByName<KotlinCompile>("compileKotlin") {
    kotlinOptions {
        jvmTarget = "17"
    }
}

tasks.register<Copy>("copyDbScript") {
    from("$rootDir/../ganttproject/src/main/resources/resources/sql/")
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
