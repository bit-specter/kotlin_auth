package cloud.meis.data.model

import com.google.gson.annotations.SerializedName

data class MovieResponse(
    @SerializedName("page")
    val page: Int = 0,
    @SerializedName("results")
    val results: List<Movie> = emptyList(),
    @SerializedName("total_pages")
    val totalPages: Int = 0,
    @SerializedName("total_results")
    val totalResults: Int = 0
)

data class Movie(
    @SerializedName("adult")
    val adult: Boolean = true,
    @SerializedName("genre_ids")
    val genreIds: List<Int> = emptyList(),
    @SerializedName("id")
    val id: Int,
    @SerializedName("original_title")
    val originalTitle: String = "",
    @SerializedName("title")
    val title: String,
    @SerializedName("overview")
    val overview: String,
    @SerializedName("poster_path")
    val posterPath: String?,
    @SerializedName("backdrop_path")
    val backdropPath: String?,
    @SerializedName("release_date")
    val releaseDate: String?,
    @SerializedName("video")
    val video: Boolean = false,
    @SerializedName("vote_average")
    val voteAverage: Double,
    @SerializedName("vote_count")
    val voteCount: Int = 0,
    @SerializedName("original_language")
    val originalLanguage: String,
    @SerializedName("popularity")
    val popularity: Double
)
