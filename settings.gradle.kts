pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "AndroidLiveProductionStudio"

include(":app")

// Core Modular Architecture
include(":core:media")
include(":core:camera")
include(":core:usb")
include(":core:audio")
include(":core:streaming")
include(":core:network")
include(":core:diagnostics")

// Feature Modular Architecture
include(":feature:live")
include(":feature:camera")
include(":feature:settings")
include(":feature:diagnostics")
