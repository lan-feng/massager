package com.massager.app.di

import android.content.Context
import androidx.room.Room
import com.massager.app.data.local.MassagerDatabase
import com.massager.app.data.local.dao.DeviceDao
import com.massager.app.data.local.dao.MassagerDeviceDao
import com.massager.app.data.local.dao.MeasurementDao
import com.massager.app.data.local.dao.RecordDao
import com.massager.app.data.local.dao.ThingDao
import com.massager.app.data.local.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): MassagerDatabase {
        return Room.databaseBuilder(
            context,
            MassagerDatabase::class.java,
            "massager.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideUserDao(database: MassagerDatabase): UserDao = database.userDao()

    @Provides
    fun provideDeviceDao(database: MassagerDatabase): DeviceDao = database.deviceDao()

    @Provides
    fun provideThingDao(database: MassagerDatabase): ThingDao = database.thingDao()

    @Provides
    fun provideRecordDao(database: MassagerDatabase): RecordDao = database.recordDao()

    @Provides
    fun provideMeasurementDao(database: MassagerDatabase): MeasurementDao = database.measurementDao()

    @Provides
    fun provideMassagerDeviceDao(database: MassagerDatabase): MassagerDeviceDao =
        database.massagerDeviceDao()
}
