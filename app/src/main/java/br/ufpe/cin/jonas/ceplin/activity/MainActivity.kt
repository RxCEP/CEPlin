package br.ufpe.cin.jonas.ceplin.activity

import android.content.Context
import android.content.res.Resources
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MotionEvent
import br.ufpe.cin.jonas.ceplin.EventManager
import br.ufpe.cin.jonas.ceplin.R
import br.ufpe.cin.jonas.ceplin.util.AccelerationEvent
import br.ufpe.cin.jonas.ceplin.util.LightEvent
import br.ufpe.cin.jonas.ceplin.util.ProximityEvent
import br.ufpe.cin.jonas.ceplin.util.TouchEvent
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Math.abs
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), SensorEventListener {

    private var mSensorManager: SensorManager? = null

    private var accSensor: Sensor? = null

    private var lightSensor: Sensor? = null
    private var proxSensor: Sensor? = null

    private val gestureManager = EventManager<TouchEvent>()

    private val accManager = EventManager<AccelerationEvent>()
    private val lightManager = EventManager<LightEvent>()
    private val proxManager = EventManager<ProximityEvent>()

    private val X = 20.dpToPx
    private val Y = 10.dpToPx

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.setContentView(R.layout.activity_main)
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        accSensor = mSensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        lightSensor = mSensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
        proxSensor = mSensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)


        setHorizontalRule()
        //setHorizontalRuleRxOnly()

        setInPocketRule()
        //setInPocketRuleRxOnly()

    }

    private fun setHorizontalRule() {
        gestureManager.asStream().sequence({ a, b ->
            abs(a.x - b.x) > X &&
                    abs(a.y - b.y) < Y
        }, 5)
                .subscribe {
                    log("Horizontal gesture")
                }
    }

    private val publishSubject = PublishSubject.create<TouchEvent>()

    private fun setHorizontalRuleRxOnly(count: Int = 5, skip: Int = count) {
        val sequenceEquals = this.publishSubject
                .buffer(count, skip)
                .filter {
                    var filter = true
                    if (it.isNotEmpty() && count > 1) {
                        for (i in 1..(it.size - 1)) {
                            if (!(abs(it[i - 1].x - it[i].x) > X
                                    && abs(it[i - 1].y - it[i].y) < Y)) {
                                filter = false
                                break
                            }
                        }
                    }
                    filter
                }
        sequenceEquals.subscribe {
            log("Horizontal gesture")
        }
    }

    private fun setInPocketRule() {
        val accelerationRule = accManager.asStream().sequence({ a, b -> a.y > b.y && b.y < -5 }, 2, 1)

        val proximityRule = proxManager.asStream().sequence({ a, b -> a.dist > b.dist && b.dist < 4 }, 2, 1)

        val lightRule = lightManager.asStream().sequence({ a, b -> a.lx > b.lx && b.lx < 10 }, 2, 1)

        accelerationRule.merge(proximityRule).merge(lightRule).subscribe { log("IN POCKET!") }
    }


    private val accelerationEvents = PublishSubject.create<AccelerationEvent>()
    private val proximityEvents = PublishSubject.create<ProximityEvent>()

    private fun setInPocketRuleRxOnly() {
        val accelerationRule = getSequenceObservable(accelerationEvents, { a, b -> a.y > b.y && b.y < -5 }, 2, 1)
        val proximityRule = getSequenceObservable(proximityEvents, { a, b -> a.dist > b.dist && b.dist < 4 }, 2, 1)

        val mergedRules = Observable.merge(accelerationRule, proximityRule)
        mergedRules.buffer(5, TimeUnit.SECONDS, 2)
                .subscribe { bundle ->

                    val events = bundle.listIterator()
                    val values = mutableSetOf<Int>()

                    for (item in events) {
                        when(item.getOrNull(0)){
                            is AccelerationEvent ->{
                                values.add(1)
                            }
                            is ProximityEvent ->{
                                values.add(2)
                            }
                        }
                    }

                    if (values.count() == 2) {
                        log("IN POCKET!")
                    }
                }
    }

    private fun <T> getSequenceObservable(subject: PublishSubject<T>, predicate: (T, T) -> Boolean, count: Int = 5, skip: Int = count): Observable<MutableList<T>>? {
        return subject.buffer(count, skip)
                .filter {
                    var filter = true
                    if (it.isNotEmpty() && count > 1) {
                        for (i in 1..(it.size - 1)) {
                            if (!predicate(it[i - 1], it[i])) {
                                filter = false
                                break
                            }
                        }
                    }
                    filter
                }
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        when (action) {
            MotionEvent.ACTION_MOVE -> {
                gestureManager.addEvent(TouchEvent(event.x, event.y))
                publishSubject.onNext(TouchEvent(event.x, event.y))
            }
        }
        return true
    }

    override fun onSensorChanged(sensorEvent: SensorEvent) {
        when (sensorEvent.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val x = sensorEvent.values[0]
                val y = sensorEvent.values[1]
                val z = sensorEvent.values[2]
                accManager.addEvent(AccelerationEvent(x, y, z))
                accelerationEvents.onNext(AccelerationEvent(x, y, z))
            }
            Sensor.TYPE_LIGHT -> {
                val light = sensorEvent.values[0]
                lightManager.addEvent(LightEvent(light))
            }
            Sensor.TYPE_PROXIMITY -> {
                val prox = sensorEvent.values[0]
                proxManager.addEvent(ProximityEvent(prox))
                proximityEvents.onNext(ProximityEvent(prox))
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    override fun onResume() {
        super.onResume()
        mSensorManager?.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager?.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager?.registerListener(this, proxSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        mSensorManager?.unregisterListener(this)
    }

    //Simple function to log on the phone screen
    fun log(s: String) {
        displayText.text = "$s\n${displayText.text}"
    }

}

val Int.dpToPx: Int get() = (this * Resources.getSystem().displayMetrics.density).toInt()
val Int.pxToDp: Int get() = (this / Resources.getSystem().displayMetrics.density).toInt()