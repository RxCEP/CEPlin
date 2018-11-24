package br.ufpe.cin.jonas.ceplin

import br.ufpe.cin.jonas.ceplin.util.IntEvent
import io.reactivex.Observable
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.functions.Function
import io.reactivex.observables.GroupedObservable
import java.util.concurrent.TimeUnit

class EventStream<T>(val observable: Observable<T>) {

    fun subscribe(onNext: ((T) -> Unit)) {
        this.observable.subscribe(onNext)
    }

    fun filter(predicate: (T) -> Boolean): EventStream<T> {
        return EventStream(this.observable.filter(predicate))
    }

    fun <R> map(transform: ((T) -> R)): EventStream<R> {
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

    fun buffer(timespan: Long, timeUnit: TimeUnit): EventStream<List<T>> {
        return EventStream(this.observable.buffer(timespan, timeUnit))
    }

    fun window(timespan: Long, timeUnit: TimeUnit): EventStream<T> {
        return EventStream(this.observable.buffer(timespan, timeUnit).flatMap { Observable.fromIterable(it) })
    }

    /**
     * Equivalent to Cugola Join operator
     *
     * <p>
     *     Merges the current stream with another
     * </p>
     */
    fun <R> merge(stream: EventStream<R>): ComplexEvent {
        var merged = Observable.merge(
                this.observable.map { element -> Pair(element, 1) },
                stream.observable.map { element -> Pair(element, 2) }
        )
        return ComplexEvent(observable = merged, numberOfEvents = 2)
    }

    /**
     * Equivalent to Cugola Union operator
     *
     * <p>
     *     Merges the current stream with another, excluding copies
     * </p>
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
        val streamAccumulated = EventStream<MutableList<T>?>(stream.accumulator().observable.startWith(ArrayList<T>()))
        val filtered = this.observable.withLatestFrom(streamAccumulated.observable).filter { (event, accumulated) ->
            accumulated?.filter {
                it == event
            }?.count() == 0
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
        val streamAccumulated = EventStream<MutableList<T>?>(stream.accumulator().observable.startWith(ArrayList<T>()))
        val filtered = this.observable.withLatestFrom(streamAccumulated.observable).filter { (event, accumulated) ->
            accumulated?.filter {
                it == event
            }?.count() != 0
        }.map {
            it.first
        }.distinct()
        return EventStream<T>(filtered)
    }

    /**
     * Equivalent to Cugola Order by operator
     *
     * <p>
     *     This is a simple wrap of RxJava sorted, to sort elements by given comparator
     * </p>
     */
    fun orderBy(comparison: Comparator<MutableList<T>?>): EventStream<MutableList<T>?> {
        val ordered = this.accumulator().observable.sorted(comparison)
        return EventStream(ordered)
    }

    /**
     * Equivalent to Cugola Group by operator
     *
     * <p>
     *     This is a simple wrap of RxJava groupBy, to split elements by given grouping function
     * </p>
     */
    fun groupBy(comparison: Function<MutableList<T>?, T>): EventStream<GroupedObservable<T?, MutableList<T>?>?> {
        val grouped = this.accumulator().observable.groupBy(comparison)
        return EventStream(grouped)
    }

    fun <R> distinct(transform: ((T) -> R)): EventStream<R> {
        return EventStream<R>(this.observable.map(transform))
    }
}

fun <T : Comparable<T>> EventStream<T>.max(): EventStream<T?> {
    return mapAccumulator(this, { it?.max() })
}

fun <T : Comparable<T>> EventStream<T>.min(): EventStream<T?> {
    return mapAccumulator(this, { it?.min() })
}

fun <T : Comparable<T>> EventStream<T>.sumBy(selector: (T) -> Int): EventStream<IntEvent?> {
    return this.mapAccumulator(this, { IntEvent(it.sumBy(selector)) })
}

private fun <T, R> EventStream<T>.mapAccumulator(eventStream: EventStream<T>, function: (List<T>) -> R?): EventStream<R?> {
    val min = eventStream.accumulator().observable
            .filter { it.size > 0 }
            .map { function(it) }
    return EventStream(min)
}