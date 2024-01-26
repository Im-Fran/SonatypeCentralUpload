package cl.franciscosolis.sonatypecentralupload.utils

import com.google.gson.JsonParser
import java.io.File
import java.net.URL
import java.net.URLEncoder
import java.util.*
import javax.net.ssl.HttpsURLConnection

/**
 * Initializes the publishing process in Sonatype Central.
 * @param file The file to upload.
 * @param deploymentName The deployment name to use.
 * @param username The username to use.
 * @param password The password to use.
 */
fun initPublishingProcess(file: File, deploymentName: String, username: String, password: String) {
    val authorizationHeader = "Basic ${Base64.getEncoder().encodeToString("$username:$password".toByteArray())}"

    val deploymentId = uploadToCentral(file, deploymentName, authorizationHeader)

    var status: String
    do {
        status = deploymentStatus(deploymentId, authorizationHeader)
        when (status) {
            "FAILED" -> {
                throw IllegalStateException("Failed to upload to Sonatype Central. Deployment ID: $deploymentId")
            }
            "VALIDATED" -> {
                println("[Sonatype Central Upload] Deployment verified. Waiting 5 seconds before publishing deployment...")
                Thread.sleep(5000)
            }
            else -> {
                println("[Sonatype Central Upload] Deployment status: $status. Waiting 5 seconds before checking status again...")
                Thread.sleep(5000)
            }
        }
    } while (status != "VALIDATED" && status != "PUBLISHED")

    publishDeployment(deploymentId, authorizationHeader)
}

/**
 * Uploads the given zipFile into Sonatype Central
 * @param file The zipFile to upload.
 * @param deploymentName The deployment id to use.
 * @param authorizationHeader The authorization header to use.
 * @return The deployment id.
 */
private fun uploadToCentral(file: File, deploymentName: String, authorizationHeader: String): String{
    println("[Sonatype Central Upload] Uploading to Sonatype Central...")
    val url = URL("https://central.sonatype.com/api/v1/publisher/upload?name=${URLEncoder.encode(deploymentName, Charsets.UTF_8)}&publishingType=AUTOMATIC")
    val connection = url.openConnection() as HttpsURLConnection
    connection.requestMethod = "POST"
    connection.doOutput = true
    connection.setRequestProperty("Authorization", authorizationHeader)
    connection.setRequestProperty("Content-Type", "multipart/form-data")
    connection.setRequestProperty("Accept", "text/plain")
    connection.setRequestProperty("User-Agent", "Gradle Sonatype Central Upload Plugin")
    connection.setRequestProperty("Content-Length", file.length().toString())
    connection.outputStream.write(file.readBytes())
    connection.outputStream.flush()
    connection.outputStream.close()
    connection.connect()
    if(connection.responseCode != 201) {
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
 * Publishes the given deployment after it has been verified.
 * @param deploymentId The deployment id to publish.
 * @param authorizationHeader The authorization header to use.
 */
private fun publishDeployment(deploymentId: String, authorizationHeader: String) {
    println("[Sonatype Central Upload] Publishing deployment...")
    val publishUrl = URL("https://central.sonatype.com/api/v1/publisher/deployment/$deploymentId")
    val publishConnection = publishUrl.openConnection() as HttpsURLConnection
    publishConnection.requestMethod = "POST"
    publishConnection.setRequestProperty("Authorization", authorizationHeader)
    publishConnection.setRequestProperty("Content-Type", "application/json")
    publishConnection.setRequestProperty("Accept", "*/*")
    publishConnection.setRequestProperty("User-Agent", "Gradle Sonatype Central Upload Plugin")
    publishConnection.connect()

    if (publishConnection.responseCode != 200) {
        throw IllegalStateException("Failed to publish deployment. Response code: ${publishConnection.responseCode}. Response message: ${publishConnection.responseMessage}.")
    }

    println("[Sonatype Central Upload] Successfully published deployment.")
}