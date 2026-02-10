plugins {
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    compileOnly("com.squareup.okhttp3:okhttp:4.12.0")
}
