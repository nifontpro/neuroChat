package ru.nb.neurochat.data.di

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import org.koin.dsl.module
import ru.nb.neurochat.data.db.DatabaseFactory
import ru.nb.neurochat.data.db.NeuroChatDatabase
import ru.nb.neurochat.data.db.RoomChatHistoryDataSource
import ru.nb.neurochat.data.network.ChatRepository
import ru.nb.neurochat.data.network.OpenAiClient
import ru.nb.neurochat.data.preferences.UserSettingsStorage
import ru.nb.neurochat.domain.datasource.IChatHistoryDataSource
import ru.nb.neurochat.domain.model.ApiSettings
import ru.nb.neurochat.domain.repository.IChatRepository

fun dataModule(settings: ApiSettings) = module {
    single { OpenAiClient(settings) }
    single<IChatRepository> { ChatRepository(get()) }
    single { UserSettingsStorage(get()) }
    single<NeuroChatDatabase> {
        // ВАЖНО (миграции Room):
        // — exportSchema = true (включён в RoomConventionPlugin), схемы лежат в core/data/schemas/.
        // — При изменении ChatMessageEntity нужно поднять @Database(version = N+1) и добавить Migration
        //   через .addMigrations(...) перед .build(). До тех пор fallbackToDestructiveMigration
        //   стирает локальную историю при несовпадении версии — приемлемо для текущего этапа,
        //   когда данные ещё не критичны.
        get<DatabaseFactory>()
            .create()
            .setDriver(BundledSQLiteDriver())
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    single { get<NeuroChatDatabase>().chatMessageDao() }
    single { get<NeuroChatDatabase>().branchDao() }
    single<IChatHistoryDataSource> { RoomChatHistoryDataSource(get(), get()) }
}
