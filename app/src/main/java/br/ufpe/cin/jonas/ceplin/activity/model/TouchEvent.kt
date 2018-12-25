package br.ufpe.cin.jonas.ceplin.util

import br.ufpe.cin.jonas.ceplin.Event
import java.util.*


class TouchEvent(val x: Float, val y: Float) : Event {
    override val timestamp = Date()
}

