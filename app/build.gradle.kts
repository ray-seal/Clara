plugins {
id("com.android.application")
id("com.google.gms.google-services")
}

android {
namespace = "com.rayseal.supportapp"
compileSdk = 34

defaultConfig {
applicationId = "com.rayseal.supportapp"
minSdk = 21
targetSdk = 34
versionCode = 1
versionName = "1.0"
}

buildTypes {
getByName("release") {
isMinifyEnabled = false
proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
}
}
}

dependencies {
implementation("com.google.material:material:1.11.0")
implementation("com.google.firebase:firebase-auth:22.3.0")
implementation("com.google.android.gms:play-services-auth:20.7.0")
implementation("androidx.browser:browser:1.5.0")
}
