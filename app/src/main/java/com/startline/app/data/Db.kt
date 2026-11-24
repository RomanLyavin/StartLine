package com.startline.app.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.time.LocalDate

class Db(context: Context) : SQLiteOpenHelper(context, "startline.db", null, 4) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE users(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                email TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                salt TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE profiles(
                user_id INTEGER PRIMARY KEY,
                name TEXT,
                height_cm INTEGER,
                weight_kg REAL,
                age INTEGER,
                sex TEXT,
                hr_max_run INTEGER,
                hr_max_bike INTEGER,
                hr_rest INTEGER,
                lthr_run INTEGER,
                lthr_bike INTEGER,
                threshold_pace_sec INTEGER,
                ftp_watts INTEGER,
                intervals_id TEXT,
                intervals_key TEXT,
                training_days INTEGER NOT NULL DEFAULT 6,
                long_run_day INTEGER NOT NULL DEFAULT 6,
                high_intensity_day INTEGER NOT NULL DEFAULT 2,
                strength_day INTEGER NOT NULL DEFAULT 4,
                strength_every_weeks INTEGER NOT NULL DEFAULT 1,
                ai_base_url TEXT,
                ai_model TEXT,
                ai_key TEXT,
                updated_at INTEGER
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE activities(
                id TEXT PRIMARY KEY,
                user_id INTEGER NOT NULL,
                sport TEXT NOT NULL,
                name TEXT,
                date TEXT NOT NULL,
                start_local TEXT,
                start_epoch INTEGER,
                distance_m REAL,
                moving_sec INTEGER,
                elapsed_sec INTEGER,
                load REAL,
                avg_hr INTEGER,
                max_hr INTEGER,
                avg_speed REAL,
                elevation_m REAL,
                calories INTEGER,
                avg_power INTEGER,
                norm_power INTEGER,
                ctl REAL,
                atl REAL,
                is_race INTEGER NOT NULL DEFAULT 0,
                source TEXT NOT NULL DEFAULT 'intervals'
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_activities_user_date ON activities(user_id, date)")
        db.execSQL(
            """
            CREATE TABLE wellness(
                user_id INTEGER NOT NULL,
                date TEXT NOT NULL,
                ctl REAL,
                atl REAL,
                ramp_rate REAL,
                resting_hr INTEGER,
                weight_kg REAL,
                sleep_secs INTEGER,
                sleep_score REAL,
                PRIMARY KEY(user_id, date)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE races(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                sport TEXT NOT NULL,
                name TEXT NOT NULL,
                date TEXT NOT NULL,
                priority TEXT NOT NULL,
                distance_km REAL,
                target_sec INTEGER,
                notes TEXT
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE workouts(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                sport TEXT NOT NULL,
                date TEXT NOT NULL,
                type TEXT NOT NULL,
                title TEXT NOT NULL,
                description TEXT,
                duration_min INTEGER,
                target TEXT,
                load REAL,
                phase TEXT
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_workouts_user_date ON workouts(user_id, date)")
        db.execSQL(
            """
            CREATE TABLE ai_analysis(
                user_id INTEGER PRIMARY KEY,
                created_at INTEGER NOT NULL,
                content TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE profiles ADD COLUMN training_days INTEGER NOT NULL DEFAULT 6")
            db.execSQL("ALTER TABLE profiles ADD COLUMN long_run_day INTEGER NOT NULL DEFAULT 6")
            db.execSQL("ALTER TABLE profiles ADD COLUMN high_intensity_day INTEGER NOT NULL DEFAULT 2")
            db.execSQL("ALTER TABLE profiles ADD COLUMN strength_day INTEGER NOT NULL DEFAULT 4")
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE activities ADD COLUMN is_race INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE activities ADD COLUMN source TEXT NOT NULL DEFAULT 'intervals'")
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE profiles ADD COLUMN strength_every_weeks INTEGER NOT NULL DEFAULT 1")
        }
    }

    // ---------- users ----------

    fun createUser(email: String, hash: String, salt: String): User? {
        val values = ContentValues().apply {
            put("email", email)
            put("password_hash", hash)
            put("salt", salt)
            put("created_at", System.currentTimeMillis())
        }
        val id = writableDatabase.insert("users", null, values)
        return if (id > 0) User(id, email) else null
    }

    fun findUser(email: String): UserRecord? {
        readableDatabase.query(
            "users", arrayOf("id", "email", "password_hash", "salt"),
            "email = ?", arrayOf(email), null, null, null
        ).use { c ->
            if (!c.moveToFirst()) return null
            return UserRecord(
                user = User(c.getLong(0), c.getString(1)),
                passwordHash = c.getString(2),
                salt = c.getString(3),
            )
        }
    }

    fun userById(id: Long): User? {
        readableDatabase.query("users", arrayOf("id", "email"), "id = ?", arrayOf(id.toString()), null, null, null)
            .use { c ->
                if (!c.moveToFirst()) return null
                return User(c.getLong(0), c.getString(1))
            }
    }

    // ---------- profile ----------

    fun profile(userId: Long): Profile? {
        readableDatabase.query("profiles", null, "user_id = ?", arrayOf(userId.toString()), null, null, null)
            .use { c ->
                if (!c.moveToFirst()) return null
                return Profile(
                    userId = userId,
                    name = c.str("name") ?: "",
                    heightCm = c.intOrNull("height_cm"),
                    weightKg = c.doubleOrNull("weight_kg"),
                    age = c.intOrNull("age"),
                    sex = c.str("sex") ?: "M",
                    hrMaxRun = c.intOrNull("hr_max_run"),
                    hrMaxBike = c.intOrNull("hr_max_bike"),
                    hrRest = c.intOrNull("hr_rest"),
                    lthrRun = c.intOrNull("lthr_run"),
                    lthrBike = c.intOrNull("lthr_bike"),
                    thresholdPaceSec = c.intOrNull("threshold_pace_sec"),
                    ftpWatts = c.intOrNull("ftp_watts"),
                    intervalsId = c.str("intervals_id") ?: "0",
                    intervalsKey = c.str("intervals_key") ?: "",
                    trainingDays = (c.intOrNull("training_days") ?: 6).coerceIn(1, 7),
                    longRunDay = (c.intOrNull("long_run_day") ?: 6).coerceIn(1, 7),
                    highIntensityDay = (c.intOrNull("high_intensity_day") ?: 2).coerceIn(1, 7),
                    strengthDay = (c.intOrNull("strength_day") ?: 4).coerceIn(1, 7),
                    strengthEveryWeeks = (c.intOrNull("strength_every_weeks") ?: 1).coerceIn(1, 4),
                    aiBaseUrl = c.str("ai_base_url") ?: "https://openrouter.ai/api/v1",
                    aiModel = c.str("ai_model") ?: "deepseek/deepseek-chat-v3-0324:free",
                    aiKey = c.str("ai_key") ?: "",
                )
            }
    }

    fun saveProfile(p: Profile) {
        val values = ContentValues().apply {
            put("user_id", p.userId)
            put("name", p.name)
            put("height_cm", p.heightCm)
            put("weight_kg", p.weightKg)
            put("age", p.age)
            put("sex", p.sex)
            put("hr_max_run", p.hrMaxRun)
            put("hr_max_bike", p.hrMaxBike)
            put("hr_rest", p.hrRest)
            put("lthr_run", p.lthrRun)
            put("lthr_bike", p.lthrBike)
            put("threshold_pace_sec", p.thresholdPaceSec)
            put("ftp_watts", p.ftpWatts)
            put("intervals_id", p.intervalsId)
            put("intervals_key", p.intervalsKey)
            put("training_days", p.trainingDays.coerceIn(1, 7))
            put("long_run_day", p.longRunDay.coerceIn(1, 7))
            put("high_intensity_day", p.highIntensityDay.coerceIn(1, 7))
            put("strength_day", p.strengthDay.coerceIn(1, 7))
            put("strength_every_weeks", p.strengthEveryWeeks.coerceIn(1, 4))
            put("ai_base_url", p.aiBaseUrl)
            put("ai_model", p.aiModel)
            put("ai_key", p.aiKey)
            put("updated_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict("profiles", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // ---------- activities ----------

    fun upsertActivities(activities: List<Activity>) {
        if (activities.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (a in activities) {
                db.insertWithOnConflict("activities", null, activityValues(a), SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun activityValues(a: Activity): ContentValues = ContentValues().apply {
        put("id", a.id)
        put("user_id", a.userId)
        put("sport", a.sport.key)
        put("name", a.name)
        put("date", a.date.toString())
        put("start_local", a.startLocal)
        put("start_epoch", a.startEpoch)
        put("distance_m", a.distanceM)
        put("moving_sec", a.movingSec)
        put("elapsed_sec", a.elapsedSec)
        put("load", a.load)
        put("avg_hr", a.avgHr)
        put("max_hr", a.maxHr)
        put("avg_speed", a.avgSpeed)
        put("elevation_m", a.elevationM)
        put("calories", a.calories)
        put("avg_power", a.avgPower)
        put("norm_power", a.normPower)
        put("ctl", a.ctl)
        put("atl", a.atl)
        put("is_race", if (a.isRace) 1 else 0)
        put("source", a.source)
    }

    fun activities(userId: Long, sport: Sport? = null, limit: Int = 400): List<Activity> {
        val where = if (sport == null) "user_id = ?" else "user_id = ? AND sport = ?"
        val args = if (sport == null) arrayOf(userId.toString()) else arrayOf(userId.toString(), sport.key)
        val result = ArrayList<Activity>()
        readableDatabase.query(
            "activities", null, where, args, null, null, "date DESC, start_epoch DESC", limit.toString()
        ).use { c ->
            while (c.moveToNext()) {
                val sportValue = Sport.fromKey(c.str("sport")) ?: continue
                result.add(
                    Activity(
                        id = c.str("id") ?: continue,
                        userId = userId,
                        sport = sportValue,
                        name = c.str("name") ?: "",
                        date = LocalDate.parse(c.str("date")),
                        startLocal = c.str("start_local") ?: "",
                        startEpoch = c.longOrZero("start_epoch"),
                        distanceM = c.doubleOrNull("distance_m") ?: 0.0,
                        movingSec = c.intOrNull("moving_sec") ?: 0,
                        elapsedSec = c.intOrNull("elapsed_sec") ?: 0,
                        load = c.doubleOrNull("load") ?: 0.0,
                        avgHr = c.intOrNull("avg_hr"),
                        maxHr = c.intOrNull("max_hr"),
                        avgSpeed = c.doubleOrNull("avg_speed"),
                        elevationM = c.doubleOrNull("elevation_m"),
                        calories = c.intOrNull("calories"),
                        avgPower = c.intOrNull("avg_power"),
                        normPower = c.intOrNull("norm_power"),
                        ctl = c.doubleOrNull("ctl"),
                        atl = c.doubleOrNull("atl"),
                        isRace = c.intOrNull("is_race") == 1,
                        source = c.str("source") ?: "intervals",
                    )
                )
            }
        }
        return result
    }

    fun activity(userId: Long, id: String): Activity? =
        activities(userId, null, 2000).firstOrNull { it.id == id }

    fun countActivities(userId: Long): Int {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM activities WHERE user_id = ?", arrayOf(userId.toString())).use {
            return if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    // ---------- wellness ----------

    fun upsertWellness(days: List<WellnessDay>, userId: Long) {
        if (days.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (d in days) {
                val values = ContentValues().apply {
                    put("user_id", userId)
                    put("date", d.date.toString())
                    put("ctl", d.ctl)
                    put("atl", d.atl)
                    put("ramp_rate", d.rampRate)
                    put("resting_hr", d.restingHr)
                    put("weight_kg", d.weightKg)
                    put("sleep_secs", d.sleepSecs)
                    put("sleep_score", d.sleepScore)
                }
                db.insertWithOnConflict("wellness", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun wellness(userId: Long, limit: Int = 120): List<WellnessDay> {
        val result = ArrayList<WellnessDay>()
        readableDatabase.query(
            "wellness", null, "user_id = ?", arrayOf(userId.toString()), null, null, "date DESC", limit.toString()
        ).use { c ->
            while (c.moveToNext()) {
                result.add(
                    WellnessDay(
                        date = LocalDate.parse(c.str("date")),
                        ctl = c.doubleOrNull("ctl"),
                        atl = c.doubleOrNull("atl"),
                        rampRate = c.doubleOrNull("ramp_rate"),
                        restingHr = c.intOrNull("resting_hr"),
                        weightKg = c.doubleOrNull("weight_kg"),
                        sleepSecs = c.intOrNull("sleep_secs"),
                        sleepScore = c.doubleOrNull("sleep_score"),
                    )
                )
            }
        }
        return result
    }

    // ---------- races ----------

    fun addRace(r: Race): Long {
        val values = ContentValues().apply {
            put("user_id", r.userId)
            put("sport", r.sport.key)
            put("name", r.name)
            put("date", r.date.toString())
            put("priority", r.priority.key)
            put("distance_km", r.distanceKm)
            put("target_sec", r.targetSec)
            put("notes", r.notes)
        }
        return writableDatabase.insert("races", null, values)
    }

    fun updateRace(r: Race) {
        val values = ContentValues().apply {
            put("sport", r.sport.key)
            put("name", r.name)
            put("date", r.date.toString())
            put("priority", r.priority.key)
            put("distance_km", r.distanceKm)
            put("target_sec", r.targetSec)
            put("notes", r.notes)
        }
        writableDatabase.update("races", values, "id = ? AND user_id = ?", arrayOf(r.id.toString(), r.userId.toString()))
    }

    fun races(userId: Long): List<Race> {
        val result = ArrayList<Race>()
        readableDatabase.query("races", null, "user_id = ?", arrayOf(userId.toString()), null, null, "date ASC")
            .use { c ->
                while (c.moveToNext()) {
                    val sport = Sport.fromKey(c.str("sport")) ?: continue
                    result.add(
                        Race(
                            id = c.getLong(c.getColumnIndexOrThrow("id")),
                            userId = userId,
                            sport = sport,
                            name = c.str("name") ?: "",
                            date = LocalDate.parse(c.str("date")),
                            priority = RacePriority.fromKey(c.str("priority")),
                            distanceKm = c.doubleOrNull("distance_km"),
                            targetSec = c.intOrNull("target_sec"),
                            notes = c.str("notes") ?: "",
                        )
                    )
                }
            }
        return result
    }

    fun deleteRace(userId: Long, id: Long) {
        writableDatabase.delete("races", "id = ? AND user_id = ?", arrayOf(id.toString(), userId.toString()))
    }

    // ---------- workouts ----------

    fun replaceWorkouts(userId: Long, list: List<Workout>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("workouts", "user_id = ?", arrayOf(userId.toString()))
            for (w in list) {
                val values = ContentValues().apply {
                    put("user_id", userId)
                    put("sport", w.sport.key)
                    put("date", w.date.toString())
                    put("type", w.type)
                    put("title", w.title)
                    put("description", w.description)
                    put("duration_min", w.durationMin)
                    put("target", w.target)
                    put("load", w.load)
                    put("phase", w.phase)
                }
                db.insert("workouts", null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun workouts(userId: Long): List<Workout> {
        val result = ArrayList<Workout>()
        readableDatabase.query(
            "workouts", null, "user_id = ?", arrayOf(userId.toString()), null, null, "date ASC, id ASC"
        ).use { c ->
            while (c.moveToNext()) {
                val sport = Sport.fromKey(c.str("sport")) ?: continue
                result.add(
                    Workout(
                        id = c.getLong(c.getColumnIndexOrThrow("id")),
                        userId = userId,
                        sport = sport,
                        date = LocalDate.parse(c.str("date")),
                        type = c.str("type") ?: "",
                        title = c.str("title") ?: "",
                        description = c.str("description") ?: "",
                        durationMin = c.intOrNull("duration_min") ?: 0,
                        target = c.str("target") ?: "",
                        load = c.doubleOrNull("load") ?: 0.0,
                        phase = c.str("phase") ?: "",
                    )
                )
            }
        }
        return result
    }

    // ---------- AI ----------

    fun saveAnalysis(userId: Long, content: String) {
        val values = ContentValues().apply {
            put("user_id", userId)
            put("created_at", System.currentTimeMillis())
            put("content", content)
        }
        writableDatabase.insertWithOnConflict("ai_analysis", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun analysis(userId: Long): Pair<Long, String>? {
        readableDatabase.query(
            "ai_analysis", arrayOf("created_at", "content"), "user_id = ?", arrayOf(userId.toString()), null, null, null
        ).use { c ->
            if (!c.moveToFirst()) return null
            return c.getLong(0) to c.getString(1)
        }
    }

    // ---------- cursor helpers ----------

    private fun android.database.Cursor.str(name: String): String? {
        val i = getColumnIndexOrThrow(name)
        return if (isNull(i)) null else getString(i)
    }

    private fun android.database.Cursor.intOrNull(name: String): Int? {
        val i = getColumnIndexOrThrow(name)
        return if (isNull(i)) null else getInt(i)
    }

    private fun android.database.Cursor.longOrZero(name: String): Long {
        val i = getColumnIndexOrThrow(name)
        return if (isNull(i)) 0L else getLong(i)
    }

    private fun android.database.Cursor.doubleOrNull(name: String): Double? {
        val i = getColumnIndexOrThrow(name)
        return if (isNull(i)) null else getDouble(i)
    }
}
