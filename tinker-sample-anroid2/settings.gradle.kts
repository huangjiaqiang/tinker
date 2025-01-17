pluginManagement {
    repositories {
        mavenLocal()
        maven(url="https://maven.aliyun.com/nexus/content/groups/public/")
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
        mavenLocal()
        maven(url="https://maven.aliyun.com/nexus/content/groups/public/")

        google()
        mavenCentral()
    }
}

rootProject.name = "tinker-sample-anroid2"
include(":app")
