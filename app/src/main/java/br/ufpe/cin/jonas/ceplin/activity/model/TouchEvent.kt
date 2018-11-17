package br.ufpe.cin.jonas.ceplin.activity.model

import br.ufpe.cin.jonas.ceplin.Event
import java.util.Date

class TouchEvent(val x: Float, val y: Float) : Event {
    override val timestamp = Date()
}

