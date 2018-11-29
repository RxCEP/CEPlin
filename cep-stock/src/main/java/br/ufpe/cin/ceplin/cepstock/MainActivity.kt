package br.ufpe.cin.ceplin.cepstock

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import br.ufpe.cin.ceplin.cepstock.simulator.SimulatorOptions
import br.ufpe.cin.ceplin.cepstock.simulator.TimeSeriesSimulator
import java.text.SimpleDateFormat

class MainActivity : Activity() {

    private val symbols = arrayOf(
            R.raw.time_series_aapl,
            R.raw.time_series_amzn,
            R.raw.time_series_fb,
            R.raw.time_series_goog,
            R.raw.time_series_msft
    )

    @SuppressLint("SimpleDateFormat")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val options = SimulatorOptions()
        options.delay = 100
        options.equitySymbols = symbols
        options.startDate = SimpleDateFormat("yyyy-MM-dd").parse("2015-01-01")
        options.endDate = SimpleDateFormat("yyyy-MM-dd").parse("2017-12-31")

        val sim = TimeSeriesSimulator(options)
        sim.startSimulation(this)
    }
}