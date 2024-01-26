package cl.franciscosolis.sonatypecentralupload

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

class SonatypeCentralUploadPluginTest {
    @Test
    fun `plugin registers task`() {
        // Create a test project and apply the plugin
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("cl.franciscosolis.sonatype-central-upload")

        // Verify the result
        assertNotNull(project.tasks.findByName("sonatypeCentralUpload"))
    }
}
