package br.ufpe.cin.ceplin.cepstock.simulator

import android.content.Context
import br.ufpe.cin.ceplin.cepstock.event.EquityPriceEvent
import br.ufpe.cin.ceplin.cepstock.event.EquityReturnEvent
import br.ufpe.cin.ceplin.cepstock.event.EquitySimulationCompletedEvent
import br.ufpe.cin.ceplin.cepstock.simulator.Monitor.MonitorEntry
import br.ufpe.cin.jonas.ceplin.EventManager
import br.ufpe.cin.jonas.ceplin.average
import br.ufpe.cin.jonas.ceplin.variance
import java.util.concurrent.ConcurrentHashMap

class TimeSeriesSimulator(private val options: SimulatorOptions) {

    private val priceManager = EventManager<EquityPriceEvent>()
    private val returnsManager = EventManager<EquityReturnEvent>()
    private val completedManager = EventManager<EquitySimulationCompletedEvent>()
    private val monitor = Monitor()

    // Iterative approach variables
    private val returnEvents = ConcurrentHashMap<String, MutableList<Double>>()

    fun startSimulation(context: Context) {
        val equitySymbols = options.equitySymbols

        addMonitorSubscriptions()

        // Iterative version - average
//        returnsManager.asStream()
//                .subscribe {
//                    val eventList = returnEvents.getOrElse(it.symbol, { mutableListOf() })
//                    eventList.add(it.value.toDouble())
//                    returnEvents.put(it.symbol, eventList)
//
//                    val sum = eventList.sum()
//                    val avg = sum/eventList.size.toDouble()
//
//                    monitor.append(MonitorEntry("Average Return Update", it.symbol, avg))
//                }

        // Iterative version - variance
//        returnsManager.asStream()
//                .subscribe {
//                    val eventList = returnEvents.getOrElse(it.symbol, { mutableListOf() })
//                    eventList.add(it.value.toDouble())
//                    returnEvents.put(it.symbol, eventList)
//
//                    val n = eventList.size
//                    var sum = 0.0
//                    var sumSq = 0.0
//
//                    for (ev in eventList) {
//                        sum += ev
//                        sumSq += ev * ev
//                    }
//
//                    val variance = (sumSq - (sum * sum)/n)/n
//
//                    monitor.append(MonitorEntry("Return Variance Update", it.symbol, variance))
//                }

        if (equitySymbols != null) {
            for ((i, symbol) in equitySymbols.withIndex()) {
                // Start the event emitter for each symbol
                val sim = EquitySimulator(symbol, options)
                sim.priceManager = priceManager
                sim.completedManager = completedManager
                sim.init(context)

                val symbolString = sim.equityData.symbol

                // Generate average stock returns in simulation
                returnsManager.asStream()
                        .filter { it.symbol == symbolString }
                        .average()
                        .subscribe {
                            println("value: ${it.value }")
                            monitor.append(MonitorEntry("Average Return Update", symbolString, it.value))
                        }

                // Generate variance of stock returns in simulation
//                returnsManager.asStream()
//                        .filter { it.symbol == symbolString }
//                        .variance()
//                        .subscribe {
//                            monitor.append(MonitorEntry("Return Variance Update", symbolString, it.value))
//                        }

                sim.start(i)
            }
        }
    }

    private fun addMonitorSubscriptions() {
        var simCompleted = 0

        priceManager.asStream()
                .subscribe {
                    monitor.append(MonitorEntry("Equity Price Event", it.symbol, it.value))

                    // Generate stock returns in simulation
                    emitReturn(it)
                }

        returnsManager.asStream()
                .subscribe {
                    monitor.append(MonitorEntry("Equity Return Event", it.symbol, it.value))
                }

        completedManager.asStream()
                .subscribe {
                    monitor.append(MonitorEntry("Equity Simulation Completed Event", it.symbol, ++simCompleted))
                    if (simCompleted == options.equitySymbols?.size) {
                        monitor.generateReport()
                    }
                }
    }

    private var previousPrices: MutableMap<String, Double> = HashMap()
    private fun emitReturn(it: EquityPriceEvent) {
        val symbol = it.symbol
        val currentPrice = it.value.toDouble()
        val previousPrice = previousPrices[symbol]

        if (previousPrice != null) {
            val returnRatio = (currentPrice/previousPrice) - 1
            returnsManager.addEvent(EquityReturnEvent(symbol, it.timestamp, returnRatio))
        }

        previousPrices.put(symbol, currentPrice)
    }
}