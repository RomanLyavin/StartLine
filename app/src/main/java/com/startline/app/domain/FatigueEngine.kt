package com.startline.app.domain

import com.startline.app.data.Activity
import com.startline.app.data.Fmt
import com.startline.app.data.WellnessDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.sqrt

data class DailyPoint(
    val date: LocalDate,
    val load: Double,
    val ctl: Double,
    val atl: Double,
)

enum class RiskLevel { LOW, MEDIUM, HIGH }

data class FatigueState(
    val ctl: Double = 0.0,
    val atl: Double = 0.0,
    val tsb: Double = 0.0,
    val rampRate: Double = 0.0,
    val acwr: Double = 0.0,
    val monotony: Double = 0.0,
    val strain: Double = 0.0,
    val readiness: Int = 50,
    val label: String = "Нет данных",
    val advice: String = "Синхронизируйте тренировки, чтобы увидеть состояние.",
    val risk: RiskLevel = RiskLevel.LOW,
    val weekLoad: Double = 0.0,
    val weekSessions: Int = 0,
    val load7: Double = 0.0,
    val load28: Double = 0.0,
    val restingHrDelta: Int? = null,
    val sleepScore: Double? = null,
    val timeline: List<DailyPoint> = emptyList(),
    val source: String = "расчёт",
)

/**
 * Модель усталости: CTL (фитнес, 42 дня), ATL (усталость, 7 дней), TSB (форма),
 * ACWR (острое/хроническое отношение нагрузки), монотонность и strain по Фостеру.
 */
object FatigueEngine {

