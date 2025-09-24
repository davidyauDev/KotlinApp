package com.example.myapplication.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy

@Dao
interface AttendanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attendance: Attendance)

    @Query("SELECT * FROM attendance_table ORDER BY timestamp DESC")
    fun getAllAttendances(): LiveData<List<Attendance>>

    @Query("SELECT * FROM attendance_table ORDER BY timestamp DESC LIMIT 1")
    fun getLastAttendance(): LiveData<Attendance?>

}