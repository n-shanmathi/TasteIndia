package com.example.tasteindia.data.favorite

import com.example.tasteindia.data.local.FavoriteMealDao
import com.example.tasteindia.data.local.FavoriteMealEntity
import com.example.tasteindia.domain.model.MealDetail
import com.example.tasteindia.domain.model.MealSummary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.tasks.await

interface FavoriteStore {
    val errorFlow: Flow<Throwable?>
    fun setUserId(userId: String?)
    fun getFavoriteIdsFlow(): Flow<Set<String>>
    fun getFavoriteMealsFlow(): Flow<List<MealSummary>>
    suspend fun isFavorite(mealId: String): Boolean
    suspend fun toggleFavorite(mealId: String, detail: MealDetail?, summary: MealSummary?): Boolean
}

class FirestoreFavoriteStore(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : FavoriteStore {
    private val userId = MutableStateFlow<String?>(auth.currentUser?.uid)
    private val errors = MutableStateFlow<Throwable?>(null)
    override val errorFlow: Flow<Throwable?> = errors

    override fun setUserId(userId: String?) {
        this.userId.value = userId
        errors.value = null
    }

    override fun getFavoriteIdsFlow(): Flow<Set<String>> {
        return favoriteDocumentsFlow().combine(errors) { documents, _ ->
            documents.map { it.id }.toSet()
        }
    }

    override fun getFavoriteMealsFlow(): Flow<List<MealSummary>> {
        return favoriteDocumentsFlow().combine(errors) { documents, _ ->
            documents.mapNotNull { document ->
                document.toObject(FavoriteCloudDocument::class.java)
                    ?.toMealSummary(document.id)
            }
        }
    }

    override suspend fun isFavorite(mealId: String): Boolean {
        val uid = requireUserId()
        return try {
            firestore.favoriteDocument(uid, mealId).get().await().exists()
        } catch (error: Exception) {
            errors.value = error
            throw error
        }
    }

    override suspend fun toggleFavorite(
        mealId: String,
        detail: MealDetail?,
        summary: MealSummary?
    ): Boolean {
        val uid = requireUserId()
        return try {
            val document = firestore.favoriteDocument(uid, mealId)
            if (document.get().await().exists()) {
                document.delete().await()
                false
            } else {
                val source = detail?.toCloudDocument() ?: summary?.toCloudDocument()
                    ?: throw IllegalArgumentException("Recipe data is unavailable for cloud save.")
                document.set(source).await()
                true
            }
        } catch (error: Exception) {
            errors.value = error
            throw error
        }
    }

    private fun favoriteDocumentsFlow(): Flow<List<com.google.firebase.firestore.DocumentSnapshot>> {
        return userId.flatMapLatest { uid -> callbackFlow {
            var registration: ListenerRegistration? = null
            if (uid == null) {
                trySend(emptyList())
                awaitClose()
                return@callbackFlow
            }
            registration = firestore.collection("users")
                .document(uid)
                .collection("favourites")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        errors.value = error
                        trySend(emptyList())
                    } else {
                        errors.value = null
                        trySend(snapshot?.documents.orEmpty())
                    }
                }
            awaitClose { registration?.remove() }
        } }
    }

    private fun requireUserId(): String {
        return userId.value ?: throw IllegalStateException(
            "You must be signed in to save favourites to the cloud."
        )
    }
}

class LocalFavoriteStore(
    private val dao: FavoriteMealDao
) : FavoriteStore {
    override val errorFlow: Flow<Throwable?> = MutableStateFlow(null)
    override fun setUserId(userId: String?) = dao.setUserId(userId)

    override fun getFavoriteIdsFlow(): Flow<Set<String>> =
        dao.getAllFavoriteIdsFlow().combine(MutableStateFlow(Unit)) { ids, _ -> ids.toSet() }

    override fun getFavoriteMealsFlow(): Flow<List<MealSummary>> =
        dao.getAllFavoriteEntitiesFlow().combine(MutableStateFlow(Unit)) { entities, _ ->
            entities.map { it.toMealSummary() }
        }

    override suspend fun isFavorite(mealId: String): Boolean = dao.isFavorite(mealId)

    override suspend fun toggleFavorite(
        mealId: String,
        detail: MealDetail?,
        summary: MealSummary?
    ): Boolean {
        if (dao.isFavorite(mealId)) {
            dao.deleteFavoriteById(mealId)
            return false
        }
        detail?.let {
            dao.insertFavorite(FavoriteMealEntity.fromMealDetail(it.copy(isFavorite = true)))
        } ?: summary?.let {
            dao.insertFavorite(FavoriteMealEntity.fromMealSummary(it))
        } ?: throw IllegalArgumentException("Recipe data is unavailable for local save.")
        return true
    }
}

private data class FavoriteCloudDocument(
    val strMeal: String = "",
    val strMealThumb: String? = null,
    val strCategory: String? = null,
    val strArea: String? = null,
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toMealSummary(idMeal: String) = MealSummary(
        idMeal = idMeal,
        strMeal = strMeal,
        strMealThumb = strMealThumb,
        strCategory = strCategory,
        strArea = strArea,
        isFavorite = true
    )
}

private fun MealDetail.toCloudDocument() = FavoriteCloudDocument(
    strMeal = strMeal,
    strMealThumb = strMealThumb,
    strCategory = strCategory,
    strArea = strArea
)

private fun MealSummary.toCloudDocument() = FavoriteCloudDocument(
    strMeal = strMeal,
    strMealThumb = strMealThumb,
    strCategory = strCategory,
    strArea = strArea
)

private fun FirebaseFirestore.favoriteDocument(uid: String, mealId: String) =
    collection("users").document(uid).collection("favourites").document(mealId)
