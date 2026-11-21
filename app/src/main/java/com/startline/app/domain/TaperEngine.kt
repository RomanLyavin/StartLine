package com.startline.app.domain

import com.startline.app.data.Fmt
import com.startline.app.data.Race
import java.time.LocalDate

/**
 * Шаг плана подводки к соревнованию (тейпера).
 * volumePct — рекомендуемый процент от обычного тренировочного объёма.
 */
data class TaperStep(
    val phase: String,
    val fromDay: Long,
    val toDay: Long,
    val volumePct: Int,
    val intensity: String,
    val note: String,
)

data class TaperPlan(
    val race: Race?,
    val daysLeft: Long?,
    val current: TaperStep?,
    val steps: List<TaperStep>,
    val recommendation: String,
) {
    val hasRace: Boolean get() = race != null
}

/**
 * Классическая ступенчатая подводка: объём падает, интенсивность
 * сохраняется. Используется ближайший старт из календаря пользователя.
 */
object TaperEngine {
    private val steps = listOf(
        TaperStep(
            "База",
            21, Long.MAX_VALUE,
            100,
            "Обычная",
            "Тренируйтесь по плану, накопление нагрузки.",
        ),
        TaperStep(
            "Подводка 1",
            14, 20,
            80,
            "Обычная + темп",
            "Сократите объём на 20%, сохраните одну темповую сессию.",
        ),
        TaperStep(
            "Подводка 2",
            7, 13,
            60,
            "Пороговая",
            "Сократите объём на 40%, добавьте короткие отрезки у порога.",
        ),
        TaperStep(
            "Подводка 3",
            3, 6,
            40,
            "Соревновательный темп",
            "Сократите объём на 60%, одна тренировка в темпе старта.",
        ),
        TaperStep(
            "Стартовая неделя",
            1, 2,
            30,
            "Лёгкая + ускорения",
            "Только короткие ускорения, много отдыха, контроль самочувствия.",
        ),
        TaperStep(
            "День гонки",
            0, 0,
            0,
            "По плану старта",
            "Разминка, старт, заминка. Тренировочной нагрузки нет.",
        ),
    )

    fun plan(races: List<Race>, today: LocalDate = LocalDate.now()): TaperPlan {
        val upcoming = races.filter { !it.date.isBefore(today) }.minByOrNull { it.date }
            ?: return TaperPlan(
                race = null,
                daysLeft = null,
                current = null,
                steps = steps,
                recommendation = "Добавьте старт в календарь, чтобы получить план подводки.",
            )
        val days = java.time.temporal.ChronoUnit.DAYS.between(today, upcoming.date)
        val current = steps.firstOrNull { days in it.fromDay..it.toDay }
        val text = if (current == null) {
            "До старта ${days} дн. Следуйте обычному плану."
        } else {
            "${current.phase}: ${current.note}"
        }
        return TaperPlan(
            race = upcoming,
            daysLeft = days,
            current = current,
            steps = steps,
            recommendation = "До старта «${upcoming.name}» (${Fmt.dateFull(upcoming.date)}) — ${days} дн. $text",
        )
    }
}
