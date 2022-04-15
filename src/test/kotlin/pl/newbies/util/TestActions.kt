package pl.newbies.util

import io.ktor.client.call.body
import io.ktor.client.plugins.onUpload
import io.ktor.client.request.*
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import io.ktor.server.testing.ApplicationTestBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import pl.newbies.auth.application.model.AuthResponse
import pl.newbies.auth.application.model.GithubUser
import pl.newbies.common.nanoId
import pl.newbies.event.application.model.EventRequest
import pl.newbies.event.application.model.EventResponse
import pl.newbies.event.application.model.EventVisibilityRequest
import pl.newbies.event.domain.model.Event
import pl.newbies.tag.application.model.TagCreateRequest
import pl.newbies.tag.application.model.TagRequest
import pl.newbies.tag.application.model.TagResponse
import pl.newbies.user.application.model.UserResponse
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.exists

suspend fun ApplicationTestBuilder.loginAs(
    githubUser: GithubUser,
): AuthResponse {
    val response = httpClient.submitForm(
        url = "/oauth/callback/github",
        formParameters = Parameters.build {
            append("code", "valid")
            append("state", githubUser.id)
        },
        encodeInQuery = true,
    )

    return response.body()
}

suspend fun ApplicationTestBuilder.createTag(
    authResponse: AuthResponse,
    request: TagCreateRequest = TagCreateRequest(name = nanoId()),
): TagResponse {
    val response = httpClient.post("api/v1/tags") {
        setBody(request)
        contentType(ContentType.Application.Json)
        bearerAuth(authResponse.accessToken)
    }

    return response.body()
}

suspend fun ApplicationTestBuilder.followTags(
    authResponse: AuthResponse,
    vararg ids: String,
): List<TagResponse> {
    val response = httpClient.put("api/v1/tags/@me") {
        setBody(ids.map { TagRequest(it) })
        contentType(ContentType.Application.Json)
        bearerAuth(authResponse.accessToken)
    }

    return response.body()
}

suspend fun ApplicationTestBuilder.createEvent(
    authResponse: AuthResponse,
    request: EventRequest = TestData.createEventRequest(),
    visibility: Event.Visibility? = Event.Visibility.PUBLIC,
): EventResponse {
    val event = httpClient.post("api/v1/events") {
        setBody(request)
        contentType(ContentType.Application.Json)
        bearerAuth(authResponse.accessToken)
    }.body<EventResponse>()

    visibility?.let {
        changeVisibility(
            authResponse = authResponse,
            eventId = event.id,
            visibility = visibility,
        )
    }

    return event
}

suspend fun ApplicationTestBuilder.changeVisibility(
    authResponse: AuthResponse,
    eventId: String,
    visibility: Event.Visibility
): EventResponse {
    val response = httpClient.put("api/v1/events/$eventId/visibility") {
        setBody(EventVisibilityRequest(visibility))
        contentType(ContentType.Application.Json)
        bearerAuth(authResponse.accessToken)
    }

    assertEquals(HttpStatusCode.OK, response.status)
    return response.body()
}

suspend fun ApplicationTestBuilder.getUser(id: String): UserResponse {
    val response = httpClient.get("api/v1/users/$id")

    assertEquals(HttpStatusCode.OK, response.status)
    return response.body()
}

suspend fun ApplicationTestBuilder.getEvent(id: String): EventResponse {
    val response = httpClient.get("api/v1/events/$id")

    assertEquals(HttpStatusCode.OK, response.status)
    return response.body()
}

suspend fun ApplicationTestBuilder.addEventImage(
    authResponse: AuthResponse,
    eventId: String,
    imagePath: String,
    contentType: String,
    fileName: String,
): HttpResponse {
    val response = httpClient.put("/api/v1/events/$eventId/theme/image") {
        bearerAuth(authResponse.accessToken)
        setBody(
            MultiPartFormDataContent(
                parts = formData {
                    append(
                        key = "image",
                        value = getResourceFile(imagePath).readBytes(),
                        headers = Headers.build {
                            append(HttpHeaders.ContentType, contentType)
                            append(HttpHeaders.ContentDisposition, "filename=$fileName")
                        },
                    )
                },
            )
        )
        onUpload { bytesSentTotal, contentLength ->
            println("Sent $bytesSentTotal bytes from $contentLength")
        }
    }

    return response
}

fun removeDirectory(path: String) {
    val storagePath = Path.of("ares-test-storage").resolve(path)

    println("[Test] Removing directory ${storagePath.toFile().path}")

    runCatching {
        Files.walk(storagePath)
            .sorted(Comparator.reverseOrder())
            .map { it.toFile() }
            .forEach { it.delete() }
    }
}

fun assertFileNotExists(path: String) {
    val storagePath = Path.of("ares-test-storage").resolve(path)
    print("[Test] asserting directory NOT exists (${storagePath.toFile().path})")

    assert(!storagePath.exists()) { "Expected file not to exist (${storagePath.toFile().path})" }
}

fun assertFileExists(path: String) {
    val storagePath = Path.of("ares-test-storage").resolve(path)
    print("[Test] asserting directory exists (${storagePath.toFile().path})")

    assert(storagePath.exists()) { "Expected file to exist (${storagePath.toFile().path})" }
}

fun getResourceFile(path: String): File =
    Path("src/test/resources/").resolve(path).toFile()
