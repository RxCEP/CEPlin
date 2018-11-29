package br.ufpe.cin.ceplin.cepstock.simulator

import java.util.*

class Monitor {

    private var entries: MutableMap<String, MutableMap<String, MutableList<MonitorEntry>>> = mutableMapOf()

    fun append(entry: MonitorEntry) {
        val symbol = entry.symbol
        val type = entry.type

        val types = entries.getOrDefault(symbol, mutableMapOf())
        val list = types.getOrDefault(type, mutableListOf())

        list.add(entry)
        types.put(type, list)
        entries.put(symbol, types)
    }

    fun generateReport() {
        val eventsDataset = mutableMapOf<String, MutableList<MonitorEntry>>()

        for (symbols in entries.values) {
            for (type in symbols.keys) {
                val ds = eventsDataset.getOrDefault(type, mutableListOf())

                symbols[type]?.let { ds.addAll(it) }
                eventsDataset.put(type, ds)
            }
        }

        println("Total Events: ${eventsDataset.values.sumBy { it.size }}")
        for (key in eventsDataset.keys) {
            println("[$key]: ${eventsDataset[key]?.size}")
        }

        val sorted = eventsDataset.flatMap { it.component2() }.sortedBy { it.date }
        println("Execution Time: ${sorted.last().date.time - sorted.first().date.time}")

        val average = computeResponseTime(Arrays.asList(Arrays.asList("Down", "Bullish"),
                                                        Arrays.asList("Up", "Bearish")))

        println("Average response time: $average")
    }

    private fun computeResponseTime(arr: List<List<String>>): Double {
        val responseTimes = mutableListOf<Long>()

        for (v in arr) {
            val trend = v[0]
            val condition = v[1]

            for (symbol in entries.keys) {
                val types = entries[symbol]
                val priceEvents = types?.get("Equity Price Event")
                val trendEvents = types?.get("$trend Trend Event")
                val engulfingEvents = types?.get("Engulfing $condition Event")
                val complexEvents = types?.get("$trend Trend $condition Update")

                if (complexEvents != null) {
                    val complexEventsIterator = complexEvents.iterator()
                    for (ev in engulfingEvents!!) {
                        if (trendEvents!!.find { it.id == ev.id } != null) {
                            val first = priceEvents!!.filter { it.id <= ev.id }.takeLast(20).first()
                            val last = complexEventsIterator.next()

                            responseTimes.add(last.date.time - first.date.time)
                        }
                    }
                }
            }
        }

        return responseTimes.sum()/responseTimes.size.toDouble()
    }

    data class MonitorEntry(val id: Long,
                            val type: String,
                            val symbol: String) {
        val date = Date()
    }
}