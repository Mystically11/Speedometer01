package com.mystically.speedometer01.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface SessionDataDao {

    @Insert
    suspend fun insertSession(session: SessionData)

    @Query("SELECT * FROM session_data ORDER BY id DESC")
    suspend fun getAllSessions(): List<SessionData>

    @Query("DELETE FROM session_data")
    suspend fun deleteAllSessions()

    @Delete
    suspend fun deleteSession(session: SessionData)
}