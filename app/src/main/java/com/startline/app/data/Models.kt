package com.startline.app.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class Sport(val key: String, val label: String) {
    RUN("run", "Бег"),
    BIKE("bike", "Велосипед"),
    STRENGTH("strength", "Сила");

    companion object {
        fun fromKey(key: String?): Sport? = entries.firstOrNull { it.key == key }

        fun fromIcu(type: String?): Sport? = when (type) {
            "Run", "VirtualRun", "TrailRun" -> RUN
            "Ride", "VirtualRide", "MountainBikeRide", "GravelRide", "TrackRide",
            "Cyclocross", "EBikeRide" -> BIKE
            else -> null
        }

        /**
         * Тип активности из сервисов часов (Amazfit, Garmin, Huawei, Polar).
         * Имена различаются, поэтому сравнение идёт по нормализованному значению.
         */
        fun fromWatchType(type: String?): Sport? {
            val t = type.orEmpty().trim().lowercase()
            return when {
                t in setOf("run", "running", "trail_run", "trailrunning", "athletics", "indoor_running") -> RUN
                t in setOf("bike", "cycling", "ride", "mountain_bike", "mtb", "indoor_cycling",
                    "gravel", "cyclocross", "ebike", "bicycling") -> BIKE
                t in setOf("strength", "strength_training", "strengthtraining", "gym", "functional") -> STRENGTH
                else -> null
            }
        }
    }
}

enum class RacePriority(val key: String, val label: String, val hint: String) {
    A("A", "A", "Главный старт сезона"),
    B("B", "B", "Контрольный старт"),
    C("C", "C", "Тренировочный старт");

    companion object {
        fun fromKey(key: String?): RacePriority =
            entries.firstOrNull { it.key == key } ?: C
    }
}

data class User(
    val id: Long,
    val email: String,
)

data class Profile(
    val userId: Long = 0,
    val name: String = "",
    val heightCm: Int? = null,
    val weightKg: Double? = null,
    val age: Int? = null,
    val sex: String = "M",
    val hrMaxRun: Int? = null,
    val hrMaxBike: Int? = null,
    val hrRest: Int? = null,
    val lthrRun: Int? = null,
    val lthrBike: Int? = null,
    val thresholdPaceSec: Int? = null,
    val ftpWatts: Int? = null,
    val intervalsId: String = "0",
    val intervalsKey: String = "",
    val trainingDays: Int = 6,
    val longRunDay: Int = 6,
    val highIntensityDay: Int = 2,
    val strengthDay: Int = 4,
    /** Частота силовых: 1 — каждую неделю, 2 — раз в 2 недели и т. д. */
    val strengthEveryWeeks: Int = 1,
    val aiBaseUrl: String = "https://openrouter.ai/api/v1",
    val aiModel: String = "deepseek/deepseek-chat-v3-0324:free",
    val aiKey: String = "",
) {
    val isFilled: Boolean
        get() = name.isNotBlank() && weightKg != null && hrMaxRun != null && hrRest != null &&
            lthrRun != null && lthrBike != null && thresholdPaceSec != null

    fun ageGroup(): String = when {
        age == null -> "возраст не указан"
        age < 30 -> "18–29"
        age < 40 -> "30–39"
        age < 50 -> "40–49"
        age < 60 -> "50–59"
        else -> "60+"
    }
}

data class Activity(
    val id: String,
    val userId: Long,
    val sport: Sport,
    val name: String,
    val date: LocalDate,
    val startLocal: String,
    val startEpoch: Long,
    val distanceM: Double,
    val movingSec: Int,
    val elapsedSec: Int,
    val load: Double,
    val avgHr: Int?,
    val maxHr: Int?,
    val avgSpeed: Double?,
    val elevationM: Double?,
    val calories: Int?,
    val avgPower: Int?,
    val normPower: Int?,
    val ctl: Double?,
    val atl: Double?,
    /** Признак «гонка»: тренировка отмечена как старт (источник — intervals.icu или часы). */
    val isRace: Boolean = false,
    /** Откуда попала тренировка: intervals.icu или подключённые часы. */
    val source: String = "intervals",
)

data class WellnessDay(
    val date: LocalDate,
    val ctl: Double?,
    val atl: Double?,
    val rampRate: Double?,
    val restingHr: Int?,
    val weightKg: Double?,
    val sleepSecs: Int?,
    val sleepScore: Double?,
)

