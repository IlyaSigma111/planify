package com.planify.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY CASE status WHEN 'TODO' THEN 0 WHEN 'IN_PROGRESS' THEN 1 ELSE 2 END, orderIndex")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY orderIndex")
    fun getTasksByStatus(status: TaskStatus): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): Task?

    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchTasks(query: String): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    @Query("UPDATE tasks SET status = :status, orderIndex = :orderIndex WHERE id = :taskId")
    suspend fun moveTask(taskId: Long, status: TaskStatus, orderIndex: Int)

    @Query("UPDATE tasks SET posX = :x, posY = :y WHERE id = :id")
    suspend fun updatePosition(id: Long, x: Float, y: Float)

    @Query("SELECT COALESCE(MAX(orderIndex), -1) + 1 FROM tasks WHERE status = :status")
    suspend fun getNextOrderIndex(status: TaskStatus): Int
}
