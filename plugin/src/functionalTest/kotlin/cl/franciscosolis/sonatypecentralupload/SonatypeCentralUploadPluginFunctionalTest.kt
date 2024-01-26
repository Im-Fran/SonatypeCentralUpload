package cl.franciscosolis.sonatypecentralupload

import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class SonatypeCentralUploadPluginFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle.kts") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle.kts") }

    @Test
    fun testSonatypeCentralUpload() {
        val mockJar = File(javaClass.getResource("/sonatypecentraluploadtest-1.0.0.jar")?.file ?: error("Could not find mock jar"))
        val mockSourcesJar = File(javaClass.getResource("/sonatypecentraluploadtest-1.0.0-sources.jar")?.file ?: error("Could not find mock sources jar"))
        val mockJavadocJar = File(javaClass.getResource("/sonatypecentraluploadtest-1.0.0-javadoc.jar")?.file ?: error("Could not find mock javadoc jar"))
        val mockPom = File(javaClass.getResource("/pom.xml")?.file ?: error("Could not find mock pom"))

        // Set up the test build
        settingsFile.writeText("""
            rootProject.name = "SonatypeCentralUploadTest"
        """.trimIndent())
        buildFile.writeText("""
            plugins {
                id("cl.franciscosolis.gradledotenv") version "1.0.1"
                id("cl.franciscosolis.sonatype-central-upload")
            }
            
            group = "cl.franciscosolis"
            version = "1.0.0"
            
            sonatypeCentralUpload {
                username = env["SONATYPE_USERNAME"] ?: ""
                password = env["SONATYPE_PASSWORD"] ?: ""
                
                signingKey = env["SIGNING_KEY"] ?: ""
                signingKeyPassphrase = env["SIGNING_PASSWORD"] ?: ""
                publicKey = env["PUBLIC_KEY"] ?: ""
                
                archives = files("${mockJar.absolutePath}", "${mockSourcesJar.absolutePath}", "${mockJavadocJar.absolutePath}")
                
                pom = file("${mockPom.absolutePath}")
            }
        """.trimIndent())

        // Run the build
        GradleRunner.create()
            .withEnvironment(System.getenv())
            .forwardOutput()
            .withPluginClasspath()
            .withArguments("sonatypeCentralUpload")
            .withProjectDir(projectDir)
            .build()

        assertTrue(File(projectDir, "build/sonatype-central-upload/cl/franciscosolis/sonatypecentraluploadtest/1.0.0/sonatypecentraluploadtest-1.0.0.jar").exists())
        assertTrue(File(projectDir, "build/sonatype-central-upload/cl/franciscosolis/sonatypecentraluploadtest/1.0.0/sonatypecentraluploadtest-1.0.0.jar.md5").exists())
        assertTrue(File(projectDir, "build/sonatype-central-upload/cl/franciscosolis/sonatypecentraluploadtest/1.0.0/sonatypecentraluploadtest-1.0.0.jar.sha1").exists())
        assertTrue(File(projectDir, "build/sonatype-central-upload/cl/franciscosolis/sonatypecentraluploadtest/1.0.0/sonatypecentraluploadtest-1.0.0.jar.sha256").exists())
        assertTrue(File(projectDir, "build/sonatype-central-upload/cl/franciscosolis/sonatypecentraluploadtest/1.0.0/sonatypecentraluploadtest-1.0.0.jar.sha512").exists())
        assertTrue(File(projectDir, "build/sonatype-central-upload/cl/franciscosolis/sonatypecentraluploadtest/1.0.0/sonatypecentraluploadtest-1.0.0.jar.asc").exists())
        assertTrue(File(projectDir, "build/sonatype-central-upload/sonatypecentraluploadtest-1.0.0.zip").exists())
    }
}
