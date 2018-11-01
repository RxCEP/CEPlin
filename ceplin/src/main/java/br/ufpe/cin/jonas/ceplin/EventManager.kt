package br.ufpe.cin.jonas.ceplin

import io.reactivex.subjects.PublishSubject

class EventManager<T : Event> {

    private val events = PublishSubject.create<T>()

    fun addEvent(event: T) {
        this.events.onNext(event)
    }

    fun asStream(): EventStream<T> {
        return EventStream(this.events)
    }
}