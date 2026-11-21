package com.startline.app.domain

import com.startline.app.data.Activity
import com.startline.app.data.Fmt
import com.startline.app.data.Profile
import com.startline.app.data.Sport
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

data class HrZone(
    val name: String,
    val low: Int,
    val high: Int,
    val purpose: String,
) {
    fun range(): String = "$low–$high"
}

data class PaceZone(
    val name: String,
    val fastSec: Int,
    val slowSec: Int,
    val note: String,
) {
    fun range(): String = "${Fmt.paceShort(fastSec)}–${Fmt.paceShort(slowSec)} /км"
}

data class AthleteModel(
    val weeklyHours: Double = 0.0,
    val weeklySessions: Double = 0.0,
    val weeklyLoad: Double = 0.0,
    val runShare: Int = 0,
    val bikeShare: Int = 0,
    val intensityShare: Int = 0,
    val consistency: Int = 0,
    val longRunMin: Int = 0,
    val longRideMin: Int = 0,
    val hrZonesRun: List<HrZone> = emptyList(),
    val hrZonesBike: List<HrZone> = emptyList(),
    val paceZones: List<PaceZone> = emptyList(),
    val strengths: List<String> = emptyList(),
    val limiters: List<String> = emptyList(),
    val summary: String = "",
    val runLoad28: Double = 0.0,
    val bikeLoad28: Double = 0.0,
)

object AthleteModelBuilder {

    fun build(
        profile: Profile,
        activities: List<Activity>,
        fatigue: FatigueState,
        today: LocalDate = LocalDate.now(),
        recentDays: Int = 28,
    ): AthleteModel {
        val from = today.minusDays(recentDays.toLong())
        val recent = activities.filter { !it.date.isBefore(from) && !it.date.isAfter(today) }
        val runActs = recent.filter { it.sport == Sport.RUN }
        val bikeActs = recent.filter { it.sport == Sport.BIKE }

        val weeklyHours = recent.sumOf { it.movingSec } / 3600.0 / (recentDays / 7.0)
        val weeklySessions = recent.size / (recentDays / 7.0)
        val runLoad = runActs.sumOf { it.load }
        val bikeLoad = bikeActs.sumOf { it.load }
        val totalLoad = runLoad + bikeLoad
        val runShare = if (totalLoad > 0) (runLoad / totalLoad * 100).roundToInt() else 0
        val bikeShare = if (totalLoad > 0) (bikeLoad / totalLoad * 100).roundToInt() else 0

        val lthrRun = profile.lthrRun ?: profile.hrMaxRun?.let { (it * 0.9).roundToInt() }
        val lthrBike = profile.lthrBike ?: profile.hrMaxBike?.let { (it * 0.9).roundToInt() }

        val intensitySessions = recent.count { a ->
            val lthr = if (a.sport == Sport.RUN) lthrRun else lthrBike
            a.avgHr != null && lthr != null && lthr > 0 && a.avgHr >= lthr * 0.92
        }
        val intensityShare = if (recent.isNotEmpty()) (intensitySessions * 100.0 / recent.size).roundToInt() else 0

        val weeks = 8
        var activeWeeks = 0
        for (w in 0 until weeks) {
            val end = today.minusDays(w * 7L)
            val start = end.minusDays(6)
            val count = activities.count { !it.date.isBefore(start) && !it.date.isAfter(end) }
            if (count >= 3) activeWeeks++
        }
        val consistency = (activeWeeks * 100.0 / weeks).roundToInt()

        val longRunMin = runActs.maxOfOrNull { it.movingSec }?.div(60) ?: 0
        val longRideMin = bikeActs.maxOfOrNull { it.movingSec }?.div(60) ?: 0

        val hrRun = buildHrZones(profile, Sport.RUN)
        val hrBike = buildHrZones(profile, Sport.BIKE)
        val paceZones = buildPaceZones(profile.thresholdPaceSec)

        val strengths = ArrayList<String>()
        val limiters = ArrayList<String>()

        if (consistency >= 70) strengths.add("Стабильность $consistency%: тренировки идут регулярно, это главный ресурс роста.")
        if (weeklyHours >= 5.5) strengths.add("Объём ${Fmt.one(weeklyHours)} ч/нед — достаточная база для развития.")
        if (longRunMin >= 75) strengths.add("Длинная пробежка до $longRunMin мин — хорошая аэробная опора.")
        if (longRideMin >= 90) strengths.add("Длинная велотренировка до $longRideMin мин — выносливость на дистанции.")
        if (intensityShare in 8..30 && recent.size >= 6) strengths.add("Здоровая доля интенсивности ($intensityShare%) — есть и лёгкие, и качественные дни.")
        if (runShare in 30..70 && bikeShare in 30..70 && recent.size >= 8) strengths.add("Сбалансированная нагрузка между бегом и велосипедом.")

        if (weeklyHours < 3) limiters.add("Малый недельный объём (${Fmt.one(weeklyHours)} ч). Начните с +30–40 мин лёгкой работы.")
        if (consistency < 50) limiters.add("Регулярность ниже 50%: не хватает 3–4 тренировок в неделю.")
        if (longRunMin in 1..59) limiters.add("Самая длинная пробежка за месяц — $longRunMin мин. Для длинных стартов нужен прогресс до 75–100 мин.")
        if (intensityShare > 35) limiters.add("Слишком много интенсивности ($intensityShare%). Добавьте лёгких дней в Z1–Z2.")
        if (fatigue.rampRate > 8) limiters.add("Быстрый рост нагрузки (+${Fmt.one(fatigue.rampRate)} CTL/нед). Ограничьте прибавку 5–8%.")
        if (recent.any { it.sport == Sport.BIKE }.not() && recent.isNotEmpty()) limiters.add("Нет велотренировок за 4 недели: велосипедная форма не поддерживается.")
        if (profile.thresholdPaceSec == null) limiters.add("Не указан темп ПАНО — темповые зоны бега рассчитаны ориентировочно.")
        if (profile.ftpWatts == null) limiters.add("Не указан FTP — мощность на велосипеде задаётся по пульсу.")

        if (strengths.isEmpty()) strengths.add("Есть база активности — можно строить прогрессию от текущего уровня.")
        if (limiters.isEmpty()) limiters.add("Явных ограничений нет. Следите за сном и не повышайте нагрузку более чем на 8% в неделю.")

        val summary = buildString {
            append("За 28 дней: ${Fmt.one(weeklyHours)} ч/нед, ")
            append("${Fmt.one(weeklySessions)} тренировки/нед, ")
            append("CTL ${Fmt.one(fatigue.ctl)}. ")
            append("Бег $runShare% / велосипед $bikeShare% нагрузки.")
        }

        return AthleteModel(
            weeklyHours = weeklyHours,
            weeklySessions = weeklySessions,
            weeklyLoad = totalLoad / 4.0,
            runShare = runShare,
            bikeShare = bikeShare,
            intensityShare = intensityShare,
            consistency = consistency,
            longRunMin = longRunMin,
            longRideMin = longRideMin,
            hrZonesRun = hrRun,
            hrZonesBike = hrBike,
            paceZones = paceZones,
            strengths = strengths.take(3),
            limiters = limiters.take(3),
            summary = summary,
            runLoad28 = runLoad,
            bikeLoad28 = bikeLoad,
        )
    }

