package com.lumoslogic.test.domain.repository

import com.lumoslogic.test.domain.model.Post
import kotlinx.coroutines.flow.Flow

interface PostRepository {

    fun observePosts(): Flow<List<Post>>

    suspend fun loadNextPage()
}