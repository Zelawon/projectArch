plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "dte.masteriot.mdp.andoirdapp_smartirigationsystem"
    compileSdk = 34

    defaultConfig {
        applicationId = "dte.masteriot.mdp.andoirdapp_smartirigationsystem"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    packagingOptions {
        pickFirst("META-INF/io.netty.versions.properties")
        pickFirst("META-INF/INDEX.LIST")
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // MQTT Client for real-time, lightweight communication with HiveMQ
    implementation("com.hivemq:hivemq-mqtt-client:1.3.4")

    implementation("com.github.angads25:toggle:1.1.0")
    implementation("com.github.Gruzer:simple-gauge-android:0.3.1")

    implementation ("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5")
    implementation ("com.google.android.material:material:1.6.0")
}