    fun buildHrZones(profile: Profile, sport: Sport): List<HrZone> {
        val lthr = (if (sport == Sport.RUN) profile.lthrRun else profile.lthrBike)
            ?: return emptyList()
        val maxHr = if (sport == Sport.RUN) profile.hrMaxRun else profile.hrMaxBike
        fun bpm(v: Double) = (lthr * v).roundToInt()
        return listOf(
            HrZone("Z1 Восстановление", bpm(0.60), bpm(0.85) - 1, "лёгкий разговор, разогрев"),
            HrZone("Z2 Аэробная", bpm(0.85), bpm(0.90) - 1, "база, длинные тренировки"),
            HrZone("Z3 Темповая", bpm(0.90), bpm(0.95) - 1, "ровный темп, sweet spot"),
            HrZone("Z4 Порог", bpm(0.95), maxOf(bpm(0.95), lthr), "ПАНО, пороговая работа"),
            HrZone("Z5 VO2max", lthr + 1, maxHr ?: bpm(1.08), "короткие интервалы"),
        )
    }

    fun buildPaceZones(thresholdPaceSec: Int?): List<PaceZone> {
        val t = thresholdPaceSec ?: return emptyList()
        fun sec(f: Double) = (t * f).roundToInt()
        return listOf(
            PaceZone("Восстановление", sec(1.35), sec(1.55), "легко, после тяжёлых дней"),
            PaceZone("Аэробная", sec(1.20), sec(1.35), "длинные и лёгкие пробежки"),
            PaceZone("Темповая", sec(1.10), sec(1.18), "устойчивый темп"),
            PaceZone("Порог", sec(0.97), sec(1.03), "ПАНО, 20–40 мин суммарно"),
            PaceZone("VO2max", sec(0.90), sec(0.96), "интервалы 2–5 мин"),
        )
    }

    fun daysUntil(date: LocalDate, today: LocalDate = LocalDate.now()): Long =
        ChronoUnit.DAYS.between(today, date)

    fun weekStart(date: LocalDate): LocalDate = date.with(DayOfWeek.MONDAY)
}
