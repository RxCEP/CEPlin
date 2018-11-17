package br.ufpe.cin.ceplin.cepstock.event

import br.ufpe.cin.jonas.ceplin.Event
import br.ufpe.cin.jonas.ceplin.util.NumericEvent
import java.util.Date

class EquityPriceEvent(val symbol: String,
                       timestamp: Date,
                       close: Number) : NumericEvent<Number>(close, timestamp)

class EquityReturnEvent(val symbol: String, timestamp: Date, returnRatio: Double) : NumericEvent<Number>(returnRatio, timestamp)
class EquitySimulationCompletedEvent(val symbol: String, override val timestamp: Date) : Event