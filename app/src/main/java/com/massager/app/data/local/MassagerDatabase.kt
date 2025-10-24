package com.massager.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.massager.app.data.local.dao.DeviceDao
import com.massager.app.data.local.dao.MassagerDeviceDao
import com.massager.app.data.local.dao.MeasurementDao
import com.massager.app.data.local.dao.RecordDao
import com.massager.app.data.local.dao.ThingDao
import com.massager.app.data.local.dao.UserDao
import com.massager.app.data.local.entity.DeviceEntity
import com.massager.app.data.local.entity.MassagerDeviceEntity
import com.massager.app.data.local.entity.MeasurementEntity
import com.massager.app.data.local.entity.RecordEntity
import com.massager.app.data.local.entity.ThingEntity
import com.massager.app.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        DeviceEntity::class,
        ThingEntity::class,
        RecordEntity::class,
        MeasurementEntity::class,
        MassagerDeviceEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MassagerDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun deviceDao(): DeviceDao
    abstract fun thingDao(): ThingDao
    abstract fun recordDao(): RecordDao
    abstract fun measurementDao(): MeasurementDao
    abstract fun massagerDeviceDao(): MassagerDeviceDao
}