data class Race(
    val id: Long = 0,
    val userId: Long,
    val sport: Sport,
    val name: String,
    val date: LocalDate,
    val priority: RacePriority,
    val distanceKm: Double? = null,
    val targetSec: Int? = null,
    val notes: String = "",
) {
    fun daysLeft(today: LocalDate = LocalDate.now()): Long = ChronoUnit.DAYS.between(today, date)
}

data class Workout(
    val id: Long = 0,
    val userId: Long,
    val sport: Sport,
    val date: LocalDate,
    val type: String,
    val title: String,
    val description: String,
    val durationMin: Int,
    val target: String,
    val load: Double,
    val phase: String,
)

data class ActivityStreams(
    val time: List<Int>,
    val heartrate: List<Int?>,
    val speed: List<Double?>,
    val distance: List<Double?>,
    val altitude: List<Double?>,
)

data class UserRecord(
    val user: User,
    val passwordHash: String,
    val salt: String,
)

object Fmt {
    private val dateRu = DateTimeFormatter.ofPattern("d MMMM", Locale("ru"))
    private val dateFullRu = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("ru"))
    private val dateShort = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    private val weekdays = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")
    private val weekdayNames = listOf(
        "Понедельник", "Вторник", "Среда", "Четверг", "Пятница", "Суббота", "Воскресенье",
    )
    private val months = listOf(
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря",
    )
    private val monthsNom = listOf(
        "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
        "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь",
    )

    fun date(d: LocalDate): String = d.format(dateRu)
    fun dateFull(d: LocalDate): String = d.format(dateFullRu)
    fun dateShort(d: LocalDate): String = d.format(dateShort)
    fun monthNom(m: Int): String = monthsNom[m - 1]
    fun weekday(d: LocalDate): String = weekdays[d.dayOfWeek.value - 1]

    /** День недели по ISO: 1 — понедельник, 7 — воскресенье. */
    fun weekdayName(day: Int): String = weekdayNames[(day - 1).coerceIn(0, 6)]

    fun weekdayShort(day: Int): String = weekdays[(day - 1).coerceIn(0, 6)].replaceFirstChar { it.uppercase() }

    fun dayMonth(d: LocalDate): String = "${d.dayOfMonth} ${months[d.monthValue - 1]}"

    fun duration(sec: Int): String {
        if (sec <= 0) return "—"
        val h = sec / 3600
        val m = (sec % 3600) / 60
        val s = sec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    fun durationMin(min: Int): String {
        val h = min / 60
        val m = min % 60
        return if (h > 0) "$h ч ${m.toString().padStart(2, '0')} мин" else "$m мин"
    }

    fun distance(m: Double): String = when {
        m >= 10000 -> "%.1f км".format(m / 1000)
        m >= 1000 -> "%.2f км".format(m / 1000)
        else -> "%.0f м".format(m)
    }

    fun pace(secPerKm: Double?): String {
        if (secPerKm == null || secPerKm <= 0 || secPerKm > 3600) return "—"
        val s = secPerKm.toInt()
        return "%d:%02d /км".format(s / 60, s % 60)
    }

    fun paceShort(secPerKm: Int?): String {
        if (secPerKm == null || secPerKm <= 0) return "—"
        return "%d:%02d".format(secPerKm / 60, secPerKm % 60)
    }

    fun speedKmh(mps: Double?): String {
        if (mps == null || mps <= 0) return "—"
        return "%.1f км/ч".format(mps * 3.6)
    }

    fun load(v: Double?): String = if (v == null) "—" else "%.0f".format(v)

    fun signed(v: Double): String = if (v >= 0) "+%.0f".format(v) else "%.0f".format(v)

    fun one(v: Double): String = "%.1f".format(v)

    fun hoursMin(totalMin: Int): String {
        val h = totalMin / 60
        val m = totalMin % 60
        return if (h > 0) "$h:${m.toString().padStart(2, '0')}" else "${m}м"
    }

    fun plural(n: Int, one: String, few: String, many: String): String = when {
        n % 10 == 1 && n % 100 != 11 -> one
        n % 10 in 2..4 && n % 100 !in 12..14 -> few
        else -> many
    }
}
