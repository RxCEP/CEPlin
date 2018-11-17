package br.ufpe.cin.ceplin.cepstock.simulator

import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue

class Monitor {

    private var entries: ConcurrentLinkedQueue<MonitorEntry> = ConcurrentLinkedQueue()

    fun append(entry: MonitorEntry) {
        entries.add(entry)
    }

    fun generateReport() {
        val dataset = entries.sortedBy { it.date }
        val epeDataset = entries.filter { it.type == "Equity Price Event" }
        val ereDataset = entries.filter { it.type == "Equity Return Event" }
        val aruDataset = entries.filter { it.type == "Average Return Update" }
        val rvuDataset = entries.filter { it.type == "Return Variance Update" }

        val ttime = test(epeDataset.drop(1), rvuDataset)

        println("Total Events: ${dataset.size}")
        println("EPE: ${epeDataset.size}")
        println("ERE: ${ereDataset.size}")
        println("ARU: ${aruDataset.size}")
        println("RVU: ${rvuDataset.size}")
        println("Execution Time: ${dataset.last().date.time - dataset.first().date.time}")
        println("Average turnaround time: $ttime")
    }

    private fun test(ereDataset: List<MonitorEntry>, aruDataset: List<MonitorEntry>) : Double {
        val result = ereDataset.zip(aruDataset, { a, b ->
            b.date.time - a.date.time
        })

        return result.average()
    }

    data class MonitorEntry(val type: String, val symbol: String, val data: Number) {
        val date = Date()
    }
}