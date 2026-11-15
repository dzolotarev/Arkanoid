plugins {
    java
    application
}

group = "com.arcanoid"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

application {
    mainClass = "com.arcanoid.Main"
}

repositories {
    mavenCentral()
}

tasks.jar {
    manifest.attributes["Main-Class"] = "com.arcanoid.Main"
}
