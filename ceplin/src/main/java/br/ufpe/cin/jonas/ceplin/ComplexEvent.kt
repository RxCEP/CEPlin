package br.ufpe.cin.jonas.ceplin

import io.reactivex.Observable
import java.util.concurrent.TimeUnit

class ComplexEvent(val observable: Observable<Pair<Any?, Int>>,
                   val numberOfEvents: Int,
                   val timespan: Long = 5,
                   val timeUnit: TimeUnit = TimeUnit.SECONDS) {

    fun subscribe(onComplete: () -> Unit) {
        this.observable.buffer(timespan, timeUnit, numberOfEvents)
            .subscribe { bundle ->

                val events = bundle.listIterator()
                val values = mutableSetOf<Int>()

                for (item in events) {
                    values.add(item.second)
                }

                if (values.count() == this.numberOfEvents) {
                    onComplete()
                }
            }
    }

    fun <E> merge(eventStream: EventStream<E>): ComplexEvent {
        var merged = Observable.merge(
            this.observable,
            eventStream.observable.map { element -> Pair(element, numberOfEvents + 1) }
        )
        return ComplexEvent(merged, numberOfEvents + 1)
    }
}