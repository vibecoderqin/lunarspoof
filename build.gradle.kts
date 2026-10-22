import dev.architectury.pack200.java.Pack200Adapter
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.util.HashSet
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import net.fabricmc.loom.task.RemapJarTask
import org.apache.commons.lang3.SystemUtils

// Buildscript based on https://github.com/lineargraph/Forge1.8.9Template (Unlicense)

// Version from GitHub Tag:
val gitRef : String? = System.getenv("GITHUB_REF_NAME")
version = "1.1.0"

// Lunar Properties:
val lunarVersion = findProperty("lunarVersion")?.toString() ?: ""
val lunarGitCommit = findProperty("lunarGitCommit")?.toString() ?: ""

// Plugins:
plugins {
    idea
    java
    id("net.kyori.blossom") version "2.2.0"
    id("gg.essential.loom") version "0.10.0.+"
    id("com.diffplug.spotless") version "8.1.0"
    id("com.gradleup.shadow") version "8.3.10"
    id("dev.architectury.architectury-pack200") version "0.1.3"
}

// Toolchains:
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

// Minecraft configuration:
loom {
    log4jConfigs.from(file("extras/log4j2.xml"))
    launchConfigs {
        "client" {
            property("mixin.debug", "true")
            arg("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
        }
    }
    runConfigs {
        "client" {
            if (SystemUtils.IS_OS_MAC_OSX) {
                // This argument causes a crash on macOS
                vmArgs.remove("-XstartOnFirstThread")
            }
        }
        remove(getByName("server"))
    }
    forge {
        pack200Provider.set(Pack200Adapter())
        mixinConfig("mixins.moonlight.json")

    }
    @Suppress("UnstableApiUsage")
    mixin {
        defaultRefmapName.set("mixins.moonlight.refmap.json")
    }
}

// Blossom configuration:
sourceSets {
    main {
        blossom {
            resources {
                property("version", version.toString())
                property("lunarVersion", lunarVersion)
                property("lunarGitCommit", lunarGitCommit)
            }
            javaSources {
                property("version", version.toString())
                property("lunarVersion", lunarVersion)
                property("lunarGitCommit", lunarGitCommit)
            }
        }
        output.setResourcesDir(sourceSets.main.flatMap { it.java.classesDirectory })
    }
}

idea {
    module {
        generatedSourceDirs.add(file("build/generated/sources/blossom/main/java"))
    }
}

// Spotless Configuration:
spotless {
    java {
        importOrder()
        removeUnusedImports()
        eclipse().configFile("extras/eclipse-formatter.xml")
        formatAnnotations()

        targetExclude("build/generated/**")
    }
}

// Dependencies:
repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven/")
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    maven("https://repo.lunarclient.dev")
    maven("https://maven.firstdark.dev/releases")
}

val shade: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")

    shade("com.google.code.gson:gson:2.13.2")
    shade("dev.firstdark.discordrpc:discord-rpc:1.0.6:legacy")
    shade("org.slf4j:slf4j-api:2.0.16")
    shade("com.lunarclient:apollo-protos:0.0.6")
    shade("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        isTransitive = false
    }
    annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT")

    runtimeOnly("me.djtheredstoner:DevAuth-forge-legacy:1.2.1")
}

// Tasks:
tasks {
    withType(JavaCompile::class) {
        options.encoding = "UTF-8"
        dependsOn("generateJavaTemplates", "generateResourceTemplates")
    }

    withType(ProcessResources::class) {
        dependsOn("generateResourceTemplates")
    }

    withType(Jar::class) {
        archiveBaseName.set("LunarSpoof")
        archiveVersion.set(project.version.toString())
        archiveClassifier.set("without-deps")
        destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
        manifest.attributes.run {
            this["FMLCorePluginContainsFMLMod"] = "true"
            this["ForceLoadAsMod"] = "true"
            this["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
            this["MixinConfigs"] = "mixins.moonlight.json"
        }
    }

    // Remove multi-release Java 20/21 entries before legacy TinyRemapper.
    // A ZIP-level filter is used because the legacy remapper scans every class entry.
    val legacyRemapInputFile = layout.buildDirectory.file(
        "intermediates/LunarSpoof-${project.version}-legacy-remap-input.jar"
    )
    val legacyRemapInput = register("legacyRemapInput") {
        dependsOn(shadowJar)
        inputs.file(shadowJar.get().archiveFile)
        outputs.file(legacyRemapInputFile)
        doLast {
            val outputFile = legacyRemapInputFile.get().asFile
            outputFile.parentFile.mkdirs()
            ZipFile(shadowJar.get().archiveFile.get().asFile).use { zip ->
                ZipOutputStream(
                    BufferedOutputStream(FileOutputStream(outputFile))
                ).use { output ->
                    val seenEntries = HashSet<String>()
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (entry.name.startsWith("META-INF/versions/")) continue
                        if (!seenEntries.add(entry.name)) continue
                        val cleanEntry = ZipEntry(entry.name)
                        if (entry.time >= 0L) cleanEntry.time = entry.time
                        output.putNextEntry(cleanEntry)
                        if (!entry.isDirectory) {
                            zip.getInputStream(entry).use { input -> input.copyTo(output) }
                        }
                        output.closeEntry()
                    }
                }
            }
        }
    }

    val remapJar = named<RemapJarTask>("remapJar") {
        archiveClassifier.set("forge")
        dependsOn(legacyRemapInput)
        input.set(legacyRemapInputFile)
        // The shaded legacy RPC jar is already in the remap input. Do not
        // scan it again as an external classpath jar: it contains Java 21
        // multi-release entries that TinyRemapper 0.10 cannot parse.
        classpath.setFrom(
            configurations.getByName("compileClasspath").filter { file ->
                !file.name.startsWith("discord-rpc-")
            }
        )
    }

    shadowJar {
        destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
        archiveClassifier.set("non-obfuscated-with-deps")
        configurations = listOf(shade)
        // Minecraft 1.8.9 runs on Java 8; keep the Java 8 base classes only.
        // Loom's legacy TinyRemapper cannot parse Java 21 multi-release class files.
        exclude("META-INF/versions/**")

        doLast {
            configurations.forEach {
                println("Copying dependencies into mod: ${it.files}")
            }
        }

        fun relocateInside(name: String) = relocate(name, "org.afterlike.moonlight.lib.$name")

        relocateInside("com.google.gson")
        relocateInside("com.google.protobuf")
        relocateInside("com.lunarclient.apollo")
        relocateInside("dev.firstdark.rpc")
        relocateInside("org.slf4j")
    }

    assemble.get().dependsOn(remapJar)
}

