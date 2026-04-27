package cloud.meis.data.model

import com.google.gson.annotations.SerializedName

data class MovieDetailResponse(
    @SerializedName("adult")
    val adult: Boolean = false,
    @SerializedName("id")
    val id: Int,
    @SerializedName("title")
    val title: String,
    @SerializedName("original_title")
    val originalTitle: String,
    @SerializedName("overview")
    val overview: String,
    @SerializedName("backdrop_path")
    val backdropPath: String?,
    @SerializedName("poster_path")
    val posterPath: String?,
    @SerializedName("release_date")
    val releaseDate: String?,
    @SerializedName("original_language")
    val originalLanguage: String,
    @SerializedName("origin_country")
    val originCountry: List<String> = emptyList(),
    @SerializedName("belongs_to_collection")
    val belongsToCollection: MovieCollection? = null,
    @SerializedName("budget")
    val budget: Long = 0L,
    @SerializedName("revenue")
    val revenue: Long = 0L,
    @SerializedName("homepage")
    val homepage: String? = null,
    @SerializedName("imdb_id")
    val imdbId: String? = null,
    @SerializedName("video")
    val video: Boolean = false,
    @SerializedName("status")
    val status: String?,
    @SerializedName("runtime")
    val runtime: Int?,
    @SerializedName("tagline")
    val tagline: String?,
    @SerializedName("vote_average")
    val voteAverage: Double,
    @SerializedName("vote_count")
    val voteCount: Int,
    @SerializedName("popularity")
    val popularity: Double,
    @SerializedName("genres")
    val genres: List<Genre> = emptyList(),
    @SerializedName("production_companies")
    val productionCompanies: List<ProductionCompany> = emptyList(),
    @SerializedName("production_countries")
    val productionCountries: List<ProductionCountry> = emptyList(),
    @SerializedName("spoken_languages")
    val spokenLanguages: List<SpokenLanguage> = emptyList()
)

data class MovieCollection(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String,
    @SerializedName("poster_path")
    val posterPath: String?,
    @SerializedName("backdrop_path")
    val backdropPath: String?
)

data class Genre(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String
)

data class ProductionCountry(
    @SerializedName("iso_3166_1")
    val iso: String,
    @SerializedName("name")
    val name: String
)

data class ProductionCompany(
    @SerializedName("id")
    val id: Int,
    @SerializedName("logo_path")
    val logoPath: String?,
    @SerializedName("name")
    val name: String,
    @SerializedName("origin_country")
    val originCountry: String
)

data class SpokenLanguage(
    @SerializedName("english_name")
    val englishName: String,
    @SerializedName("iso_639_1")
    val iso: String,
    @SerializedName("name")
    val name: String
)

data class MovieCreditsResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("cast")
    val cast: List<CastMember> = emptyList(),
    @SerializedName("crew")
    val crew: List<CrewMember> = emptyList()
)

data class CastMember(
    @SerializedName("adult")
    val adult: Boolean = false,
    @SerializedName("gender")
    val gender: Int? = null,
    @SerializedName("id")
    val id: Int,
    @SerializedName("known_for_department")
    val knownForDepartment: String? = null,
    @SerializedName("name")
    val name: String,
    @SerializedName("original_name")
    val originalName: String? = null,
    @SerializedName("popularity")
    val popularity: Double = 0.0,
    @SerializedName("cast_id")
    val castId: Int? = null,
    @SerializedName("character")
    val character: String?,
    @SerializedName("credit_id")
    val creditId: String? = null,
    @SerializedName("order")
    val order: Int? = null,
    @SerializedName("profile_path")
    val profilePath: String?
)

data class CrewMember(
    @SerializedName("adult")
    val adult: Boolean = false,
    @SerializedName("gender")
    val gender: Int? = null,
    @SerializedName("id")
    val id: Int,
    @SerializedName("known_for_department")
    val knownForDepartment: String? = null,
    @SerializedName("name")
    val name: String,
    @SerializedName("original_name")
    val originalName: String? = null,
    @SerializedName("popularity")
    val popularity: Double = 0.0,
    @SerializedName("profile_path")
    val profilePath: String? = null,
    @SerializedName("credit_id")
    val creditId: String? = null,
    @SerializedName("department")
    val department: String? = null,
    @SerializedName("job")
    val job: String? = null
)

data class WatchProvidersResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("results")
    val results: Map<String, WatchProviderRegion> = emptyMap()
)

data class WatchProviderRegion(
    @SerializedName("link")
    val link: String? = null,
    @SerializedName("flatrate")
    val flatrate: List<WatchProvider> = emptyList(),
    @SerializedName("rent")
    val rent: List<WatchProvider> = emptyList(),
    @SerializedName("buy")
    val buy: List<WatchProvider> = emptyList(),
    @SerializedName("ads")
    val ads: List<WatchProvider> = emptyList()
)

data class WatchProvider(
    @SerializedName("logo_path")
    val logoPath: String? = null,
    @SerializedName("provider_id")
    val providerId: Int,
    @SerializedName("provider_name")
    val providerName: String,
    @SerializedName("display_priority")
    val displayPriority: Int = Int.MAX_VALUE
)

data class MovieVideosResponse(
    @SerializedName("id")
    val id: Int,
    @SerializedName("results")
    val results: List<MovieVideo> = emptyList()
)

data class MovieVideo(
    @SerializedName("iso_639_1")
    val iso6391: String,
    @SerializedName("iso_3166_1")
    val iso31661: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("key")
    val key: String,
    @SerializedName("site")
    val site: String,
    @SerializedName("size")
    val size: Int,
    @SerializedName("type")
    val type: String,
    @SerializedName("official")
    val official: Boolean,
    @SerializedName("published_at")
    val publishedAt: String,
    @SerializedName("id")
    val id: String
)
