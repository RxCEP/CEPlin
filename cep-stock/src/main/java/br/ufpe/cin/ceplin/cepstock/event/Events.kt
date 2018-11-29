package br.ufpe.cin.ceplin.cepstock.event

import br.ufpe.cin.jonas.ceplin.Event
import br.ufpe.cin.jonas.ceplin.util.NumericEvent
import java.util.Date

class EquityPriceEvent(val symbol: String,
                       timestamp: Date,
                       val open: Double,
                       val high: Double,
                       val low: Double,
                       val close: Double) : NumericEvent<Double>(close, timestamp)

class EquitySimulationCompletedEvent(val symbol: String, override val timestamp: Date) : Event