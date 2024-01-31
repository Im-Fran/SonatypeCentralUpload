package cl.franciscosolis.sonatypecentralupload

import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import java.io.File

abstract class SonatypeCentralUploadExtension {
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
}