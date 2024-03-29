plugins {
    id("java")

    id("application")
    id("io.freefair.lombok") version "8.4"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "dev.starless.maggiordomo"
version = "2.1.2"

var jdaVersion = "5.0.0-beta.21"

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://repo.spongepowered.org/maven/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

dependencies {
    implementation("net.dv8tion:JDA:$jdaVersion") // JDA
    implementation("io.javalin:javalin:6.1.3") // Javalin
    implementation("org.mongodb:mongodb-driver-sync:5.0.0") // MongoDB driver
    implementation("com.github.StarlessDev:MongoStorage:1.0.8") // MongoDB (de)serialization library

    implementation("org.spongepowered:configurate-yaml:4.2.0-SNAPSHOT") // Configurate
    implementation("com.google.code.gson:gson:2.10.1") // Gson

    implementation("com.vdurmont:semver4j:3.1.0") // Semantic versioning util
    implementation("ch.qos.logback:logback-classic:1.5.3") // Logger implementation
    implementation("com.anyascii:anyascii:0.3.2") // AnyAscii (finds the best ascii character match of unicode characters)
}

application {
    mainClass.set("dev.starless.maggiordomo.Main")
}