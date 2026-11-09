package com.zyphra.browser.github

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * GitHub API service for fetching release information.
 */
interface GithubApiService {
    
    @GET("repos/palashsuryavanshi/zyphra_browser/releases")
    fun getReleases(): Call<List<Release>>
    
    @GET("repos/palashsuryavanshi/zyphra_browser/releases/latest")
    fun getLatestRelease(): Call<Release>
    
    companion object {
        private const val BASE_URL = "https://api.github.com/"
        
        fun create(): GithubApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(GithubApiService::class.java)
        }
    }
}

/**
 * GitHub Release data class
 */
data class Release(
    val id: Long,
    val tag_name: String?,
    val name: String,
    val body: String?,
    val published_at: String,
    val assets: List<Asset>,
    val prerelease: Boolean = false
)

/**
 * GitHub Release Asset data class
 */
data class Asset(
    val id: Long,
    val name: String,
    val size: Long,
    val download_count: Int,
    val browser_download_url: String,
    val content_type: String
)