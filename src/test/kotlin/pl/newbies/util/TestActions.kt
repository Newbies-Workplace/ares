package pl.newbies.util

import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.request.forms.submitForm
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import pl.newbies.auth.application.model.AuthResponse
import pl.newbies.auth.application.model.GithubUser
import pl.newbies.lecture.application.model.LectureRequest
import pl.newbies.lecture.application.model.LectureResponse
import pl.newbies.tag.application.model.TagCreateRequest
import pl.newbies.tag.application.model.TagRequest
import pl.newbies.tag.application.model.TagResponse
import pl.newbies.user.application.model.UserResponse
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
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
    request: TagCreateRequest = TagCreateRequest(name = UUID.randomUUID().toString()),
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

suspend fun ApplicationTestBuilder.createLecture(
    authResponse: AuthResponse,
    request: LectureRequest = TestData.createLectureRequest()
): LectureResponse {
    val response = httpClient.post("api/v1/lectures") {
        setBody(request)
        contentType(ContentType.Application.Json)
        bearerAuth(authResponse.accessToken)
    }

    return response.body()
}

suspend fun ApplicationTestBuilder.getUser(id: String): UserResponse {
    val response = httpClient.get("api/v1/users/$id")

    return response.body()
}

fun removeDirectory(path: String) {
    val storagePath = Path.of("ares-test-storage").resolve(path)

    println("[Test] Removing directory ${storagePath.toFile().path}")

        Files.walk(storagePath)
            .sorted(Comparator.reverseOrder())
            .map { it.toFile() }
            .forEach { it.delete() }
}

fun assertFileExists(path: String) {
    val storagePath = Path.of("ares-test-storage").resolve(path)
    print("[Test] asserting directory exists (${storagePath.toFile().path})")

    assert(storagePath.exists()) { "Expected file to exist (${storagePath.toFile().path})" }
}

fun getResourceFile(path: String): File =
    Path("src/test/resources/").resolve(path).toFile()
