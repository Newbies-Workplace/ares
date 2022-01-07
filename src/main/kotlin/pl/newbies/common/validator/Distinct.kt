package pl.newbies.common.validator

import org.valiktor.Constraint
import org.valiktor.Validator

class Distinct : Constraint

fun <E, T> Validator<E>.Property<Iterable<T>?>.distinct() =
    this.validate(Distinct()) {
        it == null || it.toList().size == it.distinct().size
    }
