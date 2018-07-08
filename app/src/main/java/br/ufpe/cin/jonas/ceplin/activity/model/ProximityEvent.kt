package br.ufpe.cin.jonas.ceplin.util

import br.ufpe.cin.jonas.ceplin.Event
import java.util.*


class ProximityEvent(val dist: Float) : Event {
    override val timeStamp = Date()
}

