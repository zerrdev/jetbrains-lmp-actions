plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

group = "dev.zerr"
version = "0.0.2"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        create("IC", "2024.3")
        instrumentationTools()
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "243"
            untilBuild = provider { null }
        }
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    patchPluginXml {
        changeNotes.set("""
            Initial release of LMP Actions for IntelliJ IDEA.
        """.trimIndent())
    }
}
