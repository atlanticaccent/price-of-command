import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


/**
 * RECOMMENDED VARIABLES TO CHANGE.
 */
/**
 * The name of your mod. Used to create a mod folder name (and the name of your mod, if using auto-updated mod_info.json).
 * Defaults to the name of the mod's folder.
 */
val modName: String = rootDir.name
val niceName = "Price of Command"

/**
 * Where your Starsector game is installed to.
 * Note: On Linux, if you installed Starsector into your home directory, you have to write /home/<user>/ instead of ~/
 */
//val starsectorDirectory = "sources/win"

// Defaults to the name of your mod, with spaces replaced by hyphens.
val modFolderName = modName.replace(" ", "-")

val shouldAutomaticallyCreateMetadataFiles = true
/**
 * END OF RECOMMENDED VARIABLES TO CHANGE.
 */

/**
 * Modify these if you wish to have mod_info.json and the Version Checker files updated for you automatically.
 */
val modVersion = "0.0.5-RC2"
val jarFileName = "${modName}.jar"
val modId = "price_of_command"
val modAuthor = "AtlanticAccent"
val modDescription = "Price of Command. Adds Fatigue, Injuries, Grave Injuries and Death to Officers."
val gameVersion = "0.96a-RC10"
val jars = arrayOf("jars/$jarFileName")
val modPlugin = "com.price_of_command.OfficerExpansionPlugin"
val isUtilityMod = false
val masterVersionFile = "https://raw.githubusercontent.com/atlanticaccent/officer-expansion/master/$modId.version"
val modThreadId = "00000"
//////////////////////

// Note: On Linux, use "${starsectorDirectory}" as core directory
val (starsectorCoreDirectory, starsectorDirectory) = run {
    val starsectorDirectory: String
    val os = System.getProperty("os.name").toLowerCase()
    when {
        os.contains("win") -> {
            starsectorDirectory = "sources/windows"
            "${starsectorDirectory}/starsector-core" to starsectorDirectory
        }
        os.contains("nix") || os.contains("nux") || os.contains("aix") -> {
            starsectorDirectory = "sources/linux"
            starsectorDirectory to starsectorDirectory
        }
        os.contains("mac") -> {
            starsectorDirectory = "sources/Starsector.app"
            "${starsectorDirectory}/Contents/Resources/Java" to starsectorDirectory
        }
        else -> throw Exception("Unknown platform")
    }
}
val starsectorModDirectory = "${starsectorDirectory}/mods"
val modInModsFolder = File("$starsectorModDirectory/${modFolderName}")
val modFiles = modInModsFolder.listFiles()

configurations {
    create("jarLibs")
}

// The dependencies for the mod to *build* (not necessarily to run).
dependencies {
    api(project(mapOf("path" to ":platform:windows")))
    api(project(mapOf("path" to ":platform:macos")))
    api(project(mapOf("path" to ":platform:linux")))
    api(project(mapOf("path" to ":platform:shared")))
    // If using auto-generated mod_info.json, scroll down and update the "dependencies" part of mod_info.json with
    // any mod dependencies to be displayed in the Starsector launcher.

    // Vanilla Starsector jars and dependencies
    compileOnly(fileTree(starsectorCoreDirectory) { include("**/*.jar") })
    // Use all mods in /mods folder to compile (this does not mean the mod requires them to run).
    // LazyLib is needed to use Kotlin, as it provides the Kotlin Runtime, so ensure that that is in your mods folder.
    compileOnly(fileTree(starsectorModDirectory) {
        include("**/*.jar")
        exclude("**/$jarFileName")
    })

    // Add any specific library dependencies needed by uncommenting and modifying the below line to point to the folder of the .jar files.
    // All mods in the /mods folder are already included, so this would be for anything outside /mods.
//    compileOnly(fileTree("C:/jars") { include("*.jar") })

    // Shouldn't need to change anything in dependencies below here
    implementation(fileTree("libs") { include("*.jar") })

    val kotlinVersionInLazyLib = "1.6.21"
    // Get kotlin sdk from LazyLib during runtime, only use it here during compile time
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersionInLazyLib")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersionInLazyLib")
}

