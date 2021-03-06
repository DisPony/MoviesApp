import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("kotlin-android-extensions")
    id("com.squareup.sqldelight")
}
group = "com.example.moviesapp"
version = "1.0-SNAPSHOT"

val ktorVersion = "1.4.0"
val serializationVersion = "1.0.0-RC"
val coroutinesVersion = "1.4.1-native-mt"
val mviKotlinVersion = "2.0.0"
val sqlDelightVersion: String by project

repositories {
    gradlePluginPortal()
    maven(url = "https://dl.bintray.com/arkivanov/maven")
    maven(url = "https://dl.bintray.com/badoo/maven")
    google()
    jcenter()
    mavenCentral()
}
kotlin {
    android()

    ios {
        binaries {
            framework {
                baseName = "shared"
                export("com.arkivanov.mvikotlin:mvikotlin:$mviKotlinVersion")
                export("com.arkivanov.mvikotlin:mvikotlin-iosx64:$mviKotlinVersion")
                export("com.arkivanov.mvikotlin:mvikotlin-main:$mviKotlinVersion")
            }
        }
    }

    val onPhone = System.getenv("SDK_NAME")?.startsWith("iphoneos") ?: false
    val iosTarget = if (onPhone) {
        iosArm64("ios")
    } else {
        iosX64("ios")
    }

    iosTarget.binaries {
        framework {
            baseName = "shared"
            export("com.arkivanov.mvikotlin:mvikotlin:$mviKotlinVersion")
            export("com.arkivanov.mvikotlin:mvikotlin-main:$mviKotlinVersion")
        }
    }

    sourceSets {
        val commonMain by getting {
            languageSettings.languageVersion = "1.4"
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
                implementation("io.ktor:ktor-client-core:$ktorVersion")
                implementation("io.ktor:ktor-client-serialization:$ktorVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
                api("com.arkivanov.mvikotlin:mvikotlin:$mviKotlinVersion")
                api("com.arkivanov.mvikotlin:mvikotlin-main:$mviKotlinVersion")
                implementation("com.arkivanov.mvikotlin:mvikotlin-logging:$mviKotlinVersion")
                implementation("com.arkivanov.mvikotlin:mvikotlin-extensions-coroutines:$mviKotlinVersion")
                implementation("com.squareup.sqldelight:runtime:$sqlDelightVersion")
                implementation("com.squareup.sqldelight:coroutines-extensions:$sqlDelightVersion")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-android:$ktorVersion")
                implementation("com.google.android.material:material:1.2.0")
                implementation("com.squareup.sqldelight:android-driver:$sqlDelightVersion")
            }
        }
        val androidTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation("junit:junit:4.12")
            }
        }
        val iosMain by getting {
            dependencies {
                api("com.arkivanov.mvikotlin:mvikotlin:$mviKotlinVersion")
                implementation("com.squareup.sqldelight:native-driver:$sqlDelightVersion")
                implementation("io.ktor:ktor-client-ios:$ktorVersion")
            }
        }
        val iosTest by getting
    }
}
android {
    compileSdkVersion(29)
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdkVersion(24)
        targetSdkVersion(29)
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}
sqldelight {
    database("AppDatabase") {
        packageName = "com.example.moviesapp.shared.cache"
    }
}
val packForXcode by tasks.creating(Sync::class) {
    group = "build"
    val mode = System.getenv("CONFIGURATION") ?: "DEBUG"
    val sdkName = System.getenv("SDK_NAME") ?: "iphonesimulator"
    val targetName = "ios" + if (sdkName.startsWith("iphoneos")) "Arm64" else "X64"
    val framework = kotlin.targets.getByName<KotlinNativeTarget>(targetName).binaries.getFramework(mode)
    inputs.property("mode", mode)
    dependsOn(framework.linkTask)
    val targetDir = File(buildDir, "xcode-frameworks")
    from({ framework.outputDirectory })
    into(targetDir)
}
tasks.getByName("build").dependsOn(packForXcode)