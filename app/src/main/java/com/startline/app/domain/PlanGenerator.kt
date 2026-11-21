package com.startline.app.domain

import com.startline.app.data.Fmt
import com.startline.app.data.Profile
import com.startline.app.data.Race
import com.startline.app.data.RacePriority
import com.startline.app.data.Sport
import com.startline.app.data.Workout
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

/**
 * Генератор 4-недельного блока. Периодизация привязана к ближайшему старту класса A
 * (если его нет — к ближайшему старту). Нагрузка ограничена текущим CTL и уровнем усталости.
 */
object PlanEngine {

    private const val DEFAULT_PHASE = "База"

    fun generate(
        profile: Profile,
        fatigue: FatigueState,
        races: List<Race>,
        today: LocalDate = LocalDate.now(),
        weeks: Int = 4,
    ): List<Workout> {
        val futureRaces = races.filter { !it.date.isBefore(today) }.sortedBy { it.date }
        val targetRace = futureRaces.filter { it.priority == RacePriority.A }.minByOrNull { it.date }
            ?: futureRaces.minByOrNull { it.date }

        val result = ArrayList<Workout>()
        val monday = today.with(DayOfWeek.MONDAY)
        val athleteFactor = when {
            fatigue.ctl < 20 -> 0.85
            fatigue.ctl < 45 -> 0.95
            fatigue.ctl < 70 -> 1.0
            else -> 1.08
        }

        for (w in 0 until weeks) {
            val weekStart = monday.plusWeeks(w.toLong())
            val weekEnd = weekStart.plusDays(6)
            val phase = phaseFor(weekStart, weekEnd, targetRace, w)
            val factor = weeklyFactor(phase) * if (fatigue.risk == RiskLevel.HIGH) 0.8 else 1.0
            val targetLoad = maxOf(120.0, fatigue.ctl * 7.0 * factor)
            val draft = ArrayList<Workout>()

            fun add(
                dayOffset: Long,
                sport: Sport,
                type: String,
                title: String,
                description: String,
                durationMin: Int,
                target: String,
                load: Double,
            ) {
                val date = weekStart.plusDays(dayOffset)
                if (date.isBefore(today) || date.isAfter(today.plusDays(weeks * 7L))) return
                draft.add(
                    Workout(
                        userId = profile.userId,
                        sport = sport,
                        date = date,
                        type = type,
                        title = title,
                        description = description,
                        durationMin = (durationMin * athleteFactor).roundToInt().coerceAtLeast(20),
                        target = target,
                        load = load * athleteFactor,
                        phase = phase,
                    )
                )
            }

            // --- бег ---
            when (phase) {
                "Подводка", "Старт" -> {
                    add(
                        1, Sport.RUN, "opener", "Открытие перед стартом",
                        "20–30 мин легко + 3×1 мин в темпе старта, отдых 2 мин. Не загоняйте себя.",
                        (28 * factor).roundToInt(), runPaceTarget(profile, 1.15, 1.30),
                        22.0
                    )
                    add(
                        3, Sport.RUN, "easy", "Лёгкий бег",
                        "Спокойный бег в Z2, дыхание ровное. Заминка 5 мин.",
                        35, runHrTarget(profile, 1), 28.0
                    )
                }
                "Восстановление" -> {
                    add(
                        1, Sport.RUN, "easy", "Восстановительный бег",
                        "Очень легко, Z1–Z2, без ускорений. Цель — восстановление, а не нагрузка.",
                        35, runHrTarget(profile, 0), 20.0
                    )
                    add(
                        5, Sport.RUN, "long", "Аэробная пробежка",
                        "Ровный лёгкий бег Z2. Не гонитесь за темпом.",
                        50, runHrTarget(profile, 1), 38.0
                    )
                }
                "Пик" -> {
                    add(
                        1, Sport.RUN, "vo2", "Интервалы VO2max",
                        "Разминка 15 мин, 5×3 мин в Z5, отдых 2 мин трусцой, заминка 10 мин.",
                        52, runHrTarget(profile, 4), 62.0
                    )
                    add(
                        3, Sport.RUN, "easy", "Лёгкий бег",
                        "Лёгкий бег Z2 после интервалов. Если ноги тяжёлые — сократите до 30 мин.",
                        42, runHrTarget(profile, 1), 30.0
                    )
                    add(
                        5, Sport.RUN, "long", "Длинная пробежка",
                        "Ровный бег Z2. Последние 15 мин можно чуть быстрее, если самочувствие хорошее.",
                        (88 * factor).roundToInt(), runHrTarget(profile, 1), 66.0
                    )
                }
                "Развитие" -> {
                    add(
                        1, Sport.RUN, "threshold", "Пороговая работа",
                        "Разминка 15 мин, 3×8 мин на ПАНО (Z4), отдых 2 мин трусцой, заминка 10 мин.",
                        55, runPaceTarget(profile, 0.98, 1.02), 58.0
                    )
                    add(
                        3, Sport.RUN, "easy", "Лёгкий бег",
                        "Лёгкий бег Z2, в конце 4×20 сек ускорения.",
                        45, runHrTarget(profile, 1), 33.0
                    )
                    add(
                        5, Sport.RUN, "long", "Длинная пробежка",
                        "Ровный бег Z2. Следите за пульсом: не уходите выше верхней границы зоны.",
                        (80 * factor).roundToInt(), runHrTarget(profile, 1), 60.0
                    )
                }
                else -> { // База
                    add(
                        1, Sport.RUN, "tempo", "Аэробный темп",
                        "Разминка 15 мин, 2×12 мин в Z3, отдых 3 мин легко, заминка 10 мин.",
                        50, runHrTarget(profile, 2), 46.0
                    )
                    add(
                        3, Sport.RUN, "easy", "Лёгкий бег",
                        "Спокойный бег Z2, в конце 4×20 сек ускорения.",
                        45, runHrTarget(profile, 1), 32.0
                    )
                    add(
                        5, Sport.RUN, "long", "Длинная пробежка",
                        "Ровный бег Z2. Цель — время в аэробной зоне, а не скорость.",
                        (72 * factor).roundToInt(), runHrTarget(profile, 1), 55.0
                    )
                }
            }

            // --- велосипед ---
            if (phase == "Старт" || phase == "Подводка") {
                add(
                    2, Sport.BIKE, "endurance", "Велосипед: аэробно",
                    "Ровная езда Z2, каденс 85–95. Пить каждые 15–20 мин.",
                    45, bikeHrTarget(profile, 1), 32.0
                )
            } else if (phase == "Восстановление") {
                add(
                    2, Sport.BIKE, "recovery", "Велосипед: восстановление",
                    "Очень легко Z1, 30–40 мин. Без длинных подъёмов.",
                    40, bikeHrTarget(profile, 0), 20.0
                )
                add(
                    6, Sport.BIKE, "endurance", "Велосипед: аэробно",
                    "Спокойная езда Z2, ровный каденс. Не гонитесь за средней скоростью.",
                    60, bikeHrTarget(profile, 1), 42.0
                )
            } else {
                val qualityType = if (phase == "Пик") "bike_vo2" else "sweetspot"
                val qualityTitle = if (phase == "Пик") "Велосипед: VO2max" else "Велосипед: sweet spot"
                val qualityDesc = if (phase == "Пик") {
                    "Разминка 15 мин, 5×3 мин в Z5, отдых 3 мин, заминка 10 мин."
                } else {
                    "Разминка 15 мин, 3×12 мин 88–92% FTP (или Z3–Z4), отдых 5 мин, заминка 10 мин."
                }
                add(
                    2, Sport.BIKE, "endurance", "Велосипед: аэробно",
                    "Ровная езда Z2, каденс 85–95. Пить каждые 15–20 мин.",
                    (75 * factor).roundToInt(), bikeHrTarget(profile, 1), 48.0
                )
                add(
                    4, Sport.BIKE, qualityType, qualityTitle, qualityDesc,
                    62, bikePowerTarget(profile, phase), 64.0
                )
                add(
                    6, Sport.BIKE, "long", "Длинная велотренировка",
                    "Ровная езда Z2, 2–3 ч. Питание 60–80 г углеводов в час, если работа дольше 2 ч.",
                    (105 * factor).roundToInt(), bikeHrTarget(profile, 1), 68.0
                )
            }

            // --- старты недели ---
            val weekRaces = races.filter { !it.date.isBefore(weekStart) && !it.date.isAfter(weekEnd) && !it.date.isBefore(today) }
            for (race in weekRaces) {
                draft.removeAll { it.date == race.date && it.type != "race" }
                draft.removeAll { it.date == race.date.minusDays(1) && it.type !in listOf("easy", "recovery") }
                val duration = raceDuration(profile, race)
                val load = when (race.priority) {
                    RacePriority.A -> 110.0
                    RacePriority.B -> 80.0
                    RacePriority.C -> 55.0
                }
                draft.add(
                    Workout(
                        userId = profile.userId,
                        sport = race.sport,
                        date = race.date,
                        type = "race",
                        title = "Старт: ${race.name}",
                        description = buildString {
                            append("Приоритет ${race.priority.key}. ")
                            if (race.distanceKm != null) append("Дистанция ${Fmt.one(race.distanceKm)} км. ")
                            append(
                                if (race.priority == RacePriority.A) {
                                    "Главный старт: разложите силы, первые 20% дистанции — спокойно."
                                } else {
                                    "Контрольный старт: отработайте питание и темп, без геройства."
                                }
                            )
                        },
                        durationMin = duration,
                        target = if (race.sport == Sport.RUN) {
                            runPaceTarget(profile, 1.0, 1.08)
                        } else {
                            bikePowerTarget(profile, "Пик")
                        },
                        load = load,
                        phase = "Старт",
                    )
                )
                val openerDay = race.date.minusDays(1)
                if (!openerDay.isBefore(today)) {
                    draft.add(
                        Workout(
                            userId = profile.userId,
                            sport = race.sport,
                            date = openerDay,
                            type = "opener",
                            title = "Открытие перед стартом",
                            description = "20–30 мин легко + 3×1 мин в темпе старта. Ноги должны быть свежими.",
                            durationMin = 30,
                            target = if (race.sport == Sport.RUN) runPaceTarget(profile, 1.1, 1.25) else bikeHrTarget(profile, 1),
                            load = 18.0,
                            phase = "Старт",
                        )
                    )
                }
            }

            // --- нормировка под недельную цель нагрузки ---
            val nonRace = draft.filter { it.type != "race" }
            val raceLoad = draft.filter { it.type == "race" }.sumOf { it.load }
            val rawLoad = nonRace.sumOf { it.load }
            if (rawLoad > 0.0) {
                val desired = (targetLoad - raceLoad).coerceAtLeast(60.0)
                val ratio = (desired / rawLoad).coerceIn(0.75, 1.3)
                for (i in draft.indices) {
                    val w = draft[i]
                    if (w.type == "race") continue
                    draft[i] = w.copy(
                        durationMin = ((w.durationMin * ratio) / 5.0).roundToInt().coerceAtLeast(15) * 5,
                        load = (w.load * ratio * 10).roundToInt() / 10.0,
                    )
                }
            }

            result.addAll(draft)
        }

        // Стартовая неделя: не дублируем тренировки в день гонки.
        return result.sortedWith(compareBy({ it.date }, { it.sport.ordinal }))
    }

