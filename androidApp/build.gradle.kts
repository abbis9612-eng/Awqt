plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.dcc.yawm"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.dcc.yawm"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":shared"))
    // MainActivity extends ComponentActivity directly.
    implementation(libs.androidx.activity)
}
