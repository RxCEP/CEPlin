package br.ufpe.cin.jonas.ceplin

import br.ufpe.cin.jonas.ceplin.util.NumericEvent
import io.reactivex.Observable
import io.reactivex.rxkotlin.withLatestFrom
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

    fun buffer(count: Int, skip: Int) : EventStream<List<T>> {
        return EventStream(this.observable.buffer(count, skip))
    }

    /**
     * The window operator only emits events that
     * happened within a given time frame.
     */
    fun window(timespan: Long, timeUnit: TimeUnit): EventStream<T> {
        return EventStream(this.observable.buffer(timespan, timeUnit).flatMap { Observable.fromIterable(it) })
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

    /**
     * Equivalent to Cugola Except operator
     *
     * <p>
     *     Filters the accumulated from all EventStreams that exists in current stream
     *     but not in given one
     * </p>
     */
    fun not(stream: EventStream<T>): EventStream<T> {
        val streamAccumulated = EventStream<MutableList<T>>(stream.accumulator().observable.startWith(ArrayList<T>()))
        val filtered = this.observable.withLatestFrom(streamAccumulated.observable).filter { (event, accumulated) ->
            !accumulated.contains(event)
        }.map {
            it.first
        }
        return EventStream<T>(filtered)
    }

    /**
     * Equivalent to Cugola Remove-duplicates operator
     *
     * <p>
     *     This is a simple wrap of RxJava distinct
     * </p>
     */
    fun distinct(): EventStream<T> {
        return EventStream<T>(this.observable.distinct())
    }

    /**
     * Equivalent to Cugola Intersect operator
     *
     * <p>
     *     Filters the accumulated from all EventStreams that exists in current stream
     *     and in given one
     * </p>
     */
    fun intersect(stream: EventStream<T>): EventStream<T> {
        val streamAccumulated = EventStream<MutableList<T>>(stream.accumulator().observable.startWith(ArrayList<T>()))
        val filtered = this.observable.withLatestFrom(streamAccumulated.observable).filter { (event, accumulated) ->
            accumulated.contains(event)
        }.map {
            it.first
        }.distinct()
        return EventStream<T>(filtered)
    }

    /**
     * Equivalent to Cugola Order by operator
     *
     * <p>
     *     Compare events by given comparator and return a stream with all of then ordered by accordingly
     * </p>
     */
    fun <R : Comparable<R>> orderBy(comparison: ((T) -> R)): EventStream<List<T>> {
        val ordered = this.accumulator().map {
            it.sortedBy(comparison)
        }.observable
        return EventStream(ordered)
    }

    /**
     * Equivalent to Cugola Group by operator
     *
     * <p>
     *     Compare events by given comparator and return a stream with all of then grouped by accordingly
     * </p>
     */
    fun <R> groupBy(comparison: ((T) -> R)): EventStream<Map<R, List<T>>> {
        val grouped = this.accumulator().map {
            it.groupBy(comparison)
        }.observable
        return EventStream(grouped)
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

fun <T : Comparable<T>> EventStream<T>.sumBy(selector: (T) -> Int): EventStream<NumericEvent<Number>?> {
    return this.mapAccumulator(this, { NumericEvent(it.sumBy(selector)) })
}

fun <T : Number>EventStream<out NumericEvent<T>>.sum(): EventStream<NumericEvent<Double>> {
    val sum = this.observable
            .scan(0.0,
                    { accumulated, item ->
                        accumulated + item.value.toDouble()
                    })
            .map { NumericEvent(it) }

    return EventStream(sum)
}

fun <T> EventStream<T>.count(): EventStream<NumericEvent<Int>> {
    val count: Observable<NumericEvent<Int>> = this.observable
            .scan(0,
                    { accumulated, _ ->
                        accumulated + 1
                    })
            .map { NumericEvent(it) }

    return EventStream(count)
}

fun <T : Number>EventStream<out NumericEvent<T>>.average() : EventStream<NumericEvent<Double>> {
    val avg = this.observable
            .scan(Pair(0.0, 0),
                    { acc, v ->
                        // first: sum, second: count
                        Pair(acc.first + v.value.toDouble(), acc.second + 1)
                    })
            // prevent division by 0
            .filter { pair -> pair.second > 0}
            .map { pair -> NumericEvent(pair.first/pair.second) }

    return EventStream(avg)
}

// Alternate implementation
//fun <T : Number>EventStream<out NumericEvent<T>>.average()
//        : EventStream<NumericEvent<Double>> {
//    val sumOb = this.sum().observable
//    val countOb = this.count().observable
//    val avg = Observable.zip(
//            sumOb,
//            countOb,
//            BiFunction { sum: NumericEvent<Double>, count: NumericEvent<Int> ->
//                NumericEvent(sum.value/count.value.toDouble())
//            }
//    )
//
//    return EventStream(avg)
//}

fun <T : Number>EventStream<out List<NumericEvent<T>>>.averageBuffer() : EventStream<NumericEvent<Double>> {
    val avg = this.observable.map {
        val sum = it.sumByDouble { it.value.toDouble() }
        NumericEvent(sum/it.size.toDouble())
    }

    return EventStream(avg)
}

fun <T : Number>EventStream<out NumericEvent<T>>.probability() : EventStream<NumericEvent<Double>> {
    val prob = this.observable
            .scan(mutableListOf<NumericEvent<T>>(),
                    { acc, ev ->
                        acc.add(ev)
                        acc
                    })
            .filter { list -> list.size > 0}
            .map { list -> NumericEvent(prob(list, list.last().value)) }

    return EventStream(prob)
}

fun <T : Number>EventStream<out List<NumericEvent<T>>>.probabilityBuffer()
        : EventStream<NumericEvent<Double>> {
    val prob = this.observable.map { NumericEvent(prob(it, it.last().value)) }
    return EventStream(prob)
}

fun <T : Number>EventStream<out NumericEvent<T>>.expected() : EventStream<NumericEvent<Double>> {
    val exp = this.observable
            .scan(mutableListOf<NumericEvent<T>>(),
                    { acc, ev ->
                        acc.add(ev)
                        acc
                    })
            .filter { list -> list.size > 0}
            .map { list -> NumericEvent(computeExpectedValue(list)) }

    return EventStream(exp)
}

fun <T : Number>EventStream<out List<NumericEvent<T>>>.expectedBuffer()
        : EventStream<NumericEvent<Double>> {
    val exp = this.observable.map { NumericEvent(computeExpectedValue(it)) }
    return EventStream(exp)
}

fun <T : Number>EventStream<out NumericEvent<T>>.variance() : EventStream<NumericEvent<Double>> {
    val variance = this.observable
            .scan(mutableListOf<NumericEvent<T>>(),
                    { acc, ev ->
                        acc.add(ev)
                        acc
                    })
            .filter { list -> list.size > 0}
            .map { list -> NumericEvent(computeVariance(list)) }

    return EventStream(variance)
}

fun <T : Number>EventStream<out List<NumericEvent<T>>>.varianceBuffer()
        : EventStream<NumericEvent<Double>> {
    val variance = this.observable.map { NumericEvent(computeVariance(it)) }
    return EventStream(variance)
}

/**
 * Given a list of events, calculates the probability of a given event value's outcome.
 */
private fun <T : Number> prob(list: List<NumericEvent<T>>, outcome: T): Double {
    val occ = list.count { it.value == outcome }
    return occ/list.size.toDouble()
}

/**
 * Given a list of events, computes the expected value of an event.
 */
private fun <T : Number> computeExpectedValue(list: List<NumericEvent<T>>) : Double {
    val probabilityList = mutableListOf<Double>()
    val outcomes = list.distinct()

    outcomes.mapTo(probabilityList) { it.value.toDouble() * prob(list, it.value) }

    return probabilityList.sum()
}

/**
 * Given a list of events, computes their variance value.
 */
private fun <T : Number> computeVariance(list: List<NumericEvent<T>>): Double {
    val n = list.size
    var sum = 0.0
    var sumSq = 0.0
    var x: Double

    for (ev in list) {
        x = ev.value.toDouble()
        sum += x
        sumSq += x * x
    }

    return (sumSq - (sum * sum)/n)/n
}

private fun <T, R> EventStream<T>.mapAccumulator(eventStream: EventStream<T>, function: (List<T>) -> R?): EventStream<R?> {
    val min = eventStream.accumulator().observable
            .filter { it.size > 0 }
            .map { function(it) }
    return EventStream(min)
}