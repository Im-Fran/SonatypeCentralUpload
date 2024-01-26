package cl.franciscosolis.sonatypecentralupload.utils

import net.lingala.zip4j.ZipFile
import java.io.File
import java.nio.file.Files

/**
 * Creates a zip of the given folder
 * @param folder The folder to zip.
 * @param output The zip file to create.
 */
fun zipFolder(folder: File, output: File) {
    Files.deleteIfExists(output.toPath())
    val zip = ZipFile(output.absolutePath)
    zip.addFolder(folder)
    zip.close()
}