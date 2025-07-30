package com.ingredientguard.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class NotificationHelper(private val context: Context) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val CHANNEL_ID = "allergen_alerts"
        private const val CHANNEL_NAME = "Alertă Alergeni"
        private const val NOTIFICATION_ID = 1001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificări pentru alergeni detectați"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showAllergenAlert(
        personalAllergens: List<String>,
        productName: String = "produs"
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ ALERTĂ ALERGENI!")
            .setContentText("$productName conține: ${personalAllergens.joinToString(", ")}")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Produsul scanat conține următorii alergeni din lista ta personală:\n\n${personalAllergens.joinToString("\n• ", "• ")}")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            .setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_LIGHTS)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun showScanCompleteNotification(
        allergensFound: Int,
        hasPersonalAllergens: Boolean = false
    ) {
        val title = if (hasPersonalAllergens) {
            "Scanare completă - ATENȚIE!"
        } else {
            "Scanare completă"
        }

        val content = when {
            hasPersonalAllergens -> "Găsit alergeni personali în produs!"
            allergensFound > 0 -> "Găsiți $allergensFound alergeni în produs"
            else -> "Niciun alergen detectat"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(if (hasPersonalAllergens) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setColor(
                if (hasPersonalAllergens) {
                    ContextCompat.getColor(context, android.R.color.holo_red_dark)
                } else {
                    ContextCompat.getColor(context, android.R.color.holo_blue_dark)
                }
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }

    fun isNotificationEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(CHANNEL_ID)
            channel?.importance != NotificationManager.IMPORTANCE_NONE
        } else {
            true
        }
    }
}