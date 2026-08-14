package com.appotato.shared.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

public actual fun databaseModule(): Module = module {
    single { databaseBuilder(androidContext()).buildWith(get()) }
    single { get<AppotatoDatabase>().pantryItemDao() }
}

private fun databaseBuilder(context: Context): RoomDatabase.Builder<AppotatoDatabase> =
    Room.databaseBuilder<AppotatoDatabase>(
        context = context.applicationContext,
        name = context.getDatabasePath(DATABASE_FILE_NAME).absolutePath
    )
