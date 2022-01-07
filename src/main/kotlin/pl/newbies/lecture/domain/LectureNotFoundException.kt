package pl.newbies.lecture.domain

import pl.newbies.common.NotFoundException

class LectureNotFoundException(
    id: String,
) : NotFoundException(id = id, entityName = "Lecture")