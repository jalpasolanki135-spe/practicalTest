package com.lumoslogic.test.data.repository

import com.lumoslogic.test.data.local.database.AppDatabase
import com.lumoslogic.test.data.mapper.toDomain
import com.lumoslogic.test.data.mapper.toEntity
import com.lumoslogic.test.data.remote.api.PostApiService
import com.lumoslogic.test.domain.model.Post
import com.lumoslogic.test.domain.repository.PostRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PostRepositoryImpl(
    private val database: AppDatabase,
    private val api: PostApiService
) : PostRepository {

    private var currentPage = 1

    override fun observePosts(): Flow<List<Post>> {
        return database.postDao()
            .observePosts()
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun loadNextPage() {

        try {

            val response = api.getPosts(currentPage)

            database.postDao().insertPosts(
                response.map { it.toEntity() }
            )

            currentPage++

        } catch (e: Exception) {
            // offline case ignore
        }
    }
}