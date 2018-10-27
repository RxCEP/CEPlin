package br.ufpe.cin.jonas.ceplin

import br.ufpe.cin.jonas.ceplin.util.IntEvent
import io.reactivex.Observable
import java.util.*
import java.util.concurrent.TimeUnit

class EventStream<T>(val observable: Observable<T>) {

    fun subscribe(onNext: ((T) -> Unit)) {
        this.observable.subscribe(onNext)
    }

    /**
     * The filter operator emits only events from an
     * EventStream that satisfies a predicate function.
     */
    fun filter(predicate: (T) -> Boolean): EventStream<T> {
        return EventStream(this.observable.filter(predicate))
    }

    /**
     * The map operator transforms an EventStream by creating
     * a new EventStream through a projection function.
     */
    fun <R> map(transform: ((T) -> R)): EventStream<R> {
        return EventStream<R>(this.observable.map(transform))
    }

    /**
     * The sequence operator emits only events that follows
     * a specified order within a set of events. The operator
     * takes a predicate function as the sequence condition
     * and the length of the sequence to be considered.
     */
    fun sequence(predicate: (T, T) -> Boolean, count: Int, skip: Int = count): EventStream<List<T>> {
        val sequenceEquals = this.observable
                .buffer(count, skip)
                .filter {
                    var filter = true
                    if (count > 1) {
                        for (i in 1..(it.size - 1)) {
                            if (!predicate(it[i - 1], it[i])) {
                                filter = false
                                break
                            }
                        }
                    }
                    filter
                }
        return EventStream(sequenceEquals)
    }

    /**
     * The merge operator merges two EventStreams and notifies
     * the subscriber through a ComplexEvent object when
     * both EventStreams happen within a given time frame.
     */
    fun <R> merge(stream: EventStream<R>): ComplexEvent {
        val merged = Observable.merge(
                this.observable.map { element -> Pair(element, 1) },
                stream.observable.map { element -> Pair(element, 2) }
        )
        return ComplexEvent(observable = merged, numberOfEvents = 2)
    }

    fun buffer(timespan: Long, timeUnit: TimeUnit): EventStream<List<T>> {
        return EventStream(this.observable.buffer(timespan, timeUnit))
    }

    /**
     * The window operator only emits events that
     * happened within a given time frame.
     */
    fun window(timespan: Long, timeUnit: TimeUnit): EventStream<T> {
        return EventStream(this.observable.buffer(timespan, timeUnit).flatMap{Observable.fromIterable(it)})
    }

    /**
     * The union operator merges two EventStreams into one EventStream
     * that emits events from both streams as they arrive.
     */
    fun union(stream: EventStream<T>): EventStream<T> {
        val merged = Observable.merge(
                this.observable,
                stream.observable).distinct()
        return EventStream<T>(merged)
    }

    fun <R> distinct(transform: ((T) -> R)): EventStream<R> {
        return EventStream<R>(this.observable.map(transform))
    }

    fun accumulator(): EventStream<MutableList<T>> {
        val accumulator = this.observable.scan(mutableListOf<T>(),
                { accumulated, item ->
                    accumulated.add(item)
                    accumulated
                }
        )
        return EventStream(accumulator)
    }
}

/***** Extension functions *****/

fun <T : Comparable<T>> EventStream<T>.max(): EventStream<T?> {
    return mapAccumulator(this, { it.max() })
}

fun <T : Comparable<T>> EventStream<T>.min(): EventStream<T?> {
    return mapAccumulator(this, { it.min() })
}

fun <T : Comparable<T>> EventStream<T>.sumBy(selector: (T) -> Int): EventStream<IntEvent?> {
    return this.mapAccumulator(this, { IntEvent(it.sumBy(selector)) })
}

fun EventStream<IntEvent>.sum(): EventStream<Int> {
    val sum = this.observable
            .scan(0,
                    { accumulated, item ->
                        accumulated + item.value
                    })

    return EventStream(sum)
}

fun EventStream<IntEvent>.count(): Observable<Int> {
    val count = this.observable
            .scan(0,
                    { accumulated, _ ->
                        accumulated + 1
                    })

    return count
}

fun EventStream<IntEvent>.average(timeSpan: Long, date: Date) : EventStream<Int> {
    val avg = this.observable
            .filter { e: IntEvent -> e.timeStamp.time - date.time < timeSpan }
            .scan(Pair(0, 0),
                    { acc, v ->
                        // first: sum, second: count
                        Pair(acc.first + v.value, acc.second + 1)
                    })
            // prevent division by 0
            .filter { acc -> acc.second > 0}
            .map { acc -> acc.first/acc.second }

    return EventStream(avg)
}

fun EventStream<IntEvent>.probability(value: Int) : EventStream<Double> {
    val prob = this.observable
            .scan(mutableListOf<IntEvent>(),
                    { acc, ev ->
                        acc.add(ev)
                        acc
                    })
            .filter { list -> list.size > 0}
            .map { list -> prob(list, list.last().value) }

    return EventStream(prob)
}

fun EventStream<IntEvent>.expected() : EventStream<Double> {
    val exp = this.observable
            .scan(mutableListOf<IntEvent>(),
                    { acc, ev ->
                        acc.add(ev)
                        acc
                    })
            .filter { list -> list.size > 0}
            .map { list -> computeExpectedValue(list) }

    return EventStream(exp)
}

/**
 * Given a list of events, calculates the probability of a given event outcome.
 */
private fun prob(list: MutableList<IntEvent>, outcome: Int): Double {
    val occ = list.count { it.value == outcome }
    return occ/list.size.toDouble()
}

/**
 * Given a list of events, computes the expected value of an event.
 */
private fun computeExpectedValue(list: MutableList<IntEvent>) : Double {
    val probabilityList = mutableListOf<Double>()
    val outcomes = list.distinct()

    outcomes.mapTo(probabilityList) { it.value * prob(list, it.value) }

    return probabilityList.sum()
}

private fun <T, R> EventStream<T>.mapAccumulator(eventStream: EventStream<T>, function: (List<T>) -> R?): EventStream<R?> {
    val min = eventStream.accumulator().observable
            .filter { it.size > 0 }
            .map { function(it) }
    return EventStream(min)
}