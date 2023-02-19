package pl.newbies.lecture.domain

import pl.newbies.common.NotFoundException

class LectureInviteNotFoundException(
    id: String,
) : NotFoundException(id = id, entityName = "LectureInvite")