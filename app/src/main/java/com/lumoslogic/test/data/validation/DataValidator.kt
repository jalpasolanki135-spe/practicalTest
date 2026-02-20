package com.lumoslogic.test.data.validation

import com.lumoslogic.test.data.remote.dto.PostDto

/**
 * Validates API response data before saving to database.
 * Prevents invalid or corrupted data from entering the local cache.
 */
object DataValidator {

    /**
     * Validates a single post DTO.
     *
     * @param post The post to validate
     * @return Validation result
     */
    fun validatePost(post: PostDto): ValidationResult {
        val errors = mutableListOf<String>()

        // ID validation
        if (post.id <= 0) {
            errors.add("Invalid post ID: ${post.id}")
        }

        // Title validation
        when {
            post.title.isBlank() -> errors.add("Post title is blank")
            post.title.length > 200 -> errors.add("Post title exceeds 200 characters")
            !post.title.isValidUtf8() -> errors.add("Post title contains invalid characters")
        }

        // Body validation
        when {
            post.body.isBlank() -> errors.add("Post body is blank")
            post.body.length > 10000 -> errors.add("Post body exceeds 10000 characters")
            !post.body.isValidUtf8() -> errors.add("Post body contains invalid characters")
        }

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(errors)
        }
    }

    /**
     * Validates a list of posts and filters out invalid ones.
     *
     * @param posts The list of posts to validate
     * @return Pair of (valid posts, list of validation errors)
     */
    fun validatePosts(posts: List<PostDto>): Pair<List<PostDto>, List<String>> {
        val validPosts = mutableListOf<PostDto>()
        val allErrors = mutableListOf<String>()

        posts.forEachIndexed { index, post ->
            when (val result = validatePost(post)) {
                is ValidationResult.Success -> validPosts.add(post)
                is ValidationResult.Failure -> {
                    allErrors.addAll(
                        result.errors.map { "Post at index $index: $it" }
                    )
                }
            }
        }

        return Pair(validPosts, allErrors)
    }

    /**
     * Checks if a string contains valid UTF-8 characters.
     */
    private fun String.isValidUtf8(): Boolean {
        return try {
            // Try encoding and decoding to verify UTF-8 validity
            val bytes = this.toByteArray(Charsets.UTF_8)
            val decoded = String(bytes, Charsets.UTF_8)
            this == decoded
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Represents the result of a validation operation.
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Failure(val errors: List<String>) : ValidationResult()
}
