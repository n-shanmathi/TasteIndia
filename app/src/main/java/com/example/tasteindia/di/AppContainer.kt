package com.example.tasteindia.di

import android.content.Context
import com.example.tasteindia.data.auth.AuthRepository
import com.example.tasteindia.data.auth.DefaultAuthRepository
import com.example.tasteindia.data.favorite.FirestoreFavoriteStore
import com.example.tasteindia.data.local.FavoriteMealDao
import com.example.tasteindia.data.local.SQLiteFavoriteMealDao
import com.example.tasteindia.data.remote.TheMealDbApi
import com.example.tasteindia.data.repository.DefaultRecipeRepository
import com.example.tasteindia.data.repository.RecipeRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

interface AppContainer {
    val recipeRepository: RecipeRepository
    val authRepository: AuthRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val baseUrl = "https://www.themealdb.com/api/json/v1/1/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val mealDbApi: TheMealDbApi by lazy {
        retrofit.create(TheMealDbApi::class.java)
    }

    private val favoriteDao: FavoriteMealDao by lazy {
        SQLiteFavoriteMealDao(context)
    }

    override val recipeRepository: RecipeRepository by lazy {
        DefaultRecipeRepository(
            api = mealDbApi,
            favoriteDao = favoriteDao,
            favoriteStore = FirestoreFavoriteStore(
                auth = FirebaseAuth.getInstance(),
                firestore = FirebaseFirestore.getInstance()
            )
        )
    }

    override val authRepository: AuthRepository by lazy {
        DefaultAuthRepository(context)
    }

    init {
        authRepository.observeAuthState { user ->
            recipeRepository.setAuthenticatedUser(user?.uid)
        }
    }
}
