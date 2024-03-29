package pl.newbies.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.SerializationException
import org.valiktor.ConstraintViolationException
import pl.newbies.auth.domain.UnauthorizedException
import pl.newbies.common.DuplicateException
import pl.newbies.common.FileTypeNotSupportedException
import pl.newbies.common.ForbiddenException
import pl.newbies.common.NotFoundException
import pl.newbies.lecture.domain.SpeakerAlreadyInLectureException
import pl.newbies.lecture.domain.TooManyLectureInvitesException

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<SerializationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, cause.message.orEmpty())
        }
        exception<DuplicateException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, cause.message.orEmpty())
        }
        exception<TooManyLectureInvitesException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, cause.message.orEmpty())
        }
        exception<SpeakerAlreadyInLectureException> { call, cause ->
            call.respond(HttpStatusCode.Conflict, cause.message.orEmpty())
        }
        exception<ConstraintViolationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, cause.constraintViolations.toString())
        }
        exception<UnauthorizedException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized, cause.message.orEmpty())
        }
        exception<NotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, cause.message.orEmpty())
        }
        exception<ForbiddenException> { call, cause ->
            call.respond(HttpStatusCode.Forbidden, cause.message.orEmpty())
        }
        exception<FileTypeNotSupportedException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, cause.message.orEmpty())
        }
    }
}