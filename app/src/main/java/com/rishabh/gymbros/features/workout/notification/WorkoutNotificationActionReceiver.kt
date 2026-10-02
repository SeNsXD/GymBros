package com.rishabh.gymbros.features.workout.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.rishabh.gymbros.features.workout.data.WorkoutRepository

class WorkoutNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val repository = WorkoutRepository.getInstance(context.applicationContext)
        when (intent.action) {
            ACTION_MINUS_15 -> repository.adjustRest(-15)
            ACTION_PLUS_30 -> repository.adjustRest(30)
            ACTION_SKIP -> repository.skipRest()
            ACTION_REST_FINISHED -> repository.skipRest()
        }
        // Repository writes asynchronously; redraw shortly after the state change is persisted.
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (repository.hasActiveWorkout()) WorkoutActiveNotification.show(context)
            else WorkoutActiveNotification.cancel(context)
        }, 180L)
    }

    companion object {
        const val ACTION_MINUS_15 = "com.rishabh.gymbros.workout.MINUS_15"
        const val ACTION_PLUS_30 = "com.rishabh.gymbros.workout.PLUS_30"
        const val ACTION_SKIP = "com.rishabh.gymbros.workout.SKIP_REST"
        const val ACTION_REST_FINISHED = "com.rishabh.gymbros.workout.REST_FINISHED"
    }
}
