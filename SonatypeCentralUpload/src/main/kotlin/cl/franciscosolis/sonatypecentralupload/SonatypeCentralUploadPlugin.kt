package cl.franciscosolis.sonatypecentralupload

import org.gradle.api.Plugin
import org.gradle.api.Project

class SonatypeCentralUploadPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("sonatypeCentralUpload", SonatypeCentralUploadTask::class.java)
    }
}
