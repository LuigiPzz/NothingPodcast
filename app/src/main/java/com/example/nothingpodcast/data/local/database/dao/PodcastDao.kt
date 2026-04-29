package com.example.nothingpodcast.data.local.database.dao

import androidx.room.*
import com.example.nothingpodcast.data.local.database.entity.PodcastEntity
import com.example.nothingpodcast.data.local.database.entity.PodcastWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface PodcastDao {

    @Query("SELECT * FROM podcasts WHERE isSubscribed = 1 ORDER BY orderIndex ASC")
    fun getSubscribedPodcasts(): Flow<List<PodcastEntity>>

    @Query("""
        SELECT p.*, (SELECT COUNT(*) FROM episodes e WHERE e.podcastId = p.id AND e.isPlayed = 0) as unplayedCount
        FROM podcasts p
        WHERE p.isSubscribed = 1
        ORDER BY p.orderIndex ASC
    """)
    fun getSubscribedPodcastsWithCount(): Flow<List<PodcastWithCount>>

    @Query("SELECT * FROM podcasts WHERE id = :id")
    suspend fun getPodcastById(id: String): PodcastEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPodcast(podcast: PodcastEntity)

    @Update
    suspend fun updatePodcast(podcast: PodcastEntity)

    @Query("UPDATE podcasts SET isSubscribed = :subscribed WHERE id = :id")
    suspend fun updateSubscriptionStatus(id: String, subscribed: Boolean)

    @Query("UPDATE podcasts SET lastUpdated = :timestamp WHERE id = :id")
    suspend fun updateLastRefreshed(id: String, timestamp: Long)

    @Query("UPDATE podcasts SET orderIndex = :orderIndex WHERE id = :id")
    suspend fun updateOrderIndex(id: String, orderIndex: Int)

    @Query("SELECT MAX(orderIndex) FROM podcasts")
    suspend fun getMaxOrderIndex(): Int?

    @Query("DELETE FROM podcasts WHERE id = :id")
    suspend fun deletePodcastById(id: String)
}
