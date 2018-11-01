package br.ufpe.cin.jonas.ceplin.util

import br.ufpe.cin.jonas.ceplin.Event
import java.util.*


class LightEvent(val lx: Float) : Event {
    override val timestamp = Date()
}

