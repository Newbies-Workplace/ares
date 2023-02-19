package pl.newbies.lecture.domain

class TooManyLectureInvitesException(
    lectureId: String,
) : RuntimeException("Lecture speaker invitation limit exceeded for lecture $lectureId")