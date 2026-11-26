plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}
android {
    namespace = "com.prince.whatstoolbox"; compileSdk = 36
    defaultConfig { applicationId = "com.prince.whatstoolbox"; minSdk = 26; targetSdk = 36; versionCode = 4; versionName = "1.4"
        // AdMob + Play Billing. These are Google's TEST ids: replace them with your own before release (AdMob console -> Apps / Ad units).
        manifestPlaceholders["admobAppId"] = "ca-app-pub-4955598430856685~1110136680"
        buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-4955598430856685/8797055013\"")
        buildConfigField("String", "ADMOB_NATIVE_VIDEO_ID", "\"ca-app-pub-4955598430856685/5150198908\"")
        // Product id of the one-time in-app com.prince.whatstoolboxproduct you create in Play Console (price $5.99).
        buildConfigField("String", "PREMIUM_PRODUCT_ID", "\"premium_unlock\"") }
    buildTypes { release { isMinifyEnabled = true; isShrinkResources = true; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") } }
    testOptions { unitTests.isReturnDefaultValues = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    buildFeatures { compose = true; buildConfig = true }
}
dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.navigation:navigation-compose:2.8.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.google.dagger:hilt-android:2.60.1"); ksp("com.google.dagger:hilt-compiler:2.60.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.room:room-runtime:2.8.5"); implementation("androidx.room:room-ktx:2.8.5"); ksp("androidx.room:room-compiler:2.8.5")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.media3:media3-transformer:1.4.1"); implementation("androidx.media3:media3-common:1.4.1")
    implementation("com.google.zxing:core:3.5.3")
    implementation("androidx.media3:media3-effect:1.4.1")
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("com.alphacephei:vosk-android:0.3.75")   // offline speech-to-text (needs a model in assets/model-en)
    implementation("com.github.NorthernCaptain:TAndroidLame:1.1")   // LAME MP3 encoder (JitPack)
    implementation("com.google.android.gms:play-services-ads:23.4.0")          // banner + native video ads
    implementation("com.google.android.ump:user-messaging-platform:3.0.0")     // consent form (required for ads in the EEA / UK)
    implementation("com.android.billingclient:billing-ktx:9.1.0")              // one-time purchase
    testImplementation("junit:junit:4.13.2")
}
