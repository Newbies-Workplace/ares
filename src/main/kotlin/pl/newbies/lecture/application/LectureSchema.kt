package pl.newbies.lecture.application

import com.apurebase.kgraphql.Context
import com.apurebase.kgraphql.schema.dsl.SchemaBuilder
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import pl.newbies.common.pagination
import pl.newbies.common.principal
import pl.newbies.lecture.application.model.LectureRequest
import pl.newbies.lecture.application.model.LectureResponse
import pl.newbies.lecture.domain.LectureNotFoundException
import pl.newbies.lecture.domain.service.LectureService
import pl.newbies.lecture.infrastructure.repository.LectureDAO
import pl.newbies.lecture.infrastructure.repository.LectureFollows
import pl.newbies.lecture.infrastructure.repository.Lectures
import pl.newbies.lecture.infrastructure.repository.toLecture
import pl.newbies.plugins.inject
import pl.newbies.user.application.UserConverter
import pl.newbies.user.application.model.UserResponse
import pl.newbies.user.domain.UserNotFoundException
import pl.newbies.user.infrastructure.repository.UserDAO
import pl.newbies.user.infrastructure.repository.toUser

fun SchemaBuilder.lectureSchema() {
    val lectureConverter: LectureConverter by inject()
    val lectureService: LectureService by inject()
    val userConverter: UserConverter by inject()

    query("lectures") {
        resolver { page: Int?, size: Int? ->
            val pagination = (page to size).pagination()

            transaction {
                LectureDAO.all()
                    .orderBy(Lectures.createDate to SortOrder.ASC)
                    .limit(pagination.limit, pagination.offset)
                    .map { it.toLecture() }
            }.map { lectureConverter.convert(it) }
        }
    }

    query("lecture") {
        resolver { id: String ->
            transaction {
                LectureDAO.findById(id)?.toLecture()
            }?.let {
                lectureConverter.convert(it)
            }
        }
    }

    mutation("createLecture") {
        resolver { request: LectureRequest, context: Context ->
            val principal = context.principal()

            val lecture = lectureService.createLecture(request, principal.userId)

            lectureConverter.convert(lecture)
        }
    }

    mutation("replaceLecture") {
        resolver { id: String, request: LectureRequest, context: Context ->
            val principal = context.principal()
            val lecture = transaction { LectureDAO.findById(id)?.toLecture() }
                ?: throw LectureNotFoundException(id)

            principal.assertLectureWriteAccess(lecture)

            val replacedLecture = lectureService.updateLecture(lecture, request)

            lectureConverter.convert(replacedLecture)
        }
    }

    mutation("deleteLecture") {
        resolver { id: String, context: Context ->
            val principal = context.principal()
            val lecture = transaction { LectureDAO.findById(id)?.toLecture() }
                ?: throw LectureNotFoundException(id)

            principal.assertLectureWriteAccess(lecture)

            lectureService.deleteLecture(lecture)

            true
        }
    }

    mutation("followLecture") {
        resolver { id: String, context: Context ->
            val principal = context.principal()

            val user = transaction { UserDAO.findById(principal.userId)?.toUser() }
                ?: throw UserNotFoundException(principal.userId)
            val lecture = transaction { LectureDAO.findById(id)?.toLecture() }
                ?: throw LectureNotFoundException(id)

            lectureService.followLecture(user, lecture)

            true
        }
    }

    mutation("unfollowLecture") {
        resolver { id: String, context: Context ->
            val principal = context.principal()

            val user = transaction { UserDAO.findById(principal.userId)?.toUser() }
                ?: throw UserNotFoundException(principal.userId)
            val lecture = transaction { LectureDAO.findById(id)?.toLecture() }
                ?: throw LectureNotFoundException(id)

            lectureService.unfollowLecture(user, lecture)

            true
        }
    }

    inputType<LectureRequest>()
    type<LectureResponse> {
        property<UserResponse>(name = "author") {
            resolver {
                val user = transaction { UserDAO[it.authorId].toUser() }

                userConverter.convert(user)
            }
        }

        property<Boolean>(name = "isFollowed") {
            resolver { response: LectureResponse, context: Context ->
                val principal = context.principal()

                val lectureFollow = transaction {
                    LectureDAO.find {
                        (LectureFollows.user eq principal.userId) and (LectureFollows.lecture eq response.id)
                    }.firstOrNull()
                }

                lectureFollow != null
            }
        }
    }
}