package pl.newbies.lecture.application.model

import kotlinx.serialization.Serializable
import org.valiktor.validate
import pl.newbies.common.validator.isHexColor

@Serializable
data class LectureThemeRequest(
    val primaryColor: String?,
    val secondaryColor: String?,
) {

    init {
        validate(this) {
            validate(LectureThemeRequest::primaryColor).isHexColor()
            validate(LectureThemeRequest::secondaryColor).isHexColor()
        }
    }
}