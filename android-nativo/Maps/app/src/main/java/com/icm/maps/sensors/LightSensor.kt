package com.icm.maps.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Composable que escucha los cambios en el sensor de luz y activa o desactiva el modo oscuro
 * según la cantidad de luz detectada.
 *
 * @param onLightChanged Función que se llama cuando cambia la luz. Recibe un booleano que indica
 * si el modo oscuro debe activarse (true) o desactivarse (false).
 */
@Composable
fun LightSensor(onLightChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val sensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) }

    val currentOnLightChanged by rememberUpdatedState(onLightChanged)

    LifecycleResumeEffect(sensorManager, sensor) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.values?.firstOrNull()?.let { lux ->
                    currentOnLightChanged(lux < 2500) // Si la luz es baja, activar modo oscuro
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (sensor != null) {
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        onPauseOrDispose { sensorManager.unregisterListener(listener) }
    }
}
