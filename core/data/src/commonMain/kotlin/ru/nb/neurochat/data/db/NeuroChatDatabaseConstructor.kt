package ru.nb.neurochat.data.db

import androidx.room.RoomDatabaseConstructor

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object NeuroChatDatabaseConstructor : RoomDatabaseConstructor<NeuroChatDatabase> {
    override fun initialize(): NeuroChatDatabase
}
