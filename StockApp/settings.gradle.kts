pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "StockApp"
include(":app")

// kotlin_krx: Use real module if available, otherwise use stub for CI builds
val krxktDir = file("../../kotlin_krx")
include(":krxkt")
if (krxktDir.exists()) {
    project(":krxkt").projectDir = krxktDir
} else {
    project(":krxkt").projectDir = file("krxkt-stub")
}
