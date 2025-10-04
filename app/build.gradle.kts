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

lint {
    baseline = file("lint-baseline.xml")
    abortOnError = false
}
}

dependencies {
implementation("com.google.android.material:material:1.11.0")
implementation("com.google.firebase:firebase-firestore:24.9.1")
implementation("com.google.firebase:firebase-auth:22.3.0")
implementation("com.google.firebase:firebase-analytics:21.5.0")
implementation("com.google.firebase:firebase-storage:20.3.0")
implementation("com.google.firebase:firebase-database:20.3.0")
implementation("com.google.android.gms:play-services-auth:20.7.0")
implementation("androidx.browser:browser:1.5.0")
implementation("com.github.bumptech.glide:glide:4.16.0")
}
