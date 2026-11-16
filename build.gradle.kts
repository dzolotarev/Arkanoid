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

// --- Нативная сборка через jpackage (app-image: ELF-лаунчер + урезанный JRE) ---
// Запуск: ./gradlew nativeImage              результат: build/jpackage/Arkanoid/
//         ./gradlew packageDist          результат: build/dist/arkanoid-<version>-linux-x64.tar.gz
// jpackage берётся из PATH или из переменной окружения JPACKAGE.
tasks.register("nativeImage") {
    group = "build"
    description = "Собирает нативный app-image через jpackage"
    dependsOn("jar")

    doLast {
        val jpackage = System.getenv("JPACKAGE") ?: "jpackage"
        val type = System.getenv("JP_TYPE") ?: "app-image"
        val jarTask = tasks.named<Jar>("jar").get()
        val jarFile = jarTask.archiveFile.get().asFile
        val distDir = layout.buildDirectory.dir("jpackage").get().asFile
        // jpackage не перезаписывает готовый app-image — очищаем выходной каталог.
        delete(distDir)
        distDir.mkdirs()

        val args = listOf(
            jpackage,
            "--name", "Arkanoid",
            "--app-version", project.version.toString(),
            "--vendor", "dzolotarev",
            "--description", "Arkanoid — классическая аркада на Java с неоновой графикой",
            "--input", jarFile.parentFile.absolutePath,
            "--main-jar", jarFile.name,
            "--main-class", "com.arcanoid.Main",
            "--add-modules", "java.base,java.desktop",
            "--jlink-options", "--strip-debug --no-header-files --no-man-pages --compress=2",
            "--dest", distDir.absolutePath,
            "--type", type
        )
        val process = ProcessBuilder(args)
            .directory(projectDir)
            .redirectErrorStream(true)
            .start()
        process.inputStream.bufferedReader().forEachLine { println(it) }
        if (process.waitFor() != 0) {
            throw GradleException("jpackage завершился с ошибкой")
        }
        println("Нативная сборка готова: ${distDir.absolutePath}/Arkanoid")
    }
}

// Упаковка app-image в tar.gz для релизных ассетов.
tasks.register("packageDist") {
    group = "distribution"
    description = "Складывает app-image в build/dist/*.tar.gz"
    dependsOn("nativeImage")

    doLast {
        val image = layout.buildDirectory.dir("jpackage/Arkanoid").get().asFile
        val outDir = layout.buildDirectory.dir("dist").get().asFile
        outDir.mkdirs()
        val archive = outDir.resolve("arkanoid-${project.version}-linux-x64.tar.gz")
        archive.delete()

        val tar = ProcessBuilder("tar", "-czf", archive.absolutePath, "Arkanoid")
            .directory(image.parentFile)
            .redirectErrorStream(true)
            .start()
        tar.inputStream.bufferedReader().forEachLine { println(it) }
        if (tar.waitFor() != 0) {
            throw GradleException("tar завершился с ошибкой")
        }
        println("Архив готов: ${archive.absolutePath} (${archive.length()} байт)")
    }
}
