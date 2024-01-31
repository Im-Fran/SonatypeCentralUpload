package cl.franciscosolis.sonatypecentralupload

import cl.franciscosolis.sonatypecentralupload.utils.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.nio.file.Files

class SonatypeCentralUploadPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        project.extensions.create("sonatypeCentralUpload", SonatypeCentralUploadExtension::class.java)
        project.tasks.register("sonatypeCentralUpload") { task ->
            val extension = project.extensions.getByType(SonatypeCentralUploadExtension::class.java)
            val groupFolder = "${project.group}".replace('.', '/').lowercase()
            val sonatypeCentralUploadDir = project.file(project.layout.buildDirectory.dir("sonatype-central-upload"))
            val uploadDir = project.file(project.layout.buildDirectory.dir("${sonatypeCentralUploadDir.path}/$groupFolder/${project.name.lowercase()}/${project.version}"))
            if (extension.username.orNull?.isBlank() == true) {
                throw IllegalStateException("'username' is empty. A username is required.")
            }

            if (extension.password.orNull?.isBlank() == true) {
                throw IllegalStateException("'password' is empty. A password is required.")
            }

            if (extension.signingKey.orNull?.isBlank() == true) {
                throw IllegalStateException("'signingKey' is empty. A signing key is required.")
            }

            if (!extension.archives.isPresent || extension.archives.orNull?.isEmpty == true) {
                throw IllegalStateException("'archives' is empty. Archives to upload are required.")
            }

            if (!extension.pom.isPresent || extension.pom.orNull == null) {
                throw IllegalStateException("'pom' is empty. A pom file is required.")
            }

            task.doLast {
                // Create upload dir
                if(!uploadDir.exists()) {
                    uploadDir.mkdirs()
                }

                // Get all artifacts
                val artifacts = extension.archives.orNull ?: emptyList()

                // Copy artifacts to upload dir
                for(artifact in artifacts) {
                    if(!artifact.nameWithoutExtension.startsWith("${project.name.lowercase()}-${project.version}")) {
                        throw IllegalStateException("Artifact name '${artifact.name}' does not match or does not start with project name '${project.name.lowercase()}-${project.version}'.")
                    }
                    val artifactFile = artifact.toPath()
                    val uploadFile = uploadDir.toPath().resolve(artifactFile.fileName)
                    Files.copy(artifactFile, uploadFile)
                }

                val pomFile = extension.pom.orNull ?: throw NullPointerException("Pom file is null.")
                Files.copy(pomFile.toPath(), uploadDir.toPath().resolve("${project.name.lowercase()}-${project.version}.pom"))

                if (extension.publicKey.orNull?.isNotBlank() == true) {
                    val publicKey = extension.publicKey.orNull ?: ""
                    val pkToDistribute = if(publicKey.startsWith("-----BEGIN PGP PUBLIC KEY BLOCK-----")) {
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
                        signingKey = extension.signingKey.orNull ?: "",
                        signingPassword = extension.signingKeyPassphrase.orNull ?: ""
                    )
                }

                // Generate checksums for all files (filter out .asc files)
                for (file in (uploadDir.listFiles() ?: emptyArray()).filter { it.extension != "asc" }) {
                    generateChecksums(file)
                }

                // Now we need to zip all the contents of 'sonatype-central-upload'
                val zipFile = File(sonatypeCentralUploadDir, "${project.name.lowercase()}-${project.version}.zip")
                zipFolder(File(sonatypeCentralUploadDir, groupFolder.split('/').first()), zipFile)

                initPublishingProcess(zipFile, extension.username.orNull ?: "", extension.password.orNull ?: "")
            }
        }
    }
}
