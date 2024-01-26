package cl.franciscosolis.sonatypecentralupload.utils

import org.pgpainless.sop.SOPImpl
import java.io.File
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection

/**
 * Sign the given file and return the signed file.
 * @param file The file to sign.
 * @param signingKey The signing key.
 * @param signingPassword The signing password.
 *
 * @return The signed file, or null if an error occurred.
 */
fun signFile(file: File, signingKey: String, signingPassword: String): File? = try {
    val signedFile = File(file.parentFile, "${file.name}.asc")
    val keyBytes = if(signingKey.contains("PGP PRIVATE KEY BLOCK")) {
        signingKey.replace("\\n", "\n")
            .toByteArray()
    } else if (File(signingKey).exists()) {
        File(signingKey).readText()
            .replace("\\n", "\n")
            .toByteArray()
    } else {
        throw IllegalStateException("Signing key is not a file or a key block.")
    }

    SOPImpl()
        .detachedSign()
        .key(keyBytes)
        .let { sign ->
            if(signingPassword.isNotBlank()) {
                sign.withKeyPassword(signingPassword)
            }
            sign
        }
        .data(file.readBytes())
        .writeTo(signedFile.outputStream())

    signedFile
} catch (e: Exception) {
    e.printStackTrace()
    null
}

/**
 * Sends the given public key to the key server.
 * @param key The public key to send.
 */
fun sendKeyToServer(key: String) {
    val url = URL("https://keyserver.ubuntu.com/pks/add")
    val connection = url.openConnection() as HttpsURLConnection
    connection.requestMethod = "POST"
    connection.addRequestProperty("Content-Type", "application/x-www-form-urlencoded")
    connection.doOutput = true
    connection.outputStream.write("keytext=${URLEncoder.encode(key, Charsets.UTF_8.name())}".toByteArray(Charsets.UTF_8))

    connection.connect()
    val responseCode = connection.responseCode
    if(responseCode != HttpsURLConnection.HTTP_OK) {
        System.err.println("Failed to send key to server. Response code: ${connection.responseCode}")
    }

    if(System.getenv("SONATYPECENTRALUPLOAD_DEBUG") != null) {
        println("Response code: $responseCode")
        println("Response message: ${connection.responseMessage}")
        println("Response body: ${connection.inputStream.bufferedReader().readText()}")
    }
}