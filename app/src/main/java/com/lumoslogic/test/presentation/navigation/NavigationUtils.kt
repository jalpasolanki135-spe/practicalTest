package com.lumoslogic.test.presentation.navigation

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Utility class for encoding and decoding navigation arguments.
 * Prevents crashes when passing special characters through navigation.
 */
object NavigationUtils {

    private const val CHARSET = "UTF-8"

    /**
     * Encodes a string for safe navigation argument passing.
     *
     * @param value The string to encode
     * @return URL-encoded string
     */
    fun encode(value: String): String {
        return try {
            URLEncoder.encode(value, CHARSET)
                .replace("+", "%20") // Replace + with %20 for space
        } catch (e: Exception) {
            // Fallback: manually encode special characters
            value
                .replace("%", "%25")
                .replace("/", "%2F")
                .replace("?", "%3F")
                .replace("&", "%26")
                .replace("=", "%3D")
                .replace(" ", "%20")
        }
    }

    /**
     * Decodes a navigation argument string.
     *
     * @param value The encoded string
     * @return Decoded string
     */
    fun decode(value: String): String {
        return try {
            URLDecoder.decode(value, CHARSET)
        } catch (e: Exception) {
            // If decoding fails, return as-is
            value
        }
    }

    /**
     * Creates a safe navigation route with encoded parameters.
     *
     * @param baseRoute The base route (e.g., "detail")
     * @param params The parameters to encode
     * @return Safe navigation route string
     */
    fun createRoute(baseRoute: String, vararg params: String): String {
        val encodedParams = params.map { encode(it) }
        return "$baseRoute/${encodedParams.joinToString("/")}"
    }
}
