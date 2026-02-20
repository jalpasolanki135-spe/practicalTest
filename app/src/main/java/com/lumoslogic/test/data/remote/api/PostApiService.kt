package com.lumoslogic.test.data.remote.api

import com.lumoslogic.test.data.remote.dto.PostDto
import retrofit2.http.GET
import retrofit2.http.Query

interface PostApiService {

    @GET("posts")
    suspend fun getPosts(
        @Query("_page") page: Int,
        @Query("_limit") limit: Int = 20
    ): List<PostDto>
}