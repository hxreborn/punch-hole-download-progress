import org.gradle.api.plugins.BasePlugin
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.Exec

plugins {
    base
}

tasks.named<Delete>("clean") {
    group = BasePlugin.BUILD_GROUP
    description = "Deletes the build directory."
    delete(rootProject.layout.buildDirectory)
}

tasks.register("assembleDebugRelease") {
    group = BasePlugin.BUILD_GROUP
    description = "Assembles both debug and release builds of the app module."
    dependsOn(":app:assembleDebug", ":app:assembleRelease")
}

tasks.register("cleanBuild") {
    group = BasePlugin.BUILD_GROUP
    description = "Cleans the project and then assembles all builds in the app module."
    dependsOn("clean", "assembleDebugRelease")
}

tasks.register<Exec>("buildLibxposedApi") {
    group = "libxposed"
    description = "Builds libxposed/api and publishes to mavenLocal"
    workingDir = layout.projectDirectory.dir("libxposed/api").asFile
    commandLine(
        "./gradlew",
        ":api:publishApiPublicationToMavenLocal",
        "-x",
        ":checks:compileKotlin",
        "--no-daemon",
    )
}

tasks.register<Exec>("buildLibxposedService") {
    group = "libxposed"
    description = "Builds libxposed/service and publishes to mavenLocal"
    workingDir = layout.projectDirectory.dir("libxposed/service").asFile
    commandLine(
        "./gradlew",
        ":interface:publishInterfacePublicationToMavenLocal",
        ":service:publishServicePublicationToMavenLocal",
        "--no-daemon",
    )
}

tasks.register("buildLibxposed") {
    group = "libxposed"
    description = "Builds both libxposed/api and libxposed/service"
    dependsOn("buildLibxposedApi", "buildLibxposedService")
}
