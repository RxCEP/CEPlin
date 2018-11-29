package br.ufpe.cin.ceplin.cepstock.simulator

import android.content.Context
import br.ufpe.cin.ceplin.cepstock.event.EquityPriceEvent
import br.ufpe.cin.ceplin.cepstock.event.EquitySimulationCompletedEvent
import br.ufpe.cin.ceplin.cepstock.model.TimeSeries
import br.ufpe.cin.ceplin.cepstock.model.TimeSeriesPoint
import br.ufpe.cin.ceplin.cepstock.util.FileUtils
import br.ufpe.cin.jonas.ceplin.EventManager
import com.google.gson.GsonBuilder
import io.reactivex.subjects.PublishSubject
import java.util.Date
import java.util.Timer
import kotlin.concurrent.timerTask

class EquitySimulator(private val resId: Int,
                      private val options: SimulatorOptions) {

    lateinit var equityData: TimeSeries
    var priceManager: EventManager<EquityPriceEvent>? = null
    var completedManager: EventManager<EquitySimulationCompletedEvent>? = null

    var publishSubject: PublishSubject<EquityPriceEvent>? = null

    private lateinit var timeSeries: List<TimeSeriesPoint>

    fun init(context: Context) {
        val startDate = options.startDate
        val endDate = options.endDate

        val jsonString = FileUtils.readRawResourceToJsonString(context, resId)
        val gson = GsonBuilder()
                .setDateFormat("yyyy-MM-dd")
                .create()

        equityData = gson.fromJson<TimeSeries>(jsonString, TimeSeries::class.java)
        timeSeries = equityData.timeSeries.filter { it.dateTime >= startDate && it.dateTime <= endDate }
    }

    fun start(i: Int, monitor: Monitor) {
        val symbol = equityData.symbol
        var n = 0
        val delay = options.delay * i
        val period = options.delay * options.equitySymbols!!.size

        val timer = Timer()
        val days = timeSeries.size
        val it = timeSeries.iterator()

        timer.schedule(timerTask {
            if (it.hasNext()) {
                val value = it.next()

                val ev = EquityPriceEvent(symbol,
                                          value.dateTime,
                                          value.open,
                                          value.high,
                                          value.low,
                                          value.close)

                priceManager?.addEvent(ev)
                publishSubject?.onNext(ev)

                monitor.append(Monitor.MonitorEntry(value.dateTime.time, "Equity Price Event", symbol))
                if (++n >= days) {
                    completedManager?.addEvent(EquitySimulationCompletedEvent(symbol, Date()))
                    timer.cancel()
                }
            }
        }, delay.toLong(), period.toLong())
    }
}