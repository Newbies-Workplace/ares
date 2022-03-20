package pl.newbies.common

import io.ktor.http.content.PartData

val PartData.FileItem.extension: String
    get() = originalFileName?.substringAfterLast('.')
        ?: throw FileTypeNotSupportedException("null", listOf())