package com.lumoslogic.test.data.mapper

import com.lumoslogic.test.data.local.entity.PostEntity
import com.lumoslogic.test.data.remote.dto.PostDto
import com.lumoslogic.test.domain.model.Post

fun PostDto.toEntity() = PostEntity(id, title, body)

fun PostEntity.toDomain() = Post(id, title, body)