package pl.newbies.common.validator

import org.valiktor.Constraint
import org.valiktor.Validator

class HexColor : Constraint

fun <E> Validator<E>.Property<String?>.isHexColor() =
    this.validate(HexColor()) {
        it == null || it.matches(Regex("^#([a-fA-F0-9]{6}|[a-fA-F0-9]{3})\$"))
    }