    private fun phaseFor(weekStart: LocalDate, weekEnd: LocalDate, race: Race?, index: Int): String {
        if (race != null) {
            if (!race.date.isBefore(weekStart) && !race.date.isAfter(weekEnd)) return "Старт"
            val days = ChronoUnit.DAYS.between(weekEnd, race.date)
            return when {
                days <= 14 -> "Подводка"
                days <= 28 -> "Пик"
                days <= 56 -> "Развитие"
                else -> DEFAULT_PHASE
            }
        }
        return when (index % 4) {
            0, 1 -> "База"
            2 -> "Развитие"
            else -> "Восстановление"
        }
    }

    private fun weeklyFactor(phase: String): Double = when (phase) {
        "Развитие" -> 1.08
        "Пик" -> 1.12
        "Подводка" -> 0.62
        "Старт" -> 0.45
        "Восстановление" -> 0.68
        else -> 1.0
    }

    private fun raceDuration(profile: Profile, race: Race): Int {
        val distance = race.distanceKm
        val minutes = when (race.sport) {
            Sport.RUN -> {
                val pace = race.targetSec?.let { if (distance != null && distance > 0) it / distance else null }
                    ?: profile.thresholdPaceSec?.toDouble()
                if (distance != null && pace != null) distance * pace * 1.05 / 60.0 else 60.0
            }
            Sport.BIKE -> if (distance != null) distance / 30.0 * 60.0 else 90.0
        }
        return minutes.roundToInt().coerceIn(25, 360)
    }

