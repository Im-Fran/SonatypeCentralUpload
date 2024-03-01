package cl.franciscosolis.sonatypecentralupload.utils

import com.google.gson.JsonParser
import java.io.File
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection

private const val manualPublishingOption = "USER_MANAGED"

/**
 * Initializes the publishing process in Sonatype Central.
 * @param file The file to upload.
 * @param username The username to use.
 * @param password The password to use.
 * @param publishingType The publishing type strategy to use.
 */
fun initPublishingProcess(file: File, username: String, password: String, publishingType: String) {
    val authorizationHeader = "UserToken ${Base64.getEncoder().encodeToString("$username:$password".toByteArray())}"

    val deploymentId = uploadToCentral(file, authorizationHeader, publishingType)

    var loops = 0
    var status: String
    do {
        status = deploymentStatus(deploymentId, authorizationHeader)
        when (status) {
            "FAILED" -> {
                throw IllegalStateException("Failed to upload to Sonatype Central. Deployment ID: $deploymentId")
            }
            "VALIDATED" -> {
                if (manualPublishingOption == publishingType) {
                    println("[Sonatype Central Upload] Deployment verified! You need to publish manually on your Sonatype Central Account.")
                } else {
                    println("[Sonatype Central Upload] Deployment verified! Now we wait for publishing...")
                    Thread.sleep(5000)
                }
            }
            "PUBLISHED" -> {
                println("[Sonatype Central Upload] Deployment published!")
            }
            else -> {
                if(loops >= 3) {
                    // Assume everything is ok
                    println("[Sonatype Central Upload] Deployment status: $status. Assuming everything is ok. For further information please check your Sonatype Central account (usually it takes 5-7 minutes to publish).")
                    break
                }

                println("[Sonatype Central Upload] Deployment status: $status. Waiting 5 seconds before checking status again...")
                loops++
                Thread.sleep(5000)
            }
        }
    } while (!hasDeploymentFinished(status, publishingType))
}

/**
 * Uploads the given zipFile into Sonatype Central
 * @param file The zipFile to upload.
 * @param authorizationHeader The authorization header to use.
 * @param publishingType The publishing type strategy to use.
 * @return The deployment id.
 */
private fun uploadToCentral(file: File, authorizationHeader: String, publishingType: String): String {
    println("[Sonatype Central Upload] Uploading to Sonatype Central...")
    val url = URL("https://central.sonatype.com/api/v1/publisher/upload?publishingType=$publishingType")
    val connection = url.openConnection() as HttpsURLConnection
    connection.requestMethod = "POST"
    connection.doOutput = true
    connection.setRequestProperty("Authorization", authorizationHeader)
    connection.setRequestProperty("Content-Type", "multipart/form-data")
    connection.setRequestProperty("Accept", "text/plain")
    connection.setRequestProperty("User-Agent", "Gradle Sonatype Central Upload Plugin")

    // Updated content type and modified payload to include "bundle" parameter
    val boundary = "*****" + System.currentTimeMillis() + "*****"
    connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")

    val outputStream = connection.outputStream
    val writer = outputStream.bufferedWriter()

    writer.write("--$boundary\r\n")
    writer.write("Content-Disposition: form-data; name=\"bundle\"; filename=\"${file.name}\"\r\n")
    writer.write("Content-Type: application/octet-stream\r\n\r\n")
    writer.flush()

    file.inputStream().copyTo(outputStream)

    writer.write("\r\n--$boundary--\r\n")
    writer.flush()

    writer.close()
    outputStream.close()

    connection.connect()

    if (connection.responseCode != 201) {
        throw IllegalStateException("Failed to upload to Sonatype Central. Response code: ${connection.responseCode}. Response message: ${connection.responseMessage}.")
    }

    val deploymentId = String(connection.inputStream.readAllBytes(), Charsets.UTF_8)
    println("[Sonatype Central Upload] Successfully uploaded to Sonatype Central. Deployment ID: $deploymentId")
    return deploymentId
}

/**
 * Gets the status of the given deployment
 * @param deploymentId The deployment id to check.
 * @param authorizationHeader The authorization header to use.
 * @return The status of the deployment.
 */
private fun deploymentStatus(deploymentId: String, authorizationHeader: String): String {
    println("[Sonatype Central Upload] Checking status of deployment...")
    val statusUrl = URL("https://central.sonatype.com/api/v1/publisher/status?id=$deploymentId")
    val statusConnection = statusUrl.openConnection() as HttpsURLConnection
    statusConnection.requestMethod = "POST"
    statusConnection.setRequestProperty("Authorization", authorizationHeader)
    statusConnection.setRequestProperty("Content-Type", "application/json")
    statusConnection.setRequestProperty("Accept", "application/json")
    statusConnection.setRequestProperty("User-Agent", "Gradle Sonatype Central Upload Plugin")
    statusConnection.connect()

    if(statusConnection.responseCode != 200) {
        throw IllegalStateException("Failed to get status of deployment. Response code: ${statusConnection.responseCode}. Response message: ${statusConnection.responseMessage}.")
    }

    val response = String(statusConnection.inputStream.readAllBytes(), Charsets.UTF_8)
    val json = JsonParser.parseString(response).asJsonObject
    return json["deploymentState"].asString
}

/**
 * Returns if the deployment has finished.
 * @param status The status of the deployment.
 * @param publishingType The publishing type strategy to use.
 * @return true if the deployment has finished.
 */
private fun hasDeploymentFinished(status: String, publishingType: String): Boolean =
    (manualPublishingOption == publishingType && "VALIDATED" == status) || "PUBLISHED" == status