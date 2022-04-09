package pl.newbies.common

import com.aventrix.jnanoid.jnanoid.NanoIdUtils

private const val DEFAULT_ID_SIZE = 10
private val DEFAULT_ID_ALPHABET = (('a'..'z') + ('A'..'Z') + ('0'..'9')).toCharArray()

fun nanoId(): String =
    NanoIdUtils.randomNanoId(
        NanoIdUtils.DEFAULT_NUMBER_GENERATOR,
        DEFAULT_ID_ALPHABET,
        DEFAULT_ID_SIZE
    )