package com.massager.app.data.local

// 文件说明：Room 数据库入口，注册实体与版本配置。
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
    version = 5,
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
