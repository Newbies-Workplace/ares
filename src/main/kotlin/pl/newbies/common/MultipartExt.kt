package pl.newbies.common

import io.ktor.http.content.PartData

val PartData.FileItem.extension: String
    get() = originalFileName?.substringAfterLast('.')
        ?: throw FileTypeNotSupportedException("null", listOf())

val PartData.FileItem.nameWithExtension: String
    get() = originalFileName
        ?: throw FileTypeNotSupportedException("null", listOf())