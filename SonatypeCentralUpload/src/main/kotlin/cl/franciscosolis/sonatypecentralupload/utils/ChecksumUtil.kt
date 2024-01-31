package cl.franciscosolis.sonatypecentralupload.utils

import java.io.File
import java.security.MessageDigest

/**
 * Generates the checksums for the given file.
 * @param file The file to generate the checksums for.
 *
 * @return A list of files containing the checksums.
 */
fun generateChecksums(file: File): List<File> = mutableListOf<File>().apply {
    listOf("MD5", "SHA-1", "SHA-256", "SHA-512").forEach { algorithm ->
        MessageDigest.getInstance(algorithm).let { digest ->
            digest.reset()
            digest.update(file.readBytes())
            add(File(file.parentFile, "${file.name}.${algorithm.lowercase().replace("-", "")}").apply {
                writeText(digest.digest().joinToString("") { byte -> "%02x".format(byte) })
            })
        }
    }
}