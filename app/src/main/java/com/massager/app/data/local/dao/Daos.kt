package com.massager.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.massager.app.data.local.entity.DeviceEntity
import com.massager.app.data.local.entity.MassagerDeviceEntity
import com.massager.app.data.local.entity.MeasurementEntity
import com.massager.app.data.local.entity.RecordEntity
import com.massager.app.data.local.entity.ThingEntity
import com.massager.app.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users LIMIT 1")
    fun currentUser(): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun clear()
}

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices")
    fun getDevices(): Flow<List<DeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(devices: List<DeviceEntity>)
}

@Dao
interface ThingDao {
    @Query("SELECT * FROM things WHERE deviceId = :deviceId")
    fun getThings(deviceId: String): Flow<List<ThingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(things: List<ThingEntity>)
}

@Dao
interface RecordDao {
    @Query("SELECT * FROM records ORDER BY createdAt DESC")
    fun getRecords(): Flow<List<RecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(records: List<RecordEntity>)
}

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM measurements ORDER BY recordedAt DESC")
    fun getMeasurements(): Flow<List<MeasurementEntity>>

    @Query("SELECT * FROM measurements WHERE deviceId = :deviceId ORDER BY recordedAt DESC")
    fun getMeasurementsForDevice(deviceId: String): Flow<List<MeasurementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(measurements: List<MeasurementEntity>)

    @Query("DELETE FROM measurements WHERE deviceId = :deviceId")
    suspend fun deleteForDevice(deviceId: String)
}

@Dao
interface MassagerDeviceDao {
    @Query("SELECT * FROM massager_devices")
    fun getMassagerDevices(): Flow<List<MassagerDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(device: MassagerDeviceEntity)
}
