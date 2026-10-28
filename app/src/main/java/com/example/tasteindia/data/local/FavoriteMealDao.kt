package com.example.tasteindia.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteMealDao {
    fun setUserId(userId: String?) = Unit

    @Query("SELECT * FROM favorite_meals ORDER BY addedAt DESC")
    fun getAllFavoriteEntitiesFlow(): Flow<List<FavoriteMealEntity>>

    @Query("SELECT idMeal FROM favorite_meals")
    fun getAllFavoriteIdsFlow(): Flow<List<String>>

    @Query("SELECT idMeal FROM favorite_meals")
    suspend fun getAllFavoriteIds(): List<String>

    @Query("SELECT * FROM favorite_meals WHERE idMeal = :idMeal LIMIT 1")
    suspend fun getFavoriteById(idMeal: String): FavoriteMealEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_meals WHERE idMeal = :idMeal)")
    suspend fun isFavorite(idMeal: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteMealEntity)

    @Query("DELETE FROM favorite_meals WHERE idMeal = :idMeal")
    suspend fun deleteFavoriteById(idMeal: String)
}
