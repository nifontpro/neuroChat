package ru.nb.neurochat.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface BranchDao {

    @Query("SELECT * FROM branches ORDER BY id ASC")
    suspend fun getAll(): List<BranchEntity>

    @Query("SELECT * FROM branches WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): BranchEntity?

    @Query("SELECT * FROM branches WHERE parentBranchId = :parentId")
    suspend fun findChildren(parentId: Long): List<BranchEntity>

    @Insert
    suspend fun insert(entity: BranchEntity): Long

    @Update
    suspend fun update(entity: BranchEntity)

    @Query("DELETE FROM branches WHERE id = :id")
    suspend fun deleteById(id: Long)
}
