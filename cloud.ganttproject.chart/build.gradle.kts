plugins {
    kotlin("multiplatform") version "2.2.20"
}

repositories {
    mavenCentral()
}

kotlin {
    js(IR) {
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
        // Produce a JavaScript library (with TypeScript definitions) instead of
        // an application bundle, so that web pages can call `drawChart`.
        binaries.library()
        compilerOptions {
            freeCompilerArgs.add("-Xgenerate-dts")
        }
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-browser:0.5.0")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

version = "1.0-SNAPSHOT"