    fun compute(
        activities: List<Activity>,
        wellness: List<WellnessDay> = emptyList(),
        today: LocalDate = LocalDate.now(),
    ): FatigueState {
        if (activities.isEmpty() && wellness.isEmpty()) return FatigueState()

        val loads = HashMap<LocalDate, Double>()
        for (a in activities) {
            if (a.date.isAfter(today)) continue
            loads[a.date] = (loads[a.date] ?: 0.0) + a.load
        }

        val firstActivity = activities.minOfOrNull { it.date }
        val start = (firstActivity ?: today).coerceAtLeast(today.minusDays(365))
            .takeIf { it.isBefore(today) } ?: today.minusDays(90)

        var ctl = 0.0
        var atl = 0.0
        val timeline = ArrayList<DailyPoint>()
        var d = start
        while (!d.isAfter(today)) {
            val load = loads[d] ?: 0.0
            ctl += (load - ctl) / 42.0
            atl += (load - atl) / 7.0
            timeline.add(DailyPoint(d, load, ctl, atl))
            d = d.plusDays(1)
        }

        val weekStart = today.with(DayOfWeek.MONDAY)
        val weekActivities = activities.filter { !it.date.isBefore(weekStart) && !it.date.isAfter(today) }
        val weekLoad = weekActivities.sumOf { it.load }

        fun sumLoad(from: LocalDate, to: LocalDate): Double =
            activities.filter { !it.date.isBefore(from) && !it.date.isAfter(to) }.sumOf { it.load }

        val load7 = sumLoad(today.minusDays(6), today)
        val load28 = sumLoad(today.minusDays(27), today)
        val chronicWeekly = load28 / 4.0
        val acwr = if (chronicWeekly > 1.0) load7 / chronicWeekly else 0.0

        val last7Loads = (0..6).map { daysAgo -> loads[today.minusDays(daysAgo.toLong())] ?: 0.0 }
        val mean7 = last7Loads.average()
        val variance = last7Loads.sumOf { (it - mean7) * (it - mean7) } / 7.0
        val std7 = sqrt(variance)
        val monotony = if (std7 > 0.5) mean7 / std7 else 0.0
        val strain = load7 * monotony

        val lastWellness = wellness.filter { !it.date.isAfter(today) }.maxByOrNull { it.date }
        val wellnessCtl = lastWellness?.ctl
        val wellnessAtl = lastWellness?.atl
        val source = if (wellnessCtl != null && wellnessAtl != null) "intervals.icu" else "расчёт"
        val finalCtl = wellnessCtl ?: ctl
        val finalAtl = wellnessAtl ?: atl
        val tsb = finalCtl - finalAtl

        val restingBaseline = wellness
            .filter { it.restingHr != null && it.date.isAfter(today.minusDays(30)) }
            .mapNotNull { it.restingHr }
            .average()
            .takeIf { !it.isNaN() }
        val restingNow = lastWellness?.restingHr
        val restingDelta = if (restingNow != null && restingBaseline != null) {
            (restingNow - restingBaseline).toInt()
        } else null

        val sleep = lastWellness?.sleepScore

        var readiness = 75.0 + tsb * 1.5
        if (acwr > 1.5) readiness -= 12
        else if (acwr > 1.3) readiness -= 6
        if ((restingDelta ?: 0) >= 5) readiness -= 8
        if (sleep != null && sleep < 70) readiness -= 5
        val readinessInt = readiness.coerceIn(15.0, 98.0).toInt()

        val risk = when {
            tsb < -30 || acwr > 1.55 || readinessInt < 35 -> RiskLevel.HIGH
            tsb < -15 || acwr > 1.3 || readinessInt < 55 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        val label = when {
            readinessInt >= 80 -> "Высокая готовность"
            readinessInt >= 65 -> "Готов к работе"
            readinessInt >= 45 -> "Умеренная усталость"
            readinessInt >= 30 -> "Накоплена усталость"
            else -> "Нужен отдых"
        }

        val advice = when {
            risk == RiskLevel.HIGH && tsb < -30 ->
                "Форма глубоко отрицательная (TSB ${Fmt.signed(tsb)}). Снизьте объём на 30–40% и уберите интервалы на 2–3 дня."
            risk == RiskLevel.HIGH && acwr > 1.5 ->
                "Нагрузка растёт слишком быстро (ACWR ${Fmt.one(acwr)}). Неделя должна быть восстановительной: только Z1–Z2."
            risk == RiskLevel.MEDIUM ->
                "Усталость выше нормы. Оставьте одну качественную тренировку, остальное — легко."
            tsb > 15 ->
                "Форма положительная: можно добавить качественную работу или проверить себя на коротком старте."
            else ->
                "Нагрузка сбалансирована. Держите недельный объём и добавляйте не больше 5–8% к прошлой неделе."
        }

        val ramp = lastWellness?.rampRate ?: if (timeline.size > 8) {
            finalCtl - timeline[maxOf(0, timeline.size - 8)].ctl
        } else 0.0

        return FatigueState(
            ctl = finalCtl,
            atl = finalAtl,
            tsb = tsb,
            rampRate = ramp,
            acwr = acwr,
            monotony = monotony,
            strain = strain,
            readiness = readinessInt,
            label = label,
            advice = advice,
            risk = risk,
            weekLoad = weekLoad,
            weekSessions = weekActivities.size,
            load7 = load7,
            load28 = load28,
            restingHrDelta = restingDelta,
            sleepScore = sleep,
            timeline = timeline.takeLast(120),
            source = source,
        )
    }

    fun daysUntil(date: LocalDate, today: LocalDate = LocalDate.now()): Long =
        ChronoUnit.DAYS.between(today, date)
}

/** Простая статистика по зонам пульса для потока активности. */
object HrZones {
    fun zoneIndex(hr: Int, lthr: Int): Int {
        if (lthr <= 0) return 0
        val ratio = hr.toDouble() / lthr
        return when {
            ratio < 0.85 -> 0
            ratio < 0.90 -> 1
            ratio < 0.95 -> 2
            ratio < 1.00 -> 3
            else -> 4
        }
    }

    fun distribution(heartrate: List<Int?>, lthr: Int): List<Double> {
        val counts = DoubleArray(5)
        var total = 0
        for (hr in heartrate) {
            if (hr == null || hr <= 0) continue
            counts[zoneIndex(hr, lthr)] += 1.0
            total++
        }
        if (total == 0) return List(5) { 0.0 }
        return counts.map { it / total }
    }
}
