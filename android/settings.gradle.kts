pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "NagarSetu"

include(":frontend:app")
include(":frontend:common-ui")
include(":frontend:wearos")

include(":frontend:components:assistant")
include(":frontend:components:charge-up")
include(":frontend:components:dashboard")
include(":frontend:components:drive-legal")
include(":frontend:components:emergency-ai")
include(":frontend:components:green-route")
include(":frontend:components:health-watch")
include(":frontend:components:park-ease")
include(":frontend:components:predictive")
include(":frontend:components:raksha")
include(":frontend:components:road-watch")
include(":frontend:components:report-it")

include(":frontend:components:auth")

include(":backend:components:auth")
include(":backend:components:core")
include(":backend:components:charge-up")
include(":backend:components:dashboard")
include(":backend:components:drive-legal")
include(":backend:components:emergency-ai")
include(":backend:components:green-route")
include(":backend:components:health-watch")
include(":backend:components:park-ease")
include(":backend:components:predictive")
include(":backend:components:raksha")
include(":backend:components:road-watch")
include(":backend:components:report-it")

include(":backend:components:firebase")
