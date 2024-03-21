import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jooq.meta.jaxb.Property

val kotlinVersion: String by project
val jooqVersion: String by project

plugins {
    id("application")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("maven-publish")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.21"
    id("nu.studer.jooq") version "9.0"
}

application {
    mainClass.set("cloud.ganttproject.colloboque.DevServerKt")
    applicationDefaultJvmArgs = listOf("-Dorg.jooq.no-logo=true")
}

repositories {
    google()
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/bardsoftware/ganttproject")
        credentials {
            username = project.findProperty("gpr.user")?.toString() ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.key")?.toString() ?: System.getenv("TOKEN")
        }
    }
    mavenLocal()
}

dependencies {
    implementation("biz.ganttproject:biz.ganttproject.core:24.+")
    implementation("biz.ganttproject:ganttproject:24.+")
    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("com.google.guava:guava:31.1-jre")

    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.postgresql:postgresql:42.5.0")

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.14.0")
    implementation(kotlin("stdlib", version = kotlinVersion))
    implementation(kotlin("reflect", version = kotlinVersion))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("org.jooq:jooq:$jooqVersion")
    implementation("com.h2database:h2:2.1.214")
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("com.github.ajalt.clikt:clikt:4.+")
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    implementation("org.nanohttpd:nanohttpd-websocket:2.3.1")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")

    implementation(files("lib/eclipsito.jar"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.0")

    jooqGenerator("org.jooq:jooq-meta-extensions:$jooqVersion")
}

jooq {
    version.set(jooqVersion)
    configurations {
        create("main") {
            jooqConfiguration.apply {
                logging = org.jooq.meta.jaxb.Logging.WARN
                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"
                    database.apply {
                        name = "org.jooq.meta.extensions.ddl.DDLDatabase"
                        properties.apply {
                            add(Property().apply {
                                key = "scripts"
                                value = "src/main/resources/database-schema-template.sql"
                            })
                            add(Property().apply {
                                key = "defaultNameCase"
                                value = "lower"
                            })
                        }
                    }

                    target.apply {
                        packageName = "cloud.ganttproject.colloboque.db"
                    }
                }
            }
        }
    }
}

tasks.getByName<KotlinCompile>("compileKotlin") {
    dependsOn(":generateJooq")
    kotlinOptions {
        jvmTarget = "17"
    }
}

tasks.register<Copy>("copyDbScriptMain") {
    from("$rootDir/../ganttproject/src/main/resources/resources/sql/")
    into("src/main/resources/sql")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
    }
}

tasks.getByName<ProcessResources>("processResources") {
    dependsOn("copyDbScriptMain")
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
