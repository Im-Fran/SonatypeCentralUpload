package cl.franciscosolis.sonatypecentralupload

import org.gradle.api.Plugin
import org.gradle.api.Project

class SonatypeCentralUploadPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        val extension: SonatypeCentralUploadExtension = project.extensions.create("sonatypeCentralUpload", SonatypeCentralUploadExtension::class.java)
        project.tasks.register("sonatypeCentralUpload", SonatypeCentralUploadTask::class.java) { task ->
            task.username.set(extension.username)
            task.password.set(extension.password)
            task.signingKey.set(extension.signingKey)
            task.signingKeyPassphrase.set(extension.signingKeyPassphrase)
            task.publicKey.set(extension.publicKey)
            task.archives.set(extension.archives)
            task.pom.set(extension.pom)
        }
    }
}
