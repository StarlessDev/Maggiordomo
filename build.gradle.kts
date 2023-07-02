plugins {
    id("java")

    id("application")
    id("io.freefair.lombok") version "8.0.1"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "dev.starless.maggiordomo"
version = "2.0.1"

var jdaVersion = "5.0.0-beta.9"

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://jitpack.io")
    }
    maven {
        url = uri("https://repo.spongepowered.org/maven/")
    }
}

dependencies {
    implementation("net.dv8tion:JDA:$jdaVersion") // JDA
    implementation("org.mongodb:mongodb-driver-sync:4.9.1") // MongoDB driver
    implementation("com.github.StarlessDev:MongoStorage:1.0.4")

    implementation("org.spongepowered:configurate-yaml:4.2.0-SNAPSHOT") // Configurate
    implementation("com.google.code.gson:gson:2.10.1") // Gson

    implementation("com.vdurmont:semver4j:3.1.0") // Semantic versioning util
    implementation("ch.qos.logback:logback-classic:1.4.7") // Logger implementation
}

application {
    mainClass.set("dev.starless.maggiordomo.Main")
}