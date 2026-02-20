package com.lumoslogic.test.di

import android.content.Context
import androidx.room.Room
import com.lumoslogic.test.data.local.database.AppDatabase
import com.lumoslogic.test.data.remote.api.PostApiService
import com.lumoslogic.test.data.repository.PostRepositoryImpl
import com.lumoslogic.test.domain.repository.PostRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideApi(): PostApiService =
        Retrofit.Builder()
            .baseUrl("https://jsonplaceholder.typicode.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PostApiService::class.java)

    @Provides
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "posts_db"
        ).build()

    @Provides
    fun provideRepository(
        db: AppDatabase,
        api: PostApiService
    ): PostRepository =
        PostRepositoryImpl(db, api)
}