package pl.newbies.common

class FileTypeNotSupportedException(
    extension: String,
    allowedExtensions: List<String>
) : RuntimeException("File type ($extension) is not supported, allowed extensions ($allowedExtensions)")