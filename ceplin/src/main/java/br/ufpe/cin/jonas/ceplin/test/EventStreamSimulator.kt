package br.ufpe.cin.jonas.ceplin.test

import br.ufpe.cin.jonas.ceplin.Event
import br.ufpe.cin.jonas.ceplin.EventManager
import br.ufpe.cin.jonas.ceplin.EventStream

/**
 * Used to simulate Events subscription
 */
class EventStreamSimulator<T : Event> {
    private var primaryEventManager = EventManager<T>()
    private var secondaryEventManager = EventManager<T>()

    /**
     * Simulate Events list throughout EventStream passing a CEP operator function that needs NO PARAMETERS
     */
    fun simulate(events: List<T>, function: ((stream: EventStream<T>) -> EventStream<T>)): List<T> {
        //create the result variable
        val result = ArrayList<T>()

        // Apply given Function to stream, and subscribe to result
        function(primaryEventManager.asStream()).subscribe {
            result.add(it)
        }

        // add all events to stream
        eventsFromEntries(events, primaryEventManager)
        return result
    }

    /**
     * Simulate Events list throughout EventStream passing a CEP operator function that needs an EventStream as parameter
     */
    fun simulate(events1: List<T>, events2: List<T>, function: ((stream1: EventStream<T>, stream2: EventStream<T>) -> EventStream<T>)): List<T> {
        //create the result variable
        val result = ArrayList<T>()

        // Apply given Function to stream, and subscribe to result
        function(primaryEventManager.asStream(), secondaryEventManager.asStream()).subscribe {
            result.add(it)
        }

        // add all events to stream
        // The secondary stream have to be filled first in order to be inspected from primary in simulation
        eventsFromEntries(events2, secondaryEventManager)
        eventsFromEntries(events1, primaryEventManager)

        return result
    }

    /**
     * Simulate Events list throughout EventStream passing a CEP operator function that needs a Comparator
     */
    fun simulateCompare(events: List<T>, function: ((stream: EventStream<T>) -> EventStream<List<T>>)): List<T> {
        //create the result variable
        var result = listOf<T>()

        // Apply given Function to stream, and subscribe to result
        function(primaryEventManager.asStream()).subscribe {
            result = it
        }

        // add all events to stream
        eventsFromEntries(events, primaryEventManager)
        return result
    }

    /**
     * Simulate Events list throughout EventStream passing a CEP operator function that needs a Grouping function
     */
    fun <R> simulate(events: List<T>, function: ((stream: EventStream<T>) -> EventStream<Map<R, List<T>>>)): Map<R, List<T>> {
        //create the result variable
        var result = mapOf<R, List<T>>()

        // Apply given Function to stream, and subscribe to result
        function(primaryEventManager.asStream()).subscribe {
            result = it
        }

        // add all events to stream
        eventsFromEntries(events, primaryEventManager)
        return result
    }

    /**
     * Add Events to EventManager
     */
    private fun eventsFromEntries(entries: List<T>, manager: EventManager<T>) {
        entries.forEach {
            manager.addEvent(it)
        }
    }
}