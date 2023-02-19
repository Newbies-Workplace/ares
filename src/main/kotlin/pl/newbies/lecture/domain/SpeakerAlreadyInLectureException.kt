package pl.newbies.lecture.domain

class SpeakerAlreadyInLectureException(
    val userId: String,
    val lectureId: String,
) : RuntimeException("User $userId is already speaker in lecture $lectureId")