plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room3)
}

kotlin {
    jvmToolchain(17)

    android {
        namespace = "com.dcc.yawm.shared"
        compileSdk = 37
        minSdk = 26
        withHostTestBuilder {}
    }

    // A plain JVM target only so CI can run the Room tests on the host with the bundled SQLite
    // (the Android host tests cannot load Android's native SQLite). There is no desktop app.
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.adhan)
            implementation(libs.kotlinx.datetime)
            implementation(libs.androidx.room3.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room3.compiler)
    add("kspJvm", libs.androidx.room3.compiler)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

// adhan2-jvm 0.0.7 ships Java 21 class files (version 65, checked 2026-10-27), so the host
// tests must run on a Java 21 JVM. Our own code still compiles with the JDK 17 toolchain.
tasks.withType<Test>().configureEach {
    javaLauncher.set(
        project.extensions.getByType<JavaToolchainService>().launcherFor {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    )
}
