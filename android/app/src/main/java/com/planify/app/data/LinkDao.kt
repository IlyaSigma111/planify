package com.planify.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkDao {
    @Query("SELECT * FROM links")
    fun getAllLinks(): Flow<List<Link>>

    @Query("SELECT * FROM links WHERE sourceId = :nodeId AND sourceType = :nodeType")
    fun getLinksFrom(nodeId: Long, nodeType: LinkNodeType): Flow<List<Link>>

    @Query("SELECT * FROM links WHERE targetId = :nodeId AND targetType = :nodeType")
    fun getLinksTo(nodeId: Long, nodeType: LinkNodeType): Flow<List<Link>>

    @Query("SELECT * FROM links WHERE (sourceId = :nodeId AND sourceType = :nodeType) OR (targetId = :nodeId AND targetType = :nodeType)")
    fun getAllLinksFor(nodeId: Long, nodeType: LinkNodeType): Flow<List<Link>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(link: Link): Long

    @Delete
    suspend fun delete(link: Link)

    @Query("DELETE FROM links WHERE (sourceId = :nodeId AND sourceType = :nodeType) OR (targetId = :nodeId AND targetType = :nodeType)")
    suspend fun deleteAllLinksFor(nodeId: Long, nodeType: LinkNodeType)
}
