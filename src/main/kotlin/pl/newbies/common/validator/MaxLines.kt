package pl.newbies.common.validator

import org.valiktor.Constraint
import org.valiktor.Validator

class MaxLines : Constraint

fun <E> Validator<E>.Property<String?>.maxLines(max: Int) =
    this.validate(MaxLines()) {
        it == null || it.lines().size <= max
    }
