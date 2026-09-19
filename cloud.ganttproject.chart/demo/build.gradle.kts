plugins {
    kotlin("multiplatform") version "2.2.20"
}

repositories {
    mavenCentral()
}

kotlin {
    js(IR) {
        browser {
            commonWebpackConfig {
                // Fixed bundle name so index.html can reference it directly.
                outputFileName = "demo.js"
            }
        }
        // A runnable application bundle (not a library): this is the demo consumer.
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                // The chart-rendering library this demo showcases.
                implementation(project(":"))
                implementation("org.jetbrains.kotlinx:kotlinx-browser:0.5.0")
            }
        }
    }
}

version = "1.0-SNAPSHOT"
