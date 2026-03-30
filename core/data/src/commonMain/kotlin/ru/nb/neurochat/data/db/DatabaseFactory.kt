package ru.nb.neurochat.data.db

import androidx.room.RoomDatabase

expect class DatabaseFactory {
    fun create(): RoomDatabase.Builder<NeuroChatDatabase>
}
