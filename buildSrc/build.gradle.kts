plugins {
    `kotlin-dsl`
    `java-library`
    idea
}

subprojects {
    dependencies {
        gradleApi()
        gradleKotlinDsl()
    }
    apply(plugin = "org.gradle.kotlin.kotlin-dsl")
    apply(plugin = "org.spongepowered.gradle.sponge.dev")
}

repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
    jcenter()
    maven(url = "https://files.minecraftforge.net/maven")
}

dependencies {
    implementation("net.minecrell.licenser:net.minecrell.licenser.gradle.plugin:0.4.1")
    implementation("net.minecraftforge.gradle:ForgeGradle:3.+")
    implementation(group = "org.spongepowered", name = "SpongeGradle", version = "0.11.0-SNAPSHOT")
}