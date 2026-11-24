package com.startline.app.data

/**
 * Общий признак «гонка». intervals.icu не имеет отдельного флага гонки в
 * публичном API, поэтому старт определяется по типу активности и названию.
 * Поддерживаются английские и русские обозначения стартов. Используется и
 * для тренировок, полученных от сервисов часов.
 */
object RaceFlags {

    private val raceTypes = setOf(
        "race", "competition", "match", "event",
        "гонка", "старт", "соревнование",
    )

    private val nameMarkers = listOf(
        "гонка", "старт", "соревнование", "забег", "марафон", "полумарафон",
        "race", "racing", "event", "competition", "marathon", "half marathon",
        "10k", "5k", "triathlon", "duathlon", "time trial", "crit", "xco", "cx ",
    )

    /**
     * @param type тип активности из API
     * @param name название активности
     * @param explicitFlag явный флаг гонки, если источник его передаёт
     */
    fun isRace(type: String?, name: String?, explicitFlag: Boolean = false): Boolean {
        if (explicitFlag) return true
        val t = type.orEmpty().trim().lowercase()
        if (t in raceTypes) return true
        val n = name.orEmpty().trim().lowercase()
        return nameMarkers.any { n.contains(it) }
    }
}
