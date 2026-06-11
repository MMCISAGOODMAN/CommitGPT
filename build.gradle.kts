plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.intellij.platform") version "2.16.0"
}

group = "com.commitgpt"
version = "1.0.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2023.3.6")
        bundledPlugin("com.intellij.java")
        bundledPlugin("Git4Idea")
        pluginVerifier()
    }
}

kotlin {
    jvmToolchain(17)
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "233"
            untilBuild = "243.*"
        }
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
            .orElse(providers.gradleProperty("PUBLISH_TOKEN"))
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
            .orElse(providers.gradleProperty("CERTIFICATE_CHAIN"))
        privateKey = providers.environmentVariable("PRIVATE_KEY")
            .orElse(providers.gradleProperty("PRIVATE_KEY"))
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
            .orElse(providers.gradleProperty("PRIVATE_KEY_PASSWORD"))
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

tasks {
    buildSearchableOptions {
        enabled = false
    }
}
