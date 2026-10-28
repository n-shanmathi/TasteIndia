package com.example.tasteindia.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SQLiteFavoriteMealDao(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    "taste_india_db",
    null,
    2
), FavoriteMealDao {

    @Volatile
    private var userId: String? = null
    private val entitiesFlow = MutableStateFlow<List<FavoriteMealEntity>>(emptyList())
    private val idsFlow = MutableStateFlow<List<String>>(emptyList())

    init {
        notifyListeners()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS favorite_meals (
                userId TEXT NOT NULL,
                idMeal TEXT NOT NULL,
                strMeal TEXT NOT NULL,
                strMealThumb TEXT,
                strCategory TEXT,
                strArea TEXT,
                strInstructions TEXT,
                strTags TEXT,
                strYoutube TEXT,
                strSource TEXT,
                ingredientsJson TEXT,
                addedAt INTEGER NOT NULL,
                PRIMARY KEY (userId, idMeal)
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS favorite_meals")
        onCreate(db)
    }

    private fun notifyListeners() {
        val entities = queryAllEntitiesSync()
        entitiesFlow.value = entities
        idsFlow.value = entities.map { it.idMeal }
    }

    private fun queryAllEntitiesSync(): List<FavoriteMealEntity> {
        val db = readableDatabase
        val currentUserId = userId
        if (currentUserId == null) return emptyList()
        val cursor = db.rawQuery(
            "SELECT * FROM favorite_meals WHERE userId = ? ORDER BY addedAt DESC",
            arrayOf(currentUserId)
        )
        val list = mutableListOf<FavoriteMealEntity>()
        cursor.use { c ->
            val idMealIdx = c.getColumnIndex("idMeal")
            val strMealIdx = c.getColumnIndex("strMeal")
            val strMealThumbIdx = c.getColumnIndex("strMealThumb")
            val strCategoryIdx = c.getColumnIndex("strCategory")
            val strAreaIdx = c.getColumnIndex("strArea")
            val strInstructionsIdx = c.getColumnIndex("strInstructions")
            val strTagsIdx = c.getColumnIndex("strTags")
            val strYoutubeIdx = c.getColumnIndex("strYoutube")
            val strSourceIdx = c.getColumnIndex("strSource")
            val ingredientsJsonIdx = c.getColumnIndex("ingredientsJson")
            val addedAtIdx = c.getColumnIndex("addedAt")

            while (c.moveToNext()) {
                list.add(
                    FavoriteMealEntity(
                        idMeal = if (idMealIdx >= 0) c.getString(idMealIdx) else "",
                        strMeal = if (strMealIdx >= 0) c.getString(strMealIdx) else "",
                        strMealThumb = if (strMealThumbIdx >= 0 && !c.isNull(strMealThumbIdx)) c.getString(strMealThumbIdx) else null,
                        strCategory = if (strCategoryIdx >= 0 && !c.isNull(strCategoryIdx)) c.getString(strCategoryIdx) else null,
                        strArea = if (strAreaIdx >= 0 && !c.isNull(strAreaIdx)) c.getString(strAreaIdx) else null,
                        strInstructions = if (strInstructionsIdx >= 0 && !c.isNull(strInstructionsIdx)) c.getString(strInstructionsIdx) else null,
                        strTags = if (strTagsIdx >= 0 && !c.isNull(strTagsIdx)) c.getString(strTagsIdx) else null,
                        strYoutube = if (strYoutubeIdx >= 0 && !c.isNull(strYoutubeIdx)) c.getString(strYoutubeIdx) else null,
                        strSource = if (strSourceIdx >= 0 && !c.isNull(strSourceIdx)) c.getString(strSourceIdx) else null,
                        ingredientsJson = if (ingredientsJsonIdx >= 0 && !c.isNull(ingredientsJsonIdx)) c.getString(ingredientsJsonIdx) else null,
                        addedAt = if (addedAtIdx >= 0) c.getLong(addedAtIdx) else 0L
                    )
                )
            }
        }
        return list
    }

    override fun getAllFavoriteEntitiesFlow(): Flow<List<FavoriteMealEntity>> {
        return entitiesFlow.asStateFlow()
    }

    override fun getAllFavoriteIdsFlow(): Flow<List<String>> {
        return idsFlow.asStateFlow()
    }

    override suspend fun getAllFavoriteIds(): List<String> = withContext(Dispatchers.IO) {
        val currentUserId = userId ?: return@withContext emptyList()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT idMeal FROM favorite_meals WHERE userId = ?",
            arrayOf(currentUserId)
        )
        val ids = mutableListOf<String>()
        cursor.use { c ->
            while (c.moveToNext()) {
                ids.add(c.getString(0))
            }
        }
        ids
    }

    override suspend fun getFavoriteById(idMeal: String): FavoriteMealEntity? = withContext(Dispatchers.IO) {
        val currentUserId = userId ?: return@withContext null
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM favorite_meals WHERE userId = ? AND idMeal = ? LIMIT 1",
            arrayOf(currentUserId, idMeal)
        )
        cursor.use { c ->
            if (c.moveToFirst()) {
                val idMealIdx = c.getColumnIndex("idMeal")
                val strMealIdx = c.getColumnIndex("strMeal")
                val strMealThumbIdx = c.getColumnIndex("strMealThumb")
                val strCategoryIdx = c.getColumnIndex("strCategory")
                val strAreaIdx = c.getColumnIndex("strArea")
                val strInstructionsIdx = c.getColumnIndex("strInstructions")
                val strTagsIdx = c.getColumnIndex("strTags")
                val strYoutubeIdx = c.getColumnIndex("strYoutube")
                val strSourceIdx = c.getColumnIndex("strSource")
                val ingredientsJsonIdx = c.getColumnIndex("ingredientsJson")
                val addedAtIdx = c.getColumnIndex("addedAt")

                FavoriteMealEntity(
                    idMeal = if (idMealIdx >= 0) c.getString(idMealIdx) else idMeal,
                    strMeal = if (strMealIdx >= 0) c.getString(strMealIdx) else "",
                    strMealThumb = if (strMealThumbIdx >= 0 && !c.isNull(strMealThumbIdx)) c.getString(strMealThumbIdx) else null,
                    strCategory = if (strCategoryIdx >= 0 && !c.isNull(strCategoryIdx)) c.getString(strCategoryIdx) else null,
                    strArea = if (strAreaIdx >= 0 && !c.isNull(strAreaIdx)) c.getString(strAreaIdx) else null,
                    strInstructions = if (strInstructionsIdx >= 0 && !c.isNull(strInstructionsIdx)) c.getString(strInstructionsIdx) else null,
                    strTags = if (strTagsIdx >= 0 && !c.isNull(strTagsIdx)) c.getString(strTagsIdx) else null,
                    strYoutube = if (strYoutubeIdx >= 0 && !c.isNull(strYoutubeIdx)) c.getString(strYoutubeIdx) else null,
                    strSource = if (strSourceIdx >= 0 && !c.isNull(strSourceIdx)) c.getString(strSourceIdx) else null,
                    ingredientsJson = if (ingredientsJsonIdx >= 0 && !c.isNull(ingredientsJsonIdx)) c.getString(ingredientsJsonIdx) else null,
                    addedAt = if (addedAtIdx >= 0) c.getLong(addedAtIdx) else 0L
                )
            } else null
        }
    }

    override suspend fun isFavorite(idMeal: String): Boolean = withContext(Dispatchers.IO) {
        val currentUserId = userId ?: return@withContext false
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT 1 FROM favorite_meals WHERE userId = ? AND idMeal = ? LIMIT 1",
            arrayOf(currentUserId, idMeal)
        )
        cursor.use { c ->
            c.moveToFirst()
        }
    }

    override suspend fun insertFavorite(favorite: FavoriteMealEntity) = withContext(Dispatchers.IO) {
        val currentUserId = userId ?: return@withContext
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("userId", currentUserId)
            put("idMeal", favorite.idMeal)
            put("strMeal", favorite.strMeal)
            put("strMealThumb", favorite.strMealThumb)
            put("strCategory", favorite.strCategory)
            put("strArea", favorite.strArea)
            put("strInstructions", favorite.strInstructions)
            put("strTags", favorite.strTags)
            put("strYoutube", favorite.strYoutube)
            put("strSource", favorite.strSource)
            put("ingredientsJson", favorite.ingredientsJson)
            put("addedAt", favorite.addedAt)
        }
        db.insertWithOnConflict("favorite_meals", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        notifyListeners()
    }

    override suspend fun deleteFavoriteById(idMeal: String) = withContext(Dispatchers.IO) {
        val currentUserId = userId ?: return@withContext
        val db = writableDatabase
        db.delete(
            "favorite_meals",
            "userId = ? AND idMeal = ?",
            arrayOf(currentUserId, idMeal)
        )
        notifyListeners()
    }

    override fun setUserId(userId: String?) {
        this.userId = userId
        notifyListeners()
    }
}
