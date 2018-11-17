package br.ufpe.cin.jonas.ceplin.activity.model

import br.ufpe.cin.jonas.ceplin.Event
import java.util.Date

class AccelerationEvent(val x: Float, val y: Float, val z: Float) : Event {
    override val timestamp = Date()
}

