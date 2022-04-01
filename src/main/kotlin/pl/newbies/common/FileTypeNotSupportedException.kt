package pl.newbies.common

class FileTypeNotSupportedException : RuntimeException {
    constructor() : super("File type is not supported. (unknown extension)")
    constructor(
        extension: String,
        allowedExtensions: List<String>
    ) : super("File type ($extension) is not supported, allowed extensions ($allowedExtensions)")
}