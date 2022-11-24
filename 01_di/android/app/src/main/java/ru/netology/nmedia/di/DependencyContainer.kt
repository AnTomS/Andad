package ru.netology.nmedia.di

import android.content.Context
import androidx.room.Room
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.api.loggingInterceptor
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryImpl
import java.util.concurrent.TimeUnit


class DependencyContainer(
    private val context: Context
) {

    companion object {

        private const val BASE_URL = "http://10.0.2.2:9999/api/slow/"

        @Volatile
        private var instance: DependencyContainer? = null

        fun initApp(context: Context) {
            instance = DependencyContainer(context)
        }


        fun getInstance(): DependencyContainer {
            return instance!!
        }

    }


    val appAuth = AppAuth(context)

    private val authInterceptor = Interceptor { chain ->
        val request = appAuth.authStateFlow.value.token?.let {
            chain.request()
                .newBuilder()
                .addHeader("Authorization", it)
                .build()
        } ?: chain.request()

        chain.proceed(request)
    }

    private val okhttp = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor())
        .addInterceptor(authInterceptor)
        .build()


    private val retrofit = Retrofit.Builder()
        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl(BASE_URL)
        .client(okhttp)
        .build()


    private val appBd = Room.databaseBuilder(context, AppDb::class.java, "app.db")
        .fallbackToDestructiveMigration()
        .build()

    val apiService = retrofit.create<ApiService>()

    private val postDao = appBd.postDao()


    val repository: PostRepository = PostRepositoryImpl(
        postDao,
        apiService,
    )
}

