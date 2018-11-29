package br.ufpe.cin.ceplin.cepstock.simulator

import android.content.Context
import br.ufpe.cin.ceplin.cepstock.event.EquityPriceEvent
import br.ufpe.cin.ceplin.cepstock.event.EquitySimulationCompletedEvent
import br.ufpe.cin.ceplin.cepstock.simulator.Monitor.MonitorEntry
import br.ufpe.cin.jonas.ceplin.EventManager
import br.ufpe.cin.jonas.ceplin.averageBuffer
import br.ufpe.cin.jonas.ceplin.util.NumericEvent
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

class TimeSeriesSimulator(private val options: SimulatorOptions) {

    private val priceManager = EventManager<EquityPriceEvent>()
    private val completedManager = EventManager<EquitySimulationCompletedEvent>()
    private val monitor = Monitor()

    // Iterative approach variables
    private val priceEvents = PublishSubject.create<EquityPriceEvent>()

    fun startSimulation(context: Context) {
        addMonitorSubscriptions()

        val equitySymbols = options.equitySymbols
        if (equitySymbols != null) {
            for ((i, symbol) in equitySymbols.withIndex()) {
                // Start the event emitter for each symbol
                val sim = EquitySimulator(symbol, options)
                sim.priceManager = priceManager
                sim.publishSubject = priceEvents
                sim.completedManager = completedManager
                sim.init(context)

                val symbolString = sim.equityData.symbol

                createUpTrendBearishRule(symbolString)
                createDownTrendBullishRule(symbolString)

//                createUpTrendBearishRuleRxOnly(symbolString)
//                createDownTrendBullishRuleRxOnly(symbolString)

                sim.start(i, monitor)
            }
        }
    }

    private fun addMonitorSubscriptions() {
        var simCompleted = 0

        completedManager.asStream()
                .subscribe {
                    monitor.append(MonitorEntry(it.timestamp.time,
                            "Equity Simulation Completed Event",
                            it.symbol))

                    if (++simCompleted == options.equitySymbols?.size) {
                        monitor.generateReport()
                    }
                }
    }

    private fun createUpTrendBearishRule(symbol: String) {
        val engulfingBearishRule = priceManager.asStream()
                .filter { it.symbol == symbol }
                .sequence({ a, b ->
                    val result = a.close > a.open && b.open >= a.close && b.close < a.open

                    if (result) {
                        monitor.append(MonitorEntry(b.timestamp.time,
                                "Engulfing Bearish Event",
                                symbol))
                    }

                    result
                }, 2, 1)

        val upTrendRule =
                priceManager.asStream()
                        .filter { it.symbol == symbol }
                        .buffer(20, 1)
                        .averageBuffer()
                        .sequence({ a, b ->
                            val result = b.value > a.value

                            if (result) {
                                monitor.append(MonitorEntry(b.timestamp.time,
                                        "Up Trend Event",
                                        symbol))
                            }

                            result
                        }, 2, 1)

        engulfingBearishRule.merge(upTrendRule).subscribe {
            monitor.append(MonitorEntry(0, "Up Trend Bearish Update", symbol))
        }
    }

    private fun createDownTrendBullishRule(symbol: String) {
        val engulfingBullishRule = priceManager.asStream()
                .filter { it.symbol == symbol }
                .sequence({ a, b ->
                    val result = a.close < a.open && b.open <= a.close && b.close > a.open

                    if (result) {
                        monitor.append(MonitorEntry(b.timestamp.time,
                                "Engulfing Bullish Event",
                                symbol))
                    }

                    result
                }, 2, 1)

        val downTrendRule =
                priceManager.asStream()
                        .filter { it.symbol == symbol }
                        .buffer(20, 1)
                        .averageBuffer()
                        .sequence({ a, b ->
                            val result = b.value < a.value

                            if (result) {
                                monitor.append(MonitorEntry(b.timestamp.time,
                                        "Down Trend Event",
                                        symbol))
                            }

                            result
                        }, 2, 1)

        engulfingBullishRule.merge(downTrendRule).subscribe {
            monitor.append(MonitorEntry(0, "Down Trend Bullish Update", symbol))
        }
    }

    private fun createUpTrendBearishRuleRxOnly(symbol: String) {
        val engulfingBearishRule = getSequenceObservable(priceEvents.filter { it.symbol == symbol },
                { a, b ->
                    val result = a.close > a.open && b.open >= a.close && b.close < a.open

                    if (result) {
                        monitor.append(MonitorEntry(b.timestamp.time,
                                "Engulfing Bearish Event",
                                symbol))
                    }

                    result
                }, 2, 1)

        val averagePriceEvents = this.priceEvents
                .filter { it.symbol == symbol }
                .buffer(20, 1)
                .map { NumericEvent(it.sumByDouble { it.value }/it.size.toDouble(), it.last().timestamp) }

        val upTrendRule = getSequenceObservable(averagePriceEvents,
                { a, b ->
                    val result = b.value > a.value

                    if (result) {
                        monitor.append(MonitorEntry(b.timestamp.time,
                                "Up Trend Event",
                                symbol))
                    }

                    result
                }, 2, 1
        )

        val mergedRules = Observable.merge(engulfingBearishRule, upTrendRule)
        mergedRules.buffer( 100, TimeUnit.MILLISECONDS, 2)
                .subscribe { bundle ->
                    if (bundle.size == 2) {
                        monitor.append(MonitorEntry(0, "Up Trend Bearish Update", symbol))
                    }
                }
    }

    private fun createDownTrendBullishRuleRxOnly(symbol: String) {
        val engulfingBullishRule = getSequenceObservable(priceEvents.filter { it.symbol == symbol },
                { a, b ->
                    val result = a.close < a.open && b.open <= a.close && b.close > a.open

                    if (result) {
                        monitor.append(MonitorEntry(b.timestamp.time,
                                "Engulfing Bullish Event",
                                symbol))
                    }

                    result
                }, 2, 1)

        val averagePriceEvents = this.priceEvents
                .filter { it.symbol == symbol }
                .buffer(20, 1)
                .map { NumericEvent(it.sumByDouble { it.value }/it.size.toDouble(), it.last().timestamp) }

        val downTrendRule = getSequenceObservable(averagePriceEvents,
                { a, b ->
                    val result = b.value < a.value

                    if (result) {
                        monitor.append(MonitorEntry(b.timestamp.time,
                                "Down Trend Event",
                                symbol))
                    }

                    result
                }, 2, 1
        )

        val mergedRules = Observable.merge(engulfingBullishRule, downTrendRule)
        mergedRules.buffer( 100, TimeUnit.MILLISECONDS, 2)
                .subscribe { bundle ->
                    if (bundle.size == 2) {
                        monitor.append(MonitorEntry(0, "Down Trend Bullish Update", symbol))
                    }
                }
    }

    private fun <T> getSequenceObservable(subject: Observable<T>, predicate: (T, T) -> Boolean, count: Int = 5, skip: Int = count): Observable<MutableList<T>>? {
        return subject.buffer(count, skip)
                .filter {
                    var filter = true
                    if (it.isNotEmpty() && count > 1) {
                        for (i in 1..(it.size - 1)) {
                            if (!predicate(it[i - 1], it[i])) {
                                filter = false
                                break
                            }
                        }
                    }
                    filter
                }
    }
}