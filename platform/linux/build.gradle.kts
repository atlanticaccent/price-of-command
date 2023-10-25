import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


/**
 * Where your Starsector game is installed to.
 * Note: On Linux, if you installed Starsector into your home directory, you have to write /home/<user>/ instead of ~/
 */
val starsectorDirectory = "/Users/iain.laird/Documents/starsector_linux"

val jarFileName = "linux.jar"

// Note: On Linux, use "${starsectorDirectory}" as core directory
val starsectorCoreDirectory = starsectorDirectory
val starsectorModDirectory = "${starsectorDirectory}/mods"

// The dependencies for the mod to *build* (not necessarily to run).
dependencies {
    implementation(project(mapOf("path" to ":platform:shared")))
    // If using auto-generated mod_info.json, scroll down and update the "dependencies" part of mod_info.json with
    // any mod dependencies to be displayed in the Starsector launcher.

    // Vanilla Starsector jars and dependencies
    api(fileTree(starsectorCoreDirectory) { include("**/*.jar") })
    // Use all mods in /mods folder to compile (this does not mean the mod requires them to run).
    // LazyLib is needed to use Kotlin, as it provides the Kotlin Runtime, so ensure that that is in your mods folder.
//    compileOnly(fileTree(starsectorModDirectory) {
//        include("**/*.jar")
//        exclude("**/price-of-command.jar")
//    })

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
    named<Jar>("jar")
    {
        // Tells Gradle to put the .jar file in the /jars folder.
        destinationDirectory.set(file("$rootDir/jars"))
        // Sets the name of the .jar file.
        archiveFileName.set(jarFileName)
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
    kotlin("jvm")
    `java-library`
}

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