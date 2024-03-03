import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`
    alias(libs.plugins.jvm)
    id("com.gradle.plugin-publish") version "1.2.1"
}

version = "1.0.3"
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
    implementation("org.pgpainless:pgpainless-sop:1.6.5")
    implementation("net.lingala.zip4j:zip4j:2.11.5")
    implementation("com.google.code.gson:gson:2.10.1")

    // Use the Kotlin JUnit 5 integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

gradlePlugin {
    website = "https://github.com/Im-Fran/SonatypeCentralUpload"
    vcsUrl = "https://github.com/Im-Fran/SonatypeCentralUpload"

    // Define the plugin
    val sonatypeCentralUpload by plugins.creating {
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
val functionalTest by tasks.registering(Test::class) {
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

    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "11"
        }
    }
}


configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
    }
}