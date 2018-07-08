package br.ufpe.cin.jonas.ceplin

import br.ufpe.cin.jonas.ceplin.util.IntEvent
import io.reactivex.Observable
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

    fun <R> merge(stream: EventStream<R>): ComplexEvent {
        var merged = Observable.merge(
            this.observable.map { element -> Pair(element, 1) },
            stream.observable.map { element -> Pair(element, 2) }
        )
        return ComplexEvent(observable = merged, numberOfEvents = 2)
    }

    fun buffer(timespan: Long, timeUnit: TimeUnit): EventStream<List<T>> {
        return EventStream(this.observable.buffer(timespan, timeUnit))
    }

    fun window(timespan: Long, timeUnit: TimeUnit): EventStream<T> {
        return EventStream(this.observable.buffer(timespan, timeUnit).flatMap{Observable.fromIterable(it)})
    }

    fun union(stream: EventStream<T>): EventStream<T> {
        val merged = Observable.merge(
            this.observable,
            stream.observable).distinct()
        return EventStream<T>(merged)
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
