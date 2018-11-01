package br.ufpe.cin.jonas.ceplin.util

import br.ufpe.cin.jonas.ceplin.Event
import java.util.*

class NumericEvent<out T : Number>(val value: T) : Event {

    override val timestamp = Date()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NumericEvent<*>

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return value.toString()
    }
}