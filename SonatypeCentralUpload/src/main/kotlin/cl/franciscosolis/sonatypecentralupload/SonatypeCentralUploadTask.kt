package cl.franciscosolis.sonatypecentralupload

import cl.franciscosolis.sonatypecentralupload.utils.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Files


abstract class SonatypeCentralUploadTask: DefaultTask() {

    @get:Input
    abstract val username: Property<String>

    @get:Input
    abstract val password: Property<String>

    @get:Input
    @get:Optional
    abstract val signingKey: Property<String>

    @get:Input
    @get:Optional
    abstract val signingKeyPassphrase: Property<String>

    @get:Input
    @get:Optional
    abstract val publicKey: Property<String>

    @get:Input
    abstract val archives: Property<FileCollection>

    @get:Input
    abstract val pom: Property<File>

    @TaskAction
    fun run() {
        val groupFolder = "${project.group}".replace('.', '/').lowercase()
        val sonatypeCentralUploadDir = project.file(project.layout.buildDirectory.dir("sonatype-central-upload"))
        val uploadDir = project.file(project.layout.buildDirectory.dir("${sonatypeCentralUploadDir.path}/$groupFolder/${project.name.lowercase()}/${project.version}"))
        if (username.orNull?.isBlank() == true) {
            throw IllegalStateException("'username' is empty. A username is required.")
        }

        if (password.orNull?.isBlank() == true) {
            throw IllegalStateException("'password' is empty. A password is required.")
        }

        if (signingKey.orNull?.isBlank() == true) {
            throw IllegalStateException("'signingKey' is empty. A signing key is required.")
        }

        if (!archives.isPresent || archives.orNull?.isEmpty == true) {
            throw IllegalStateException("'archives' is empty. Archives to upload are required.")
        }

        if (!pom.isPresent || pom.orNull == null) {
            throw IllegalStateException("'pom' is empty. A pom file is required.")
        }

        // Create upload dir
        if(!uploadDir.exists()) {
            uploadDir.mkdirs()
        }

        // Get all artifacts
        val artifacts = archives.orNull ?: emptyList()

        // Copy artifacts to upload dir
        for(artifact in artifacts) {
            if(!artifact.nameWithoutExtension.startsWith("${project.name.lowercase()}-${project.version}")) {
                throw IllegalStateException("Artifact name '${artifact.name}' does not match or does not start with project name '${project.name.lowercase()}-${project.version}'.")
            }
            val artifactFile = artifact.toPath()
            val uploadFile = uploadDir.toPath().resolve(artifactFile.fileName)
            Files.copy(artifactFile, uploadFile)
        }

        val pomFile = pom.orNull ?: throw NullPointerException("Pom file is null.")
        Files.copy(pomFile.toPath(), uploadDir.toPath().resolve("${project.name.lowercase()}-${project.version}.pom"))

        if (publicKey.orNull?.isNotBlank() == true) {
            val publicKey = publicKey.orNull ?: ""
            val pkToDistribute = if(publicKey.startsWith("-----BEGIN PGP") && publicKey.contains("KEY BLOCK-----")) {
                publicKey.replace("\\n", "\n")
            } else if (File(publicKey).exists()) {
                File(uploadDir, "public.key").readText().replace("\\n", "\n")
            } else {
                throw IllegalStateException("'publicKey' is not a file or a key block.")
            }

            sendKeyToServer(pkToDistribute)
        }

        // Generate signatures for all files
        for(file in uploadDir.listFiles() ?: emptyArray()) {
            signFile(
                file = file,
                signingKey = signingKey.orNull ?: "",
                signingPassword = signingKeyPassphrase.orNull ?: ""
            )
        }

        // Generate checksums for all files (filter out .asc files)
        for (file in (uploadDir.listFiles() ?: emptyArray()).filter { it.extension != "asc" }) {
            generateChecksums(file)
        }

        // Now we need to zip all the contents of 'sonatype-central-upload'
        val zipFile = File(sonatypeCentralUploadDir, "${project.name.lowercase()}-${project.version}.zip")
        zipFolder(File(sonatypeCentralUploadDir, groupFolder.split('/').first()), zipFile)

        initPublishingProcess(zipFile, username.orNull ?: "", password.orNull ?: "")
    }



}