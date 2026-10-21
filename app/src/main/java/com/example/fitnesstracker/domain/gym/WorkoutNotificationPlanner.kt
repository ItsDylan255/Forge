package com.example.fitnesstracker.domain.gym

/**
 * Inhalt der Trainings-Benachrichtigung. [chronometerBase] ist der Zeitstempel für die
 * Systemuhr der Benachrichtigung: Ablaufzeit bei [countDown], sonst Trainingsbeginn.
 * [progressMax] = 0 bedeutet: kein Fortschrittsbalken.
 */
data class NotificationPlan(
    val title: String,
    val text: String,
    val chronometerBase: Long,
    val countDown: Boolean,
    val progressMax: Int,
    val progress: Int
)

object WorkoutNotificationPlanner {

    /**
     * Läuft eine Pause ([restEndsAt] in der Zukunft), zählt die Uhr bis zu ihrem Ende herunter und
     * der Balken füllt sich. Ist sie schon vorbei, steht ein Hinweis da. Ohne Pause ([restEndsAt] = 0)
     * zeigt die Uhr die Trainingsdauer.
     */
    fun plan(
        sessionName: String,
        startedAt: Long,
        restEndsAt: Long,
        restTotalMillis: Long,
        restLabel: String,
        now: Long
    ): NotificationPlan {
        val title = sessionName.ifBlank { "Training" }

        return when {
            restEndsAt > now -> {
                val max = (restTotalMillis / 1000L).toInt().coerceAtLeast(1)
                val elapsed = ((restTotalMillis - (restEndsAt - now)) / 1000L).toInt().coerceIn(0, max)
                NotificationPlan(
                    title = title,
                    text = if (restLabel.isBlank()) "Pause" else "Pause · $restLabel",
                    chronometerBase = restEndsAt,
                    countDown = true,
                    progressMax = max,
                    progress = elapsed
                )
            }

            restEndsAt > 0L -> NotificationPlan(
                title = title,
                text = "Pause vorbei · weiter mit dem nächsten Satz",
                chronometerBase = startedAt,
                countDown = false,
                progressMax = 0,
                progress = 0
            )

            else -> NotificationPlan(
                title = title,
                text = "Training läuft",
                chronometerBase = startedAt,
                countDown = false,
                progressMax = 0,
                progress = 0
            )
        }
    }
}
