package com.example.myapplication.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.local.entity.Attendance

@Dao
interface AttendanceDao {
    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insert(attendance: Attendance): Long

    @Query("SELECT * FROM attendance_table ORDER BY timestamp DESC")
    fun getAllAttendances(): LiveData<List<Attendance>>

    @Query("SELECT * FROM attendance_table ORDER BY timestamp DESC LIMIT 1")
    fun getLastAttendance(): LiveData<Attendance?>

    @Query("SELECT * FROM attendance_table WHERE timestamp BETWEEN :start AND :end ORDER BY timestamp DESC")
    fun getAttendancesBetween(start: Long, end: Long): LiveData<List<Attendance>>

    @Query("SELECT * FROM attendance_table WHERE synced = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedAttendances(): List<Attendance>

    @Query("UPDATE attendance_table SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Int)

    @Query("UPDATE attendance_table SET synced = 1 WHERE externalId = :externalId")
    suspend fun markAsSyncedByExternalId(externalId: String)

    @Query("UPDATE attendance_table SET address = :address WHERE id = :id")
    suspend fun updateAddress(id: Int, address: String)

    @Query("UPDATE attendance_table SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetryCount(id: Int)

    @Query("UPDATE attendance_table SET serverId = :serverId WHERE id = :id")
    suspend fun updateServerId(id: Int, serverId: Int)

    @Query("SELECT * FROM attendance_table WHERE externalId = :externalId LIMIT 1")
    suspend fun getByExternalId(externalId: String): Attendance?

}