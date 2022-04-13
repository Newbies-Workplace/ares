package pl.newbies.common.validator

import org.valiktor.Constraint
import org.valiktor.Validator

class OneLine : Constraint

fun <E> Validator<E>.Property<String?>.oneLine() =
    this.validate(OneLine()) {
        it == null || it.lines().size == 1
    }