    private fun hrZone(profile: Profile, sport: Sport, index: Int): Pair<Int, Int>? {
        val zones = AthleteModelBuilder.buildHrZones(profile, sport)
        val z = zones.getOrNull(index) ?: return null
        return z.low to z.high
    }

    private fun runHrTarget(profile: Profile, zone: Int): String {
        val r = hrZone(profile, Sport.RUN, zone) ?: return "Ровный темп по дыханию"
        return "Пульс ${r.first}–${r.second}"
    }

    private fun bikeHrTarget(profile: Profile, zone: Int): String {
        val r = hrZone(profile, Sport.BIKE, zone) ?: return "Ровный темп по дыханию"
        return "Пульс ${r.first}–${r.second}"
    }

    private fun runPaceTarget(profile: Profile, fastFactor: Double, slowFactor: Double): String {
        val t = profile.thresholdPaceSec ?: return "Темп по дыханию"
        val fast = (t * fastFactor).roundToInt()
        val slow = (t * slowFactor).roundToInt()
        return "Темп ${Fmt.paceShort(fast)}–${Fmt.paceShort(slow)} /км"
    }

    private fun bikePowerTarget(profile: Profile, phase: String): String {
        val ftp = profile.ftpWatts
        return if (ftp != null && ftp > 0) {
            if (phase == "Пик") {
                "Мощность ${(ftp * 1.05).roundToInt()}–${(ftp * 1.15).roundToInt()} Вт"
            } else {
                "Мощность ${(ftp * 0.88).roundToInt()}–${(ftp * 0.92).roundToInt()} Вт"
            }
        } else {
            bikeHrTarget(profile, if (phase == "Пик") 4 else 2)
        }
    }
}
