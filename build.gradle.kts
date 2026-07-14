plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

val targetMcVersion = if (project.hasProperty("mcVer")) project.property("mcVer") as String else "26.2"
val targetJavaVer = if (project.hasProperty("javaVer")) (project.property("javaVer") as String).toInt() else 25
val targetPaperVer = if (project.hasProperty("paperVer")) project.property("paperVer") as String else "26.2.build.+"
val targetBuildDir = if (project.hasProperty("buildDirName")) project.property("buildDirName") as String else "build"

layout.buildDirectory.set(layout.projectDirectory.dir(targetBuildDir))
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${targetPaperVer}")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVer)
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion(targetMcVersion)
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
