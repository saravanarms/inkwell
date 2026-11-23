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
    }
}

rootProject.name = "Inkwell"

// Core modules
include(":core:common")
include(":core:model")
include(":core:memory")

// Graphics & Stroke engine (NDK + C++ + GLES3)
include(":engine")

// Feature modules
include(":feature:canvas")
include(":feature:layers")
include(":feature:color")
include(":feature:gallery")
include(":feature:export")

// Data & Persistence modules
include(":data:project")
include(":data:repository")

// Application entry
include(":app")
