import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-gradle-plugin`
    alias(libs.plugins.jvm)
    alias(libs.plugins.plugin.publish)
}

version = "2.0.0"
group = "cl.franciscosolis"

// Set up the publishing plugin
if(System.getenv("GRADLE_PUBLISH_KEY") != null && System.getenv("GRADLE_PUBLISH_SECRET") != null) {
    System.setProperty("gradle.publish.key", System.getenv("GRADLE_PUBLISH_KEY"))
    System.setProperty("gradle.publish.secret", System.getenv("GRADLE_PUBLISH_SECRET"))
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.pgpainless.sop)
    implementation(libs.zip4j)
    implementation(libs.gson)

    // Use the Kotlin JUnit 5 integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

gradlePlugin {
    website = "https://github.com/Im-Fran/SonatypeCentralUpload"
    vcsUrl = "https://github.com/Im-Fran/SonatypeCentralUpload"

    // Define the plugin
    plugins.create("sonatypeCentralUpload") {
        id = "cl.franciscosolis.sonatype-central-upload"
        implementationClass = "cl.franciscosolis.sonatypecentralupload.SonatypeCentralUploadPlugin"
        displayName = "Sonatype Central Upload"
        version = project.version
        description = "A Gradle plugin to upload artifacts to Sonatype Central."
        tags = listOf("sonatype", "central", "upload", "publish", "maven")
    }
}

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest")
configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])
configurations["functionalTestRuntimeOnly"].extendsFrom(configurations["testRuntimeOnly"])

// Add a task to run the functional tests
val functionalTest = tasks.register<Test>("functionalTest") {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
    useJUnitPlatform()
}

gradlePlugin.testSourceSets.add(functionalTestSourceSet)

tasks {
    named<Task>("check") {
        // Run the functional tests as part of `check`
        dependsOn(functionalTest)
    }

}

// Gradle 9 requires Java 17 to run, so there's no point targeting anything older
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}
