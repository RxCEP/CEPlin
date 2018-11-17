package br.ufpe.cin.jonas.ceplin.util

import br.ufpe.cin.jonas.ceplin.Event
import java.util.*

open class NumericEvent<out T : Number> : Event {

    val value: T
    final override val timestamp: Date

    constructor(value: T) : this(value, Date())

    constructor(value: T, timestamp: Date) {
        this.value = value
        this.timestamp = timestamp
    }

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