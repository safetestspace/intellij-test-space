import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "com.github.safetestspace.testspace"
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        val idePath = providers.gradleProperty("idePath")
        if (idePath.isPresent) {
            local(idePath.get())
        } else {
            intellijIdeaUltimate(providers.gradleProperty("platformVersion"))
        }
        bundledPlugin("com.intellij.java")
        plugin("PythonCore:243.21565.193")
        plugin("com.jetbrains.rust:243.21565.245")
        testFramework(TestFrameworkType.Platform)
        testFramework(TestFrameworkType.Plugin.Java)
    }
    // IntelliJ's LightJavaCodeInsightFixtureTestCase uses the JUnit 3/4 infrastructure.
    // This dependency is test-only and is not included in the plugin archive.
    testImplementation("junit:junit:4.13.2")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 21
}

intellijPlatform {
    pluginConfiguration {
        name = "Test Space"
        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
        }
    }
    pluginVerification {
        ides {
            current()
            create(IntelliJPlatformType.PyCharmProfessional, providers.gradleProperty("platformVersion"))
            create(IntelliJPlatformType.RustRover, providers.gradleProperty("platformVersion"))
            providers.gradleProperty("verificationIdePaths")
                .orElse(providers.gradleProperty("verificationIdePath")).orNull?.split(',')?.forEach {
                    local(file(it.trim()))
            }
        }
    }
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}

tasks.test {
    maxHeapSize = "2g"
    systemProperty("java.awt.headless", "true")
    // Load only this plugin and its dependencies, including when testing against a local Ultimate IDE.
    systemProperty(
        "idea.load.plugins.id",
        "com.github.safetestspace.testspace,com.intellij.java,PythonCore,com.jetbrains.rust"
    )
}

tasks.wrapper {
    gradleVersion = "9.8.0"
    distributionSha256Sum = "bafd5ce9cfaea0fbccfdc8439a1ac42fbd4cd9c89dc9a988228d8a2639a58e6c"
}