tasks {
    clean {
        File(projectDir, "jars").deleteRecursively()
    }

    named<Jar>("jar")
    {
        dependsOn(getTasksByName("jar", true).filter { this.path != it.path })
        // Copy platform jars in to create thin-fat jar
        fileTree("jars") {
            include("*.jar")
            exclude(jarFileName)
        }.forEach {
            from(zipTree(it.absoluteFile))
        }
        // Tells Gradle to put the .jar file in the /jars folder.
        destinationDirectory.set(file("$rootDir/jars"))
        // Sets the name of the .jar file.
        archiveFileName.set(jarFileName)
    }

    register("create-metadata-files") {
        val version = modVersion.split(".").let { javaslang.Tuple3(it[0], it[1], it[2]) }
        System.setProperty("line.separator", "\n") // Use LF instead of CRLF like a normal person

        if (shouldAutomaticallyCreateMetadataFiles) {
            // Generates a mod_info.json from the variables defined at the top of this script.
            File(projectDir, "mod_info.json")
                .writeText(
                    """
                    # THIS FILE IS GENERATED BY build.gradle.kts. (Note that Starsector's json parser permits `#` for comments)
                    {
                        "id": "$modId",
                        "name": "$niceName",
                        "author": "$modAuthor",
                        "utility": "$isUtilityMod",
                        "version": { "major":"${version._1}", "minor": "${version._2}", "patch": "${version._3}" },
                        "description": "$modDescription",
                        "gameVersion": "$gameVersion",
                        "jars": [
                            ${jars.joinToString { "\"$it\"" }},
                            "jars/windows.jar",
                            "jars/macos.jar",
                            "jars/linux.jar",
                            "jars/shared.jar"
                        ],
                        "modPlugin":"$modPlugin",
                        "dependencies": [
                            {
                                "id": "lw_lazylib",
                                "name": "LazyLib",
                                # "version": "2.6" # If a specific version or higher is required, include this line
                            },
                            {
                                "id": "lunalib",
                                "name": "LunaLib",
                            },
                            {
                                "id": "MagicLib",
                                "name": "MagicLib",
                            },
                        ]
                    }
                """.trimIndent()
                )

            // Generates a Version Checker csv file from the variables defined at the top of this script.
            with(File(projectDir, "data/config/version/version_files.csv")) {
                this.parentFile.mkdirs()
                this.writeText(
                    """
                    version file
                    ${modId}.version

                """.trimIndent()
                )
            }


            // Generates a Version Checker .version file from the variables defined at the top of this script.
            File(projectDir, "${modId}.version")
                .writeText(
                    """
                    {
                        "masterVersionFile":"$masterVersionFile",
                        "modName":"$niceName",
                        "modThreadId":${modThreadId},
                        "modVersion":
                        {
                            "major":${version._1},
                            "minor":${version._2},
                            "patch":${version._3}
                        }
                    }
                """.trimIndent()
                )
        }

        // Creates a file with the mod name to tell the Github Actions script the name of the mod.
        // Not needed if not using Github Actions (but doesn't hurt to keep).
        with(File(projectDir, ".github/workflows/mod-folder-name.txt")) {
            this.parentFile.mkdirs()
            this.writeText(modFolderName)
        }
    }

    register<Delete>("uninstall-mod") {
        val enabled = true

        if (!enabled) return@register

        println("Deleting old version...")

        modInModsFolder.deleteRecursively()
    }

    // If enabled, will copy your mod to the /mods directory when run (and whenever gradle syncs).
    // Disabled by default, as it is not needed if your mod directory is symlinked into your /mods folder.
    register<Copy>("install-mod") {
        val enabled = true

        if (!enabled) return@register

        println("Installing mod into Starsector mod folder...")

        val destinations = listOf(modInModsFolder)

        destinations.forEach { dest ->
            copy {
                from(projectDir)
                into(dest)
                exclude(".git", ".github", ".gradle", ".idea", ".run", "gradle")
                exclude(".gitignore", "build.gradle.kts", "*gradle*", "README.md")
                exclude("build")
                exclude("jars/linux.jar")
                exclude("jars/windows.jar")
                exclude("jars/macos.jar")
                exclude("jars/shared.jar")
                exclude("sources")
            }
        }
    }

    register<Zip>("zip") {
        dependsOn("jar")

        from(projectDir)
        exclude(".git", ".github", ".gradle", ".idea", ".run", "gradle")
        exclude(".gitignore", "build.gradle.kts", "*gradle*", "README.md")
        exclude("build")
        exclude("sources")

        archiveFileName.set("price-of-command.zip")
    }
}

sourceSets.main {
    // List of where your Java source code is, if any.
    java.setSrcDirs(listOf("src"))
}
kotlin.sourceSets.main {
    // List of where your Kotlin source code is, if any.
    kotlin.setSrcDirs(listOf("src"))
    // List of where resources (the "data" folder) are.
    resources.setSrcDirs(listOf("data"))
}

// Don't touch stuff below here unless you know what you're doing.
plugins {
    kotlin("jvm") version "1.5.31"
    java
    `java-library`
}

version = modVersion

repositories {
    maven(url = uri("$projectDir/libs"))
    jcenter()
}

// Compile to Java 6 bytecode so that Starsector can use it (options are only 6 or 8)
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.6"
}
// Compile to Java 7 bytecode so that Starsector can use it
java {
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7
}