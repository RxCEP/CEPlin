package br.ufpe.cin.jonas.ceplin.activity.model

import br.ufpe.cin.jonas.ceplin.Event
import java.util.Date

class ProximityEvent(val dist: Float) : Event {
    override val timestamp = Date()
}

