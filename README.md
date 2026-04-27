# Movie Gallery — App Documentation

## Table of Contents
1. [App Overview](#1-app-overview)
2. [Tech Stack](#2-tech-stack)
3. [Data Sources](#3-data-sources)
4. [Architecture](#4-architecture)
5. [Pages & Features](#5-pages--features)
   - [Login Page](#51-login-page)
   - [Home Page](#52-home-page)
   - [Movie Detail Page](#53-movie-detail-page)
6. [Data Models](#6-data-models)
7. [Animations & UX Details](#7-animations--ux-details)
8. [Color Palette](#8-color-palette)
9. [Dependencies](#9-dependencies)
10. [Sample Accounts](#10-sample-accounts)

---

## 1. App Overview

**Movie Gallery** is an Android movie discovery application that lets users browse currently popular movies, view rich detail pages for each film, and watch trailers directly inside the app. The app follows a clean login → home → detail navigation flow and is built with a mix of Jetpack Compose (for layout hosting) and traditional XML layouts rendered via `AndroidView`.

| Property | Value |
|---|---|
| **App Name** | Movie Gallery |
| **Package** | `cloud.meis` |
| **Min Android** | API 24 (Android 7.0 Nougat) |
| **Target Android** | API 36 |
| **Language** | Kotlin |
| **Version** | 1.0 |

---

## 2. Tech Stack

| Layer | Technology |
|---|---|
| UI | XML layouts + Jetpack Compose (`AndroidView`) |
| Architecture | MVVM (ViewModel + Repository + LiveData) |
| Async | Kotlin Coroutines + `lifecycleScope` |
| Networking | Retrofit 2.11 + OkHttp 4.12 |
| JSON Parsing | Gson (via Retrofit converter) |
| Image Loading | Coil 2.7 |
| Video Playback | Android `WebView` + YouTube iFrame API |
| Theming | Material Design 3 |
| Build | AGP 9.1 + Kotlin 2.3.20 |

---

## 3. Data Sources

### 3.1 Movie Data — The Movie Database (TMDB)

All movie content is fetched from **TMDB** (themoviedb.org) via their public API.

- **Base URL:** `https://api.themoviedb.org/3/`
- **Auth:** Bearer token (`TMDB_READ_ACCESS_TOKEN`) injected via OkHttp interceptor on every request
- **Image CDN:** `https://image.tmdb.org/t/p/{size}{path}`
  - Posters: `w500`
  - Backdrops / cast / provider logos: `w780` or `w185`

#### API Endpoints Used

| Endpoint | Purpose |
|---|---|
| `GET /movie/popular` | Fetch list of currently popular movies |
| `GET /movie/{id}` | Full movie details (metadata, genres, companies, etc.) |
| `GET /movie/{id}/credits` | Cast and crew |
| `GET /movie/{id}/watch/providers` | Streaming/rental availability by region |
| `GET /movie/{id}/videos` | Trailers and clips (YouTube keys) |

### 3.2 User Authentication — Local (Dummy)

Authentication is **local only** — no backend server. Credentials are hardcoded as a demo:

| Username | Password | Display Name |
|---|---|---|
| `rifky` | `123456` | Rifky Abdul Hanan |
| `jesslyn` | `654321` | Jesslyn Eklesia |

> Login validation is case-insensitive for usernames and exact-match for passwords.

### 3.3 Video Playback — YouTube

Trailers are sourced from TMDB's `/videos` endpoint which returns YouTube video keys. The app renders them inside a `WebView` using the YouTube iFrame Player API.

---

## 4. Architecture

```
┌─────────────────────────────────────────────────────┐
│                      UI Layer                        │
│  MainActivity  │  HomeActivity  │  MovieDetailActivity│
│      (XML via AndroidView in Compose setContent)     │
└────────────────────┬────────────────────────────────┘
                     │ observes / calls
┌────────────────────▼────────────────────────────────┐
│                  ViewModel Layer                     │
│              MovieViewModel                          │
│   LiveData: movies, isLoading, error                 │
└────────────────────┬────────────────────────────────┘
                     │ suspend calls
┌────────────────────▼────────────────────────────────┐
│                 Repository Layer                     │
│              MovieRepository                         │
│   Returns Result<T> (success / failure)             │
└────────────────────┬────────────────────────────────┘
                     │ HTTP
┌────────────────────▼────────────────────────────────┐
│                 Network Layer                        │
│  TmdbApiService (Retrofit)  +  NetworkModule         │
│  OkHttp with auth interceptor + logging interceptor  │
└─────────────────────────────────────────────────────┘
```

### Key Architectural Decisions

- **`AndroidView` in Compose** — Activities use `setContent { AndroidView { ... } }` so that XML layouts work within a Compose host. This allows the existing XML design system to be reused while the project slowly migrates toward full Compose.
- **Repository returns `Result<T>`** — Every API call is wrapped in `runCatching`, so the caller handles success/failure via `.onSuccess { }` / `.onFailure { }` without try-catch at the UI layer.
- **ViewModel protects against duplicate loads** — `loadPopularMovies()` returns early if `_isLoading.value == true`.

---

## 5. Pages & Features

---

### 5.1 Login Page

**File:** `MainActivity.kt` + `activity_login.xml`

The first screen users see. It handles credentials and transitions to the home screen on success.

#### Layout Structure

```
FrameLayout
├── Background layers (split gradient / light)
└── ScrollView
    └── Card Container (white, 12dp elevation)
        ├── "Movie Gallery" title (32sp bold, white)
        ├── "Welcome Back" heading
        ├── "Enter your details below" subtitle
        ├── Username label + EditText
        ├── Password label + FrameLayout
        │   ├── Password EditText (inputType=textPassword)
        │   └── Eye toggle ImageButton (top-right of field)
        ├── "Forgot your password?" link
        ├── Sign In button (purple, pill-shaped, 56dp)
        ├── "Or sign in with" divider
        └── Social row
            ├── Google button (icon + label)
            └── Apple button (icon + label)
```

#### Features

| Feature | Detail |
|---|---|
| **Username field** | `inputType="textPersonName"`, hint "Masukkan username" |
| **Password field** | `inputType="textPassword"` by default, 52dp right-padding to clear the eye icon |
| **Show/hide password** | Eye icon toggles between `ic_eye_off` (default) and `ic_eye_on`. Switches `inputType` between `TYPE_TEXT_VARIATION_PASSWORD` and `TYPE_TEXT_VARIATION_VISIBLE_PASSWORD`. Cursor position is preserved across the toggle. |
| **Sign In button** | Pill-shaped (28dp corner radius), brand purple with white ripple on press |
| **Login validation** | Case-insensitive username match + exact password match against `DummyUsers.list` |
| **Success flow** | Toast "Selamat Datang, {Name}!" → starts `HomeActivity` with `USER_NAME` extra → calls `finish()` |
| **Failure feedback** | Toast with error message + shake animation on the button (3-frame left-right, 40ms each) |
| **Entry animation** | Entire view slides up from +80px and fades in over 800ms |
| **Social buttons** | Google and Apple buttons show a "Coming Soon" toast — not yet implemented |
| **Forgot password** | Tappable text with no action currently wired |

#### Sample Login

```
Username : rifky
Password : 123456

Username : jesslyn
Password : 654321
```

---

### 5.2 Home Page

**Files:** `HomeActivity.kt` + `activity_home.xml` + `MovieViewModel.kt` + `MovieGridAdapter.kt` + `item_movie_grid.xml`

The main browsing screen. Shows a 2-column grid of currently popular movies fetched from TMDB.

#### Layout Structure

```
LinearLayout (vertical)
├── App Bar (gradient background)
│   ├── "Movie Gallery" title (left, 20sp bold white)
│   └── Logout button (right, ic_logout icon)
├── Subtitle bar
│   ├── tvSubtitle — "Hi, {UserName} 👋" (marquee, gray)
│   └── Tagline — "Temukan film populer hari ini"
└── FrameLayout (flex container)
    ├── RecyclerView (2-column grid)
    ├── ProgressBar (centered, visible during loading)
    ├── tvError (white card, tap-to-retry)
    └── tvEmpty (white card, no results message)
```

#### Features

| Feature | Detail |
|---|---|
| **Movie grid** | 2-column `GridLayoutManager` with `MovieGridAdapter` |
| **Welcome message** | Receives `USER_NAME` from login intent, displayed as "Hi, {Name} 👋" |
| **Data loading** | `MovieViewModel.loadPopularMovies(language="en-US", page=1)` called on `onCreate` |
| **Loading state** | Full-screen `ProgressBar` (centered) while movies are fetching |
| **Error state** | `tvError` ("Gagal memuat film. Tap untuk coba lagi.") replaces the grid; tapping retries the API call |
| **Empty state** | `tvEmpty` shown if API returns 0 results |
| **Status bar styling** | Window insets listener adjusts app bar top padding for edge-to-edge display |
| **Logout** | Tapping the logout icon starts `MainActivity` with `FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TASK`, finishing the home screen — user returns to login |
| **Movie tap** | Opens `MovieDetailActivity` with 9 extras: id, title, overview, releaseDate, language, popularity, rating, backdropPath, posterPath |
| **Grid animation** | Each card fades in and slides up (translationY 28→0, alpha 0→1, 380ms) with a staggered delay based on adapter position |

#### Movie Grid Card (item_movie_grid.xml)

Each card in the grid shows:

| Element | Detail |
|---|---|
| **Poster image** | `w500` TMDB image, 220dp height, rounded corners, centerCrop |
| **Rating chip** | Overlaid top-right of the poster. Format: `★ 7.4` (orange text on dark chip) |
| **Image progress** | 0→90% fake loading animation (900ms loop). Jumps to 100% and hides after Coil finishes loading |
| **Title** | Below poster, 14sp bold, max 2 lines |
| **Subtitle** | Year extracted from `releaseDate` + language code. Format: `2024 • EN` |

---

### 5.3 Movie Detail Page

**Files:** `MovieDetailActivity.kt` + `activity_movie_detail.xml` + `CastAdapter.kt` + `WatchProviderAdapter.kt` + `item_cast_member.xml` + `item_watch_provider.xml`

The richest screen in the app. Displays everything TMDB knows about a film in a scrollable detail view.

#### Layout Structure

```
ScrollView
└── LinearLayout
    ├── Backdrop section (320dp, FrameLayout)
    │   ├── ivBackdrop — hero image (centerCrop)
    │   ├── webViewPlayer — YouTube iFrame (hidden until play)
    │   ├── btnPlayTrailer — pulsing play button (center)
    │   ├── backdropLoadingOverlay — progress spinner + "X%"
    │   └── btnBack — back button (top-left, insets-aware)
    │
    └── Detail card (neg. margin -24dp, 6dp elevation)
        ├── Title (30sp bold)
        ├── Tagline (14sp italic gray)
        ├── Inline row: Rating chip + Runtime
        ├── Original title
        │
        ├── Metadata grid (label → value, 15 rows)
        │   Status, Genres, Release, Budget, Revenue, Votes,
        │   Origin, Collection, Companies, Homepage, IMDb,
        │   Countries, Spoken Languages, Language, Popularity
        │
        ├── Overview section
        │   └── Full plot summary (16sp, 4dp line spacing)
        │
        ├── Top Cast section
        │   └── Horizontal RecyclerView (200dp, top 20 actors)
        │
        ├── Crew Highlights section
        │   └── Bulleted list (up to 8 key crew, "• Job: Name")
        │
        ├── Watch Providers section
        │   ├── Region info + JustWatch link
        │   └── Vertical RecyclerView (flat, nested scroll off)
        │
        └── Videos section
            ├── tvVideosTitle (bold header, hidden if no videos)
            └── tvVideos (trailer name + YouTube URL, auto-link)
```

#### Features

##### Backdrop & Hero Image

| Feature | Detail |
|---|---|
| **Initial load** | Uses `backdropPath` or `posterPath` passed from the grid as a fast placeholder while the full detail API loads |
| **Full detail image** | Replaced with higher-quality backdrop from `getMovieDetail()` response |
| **Loading progress** | Animated `0→90%` loop (900ms) while Coil fetches; jumps to `100%` and disappears 120ms after load completes |
| **Parallax scroll** | `ivBackdrop.translationY = scrollY * 0.35f` — backdrop moves at 35% scroll speed, creating a depth effect |
| **Fade on scroll** | Backdrop fades from 100% → 55% opacity as user scrolls down (over first 500px) |
| **Back button** | Fixed top-left, adjusts its top margin using window insets so it clears the status bar on any device |

##### Trailer / Video Playback

| Feature | Detail |
|---|---|
| **Source** | `GET /movie/{id}/videos` — filters for YouTube only, validates 11-char key format |
| **Priority** | Official trailers sorted first, then by type "Trailer" |
| **Limit** | Up to 5 videos fetched; first one is auto-selected |
| **Play button** | Visible only when a trailer exists. Pulsing animation: scale 1→1.22→1 + alpha 1→0.75→1, 950ms infinite loop |
| **Tap to play** | Tapping the backdrop image OR the play button starts playback |
| **Playback method** | `WebView` loading YouTube iFrame API HTML via `loadDataWithBaseURL("https://www.youtube.com", ...)` |
| **Auto-hide on play** | Play button fades out (alpha 0, scale 0.7, 260ms) as soon as playback starts |
| **Error handling** | YouTube iFrame `onError` JS callback → `AndroidBridge.onPlayerError(code)` Java bridge → hides WebView, opens YouTube app (`vnd.youtube:{key}`), falls back to browser if app not installed |
| **Error 150/152** | Major studio trailers often disable embedding. App auto-detects this and redirects to YouTube app so the video always plays |
| **Videos section title** | "Videos" bold heading is only shown when videos are available |
| **Trailer link** | Also displayed as a clickable URL below the title for manual access |

##### Metadata Grid (15 fields)

Each row has a **120dp purple label** on the left and a **flex value** on the right:

| Label | Source | Example |
|---|---|---|
| Status | `detail.status` | Released |
| Genres | `detail.genres` joined | Action, Drama |
| Release | `detail.releaseDate` | 2024-05-01 |
| Budget | `detail.budget` (USD) | $200,000,000 |
| Revenue | `detail.revenue` (USD) | $1,450,000,000 |
| Votes | `detail.voteCount` | 4,321 |
| Origin | `detail.originCountry` | US |
| Collection | `detail.belongsToCollection` | Avengers Collection |
| Companies | `detail.productionCompanies` | Marvel Studios |
| Homepage | `detail.homepage` (auto-link) | https://... |
| IMDb | `detail.imdbId` | tt1234567 |
| Countries | `detail.productionCountries` | United States of America |
| Spoken Lang | `detail.spokenLanguages` | English, Spanish |
| Language | `detail.originalLanguage` | EN |
| Popularity | `detail.popularity` | 1234.5 |

> All fields fall back to `"-"` if not available from the API.

##### Top Cast (Horizontal RecyclerView)

- Up to **20 cast members**, sorted by `order` ascending (lead actors first)
- Each card (`item_cast_member.xml`):
  - **Profile photo** — 120×140dp, rounded, loaded from `w185` TMDB image
  - **Loading progress** — 0→90% animation (850ms loop) while photo loads
  - **Actor name** — 13sp bold, max 2 lines
  - **Character name** — 12sp gray, max 2 lines; shows "Unknown role" if blank
- If no cast data: `tvCastEmpty` shows "No cast information available."

##### Crew Highlights

- Up to **8 crew members** shown (filtered to those with a job title and non-blank name)
- Format: bulleted list `• Job: Name` (e.g., `• Director: James Cameron`)
- If no crew data: "No crew information available."

##### Watch Providers

- Source: `GET /movie/{id}/watch/providers`
- **Region priority:** Indonesia (`ID`) first, then US (`US`), then first available
- Flattens all provider types: `flatrate + rent + buy + ads`, deduped by `providerId`, sorted by `displayPriority`
- Shows region code and JustWatch deep link
- Each row (`item_watch_provider.xml`):
  - **Provider logo** — 64×64dp from `w185` TMDB image
  - **Provider name** — 14sp bold (e.g., Netflix, Disney+)
- If no providers: "No watch provider information available."

---

## 6. Data Models

### Movie (Grid item)

```kotlin
data class Movie(
    val id: Int,
    val title: String,
    val overview: String,
    val originalLanguage: String,
    val popularity: Double,
    val voteAverage: Double,
    val voteCount: Int,
    val releaseDate: String?,
    val posterPath: String?,
    val backdropPath: String?,
    val genreIds: List<Int>,
    val adult: Boolean,
    val video: Boolean,
    val originalTitle: String
)
```

### MovieDetail (Detail page, extends Movie fields)

```kotlin
// Additional fields in the detail response:
budget: Long                         // e.g. 200000000
revenue: Long                        // e.g. 1450000000
homepage: String?
imdbId: String?
status: String?                      // e.g. "Released"
runtime: Int?                        // minutes
tagline: String?
genres: List<Genre>                  // id + name
productionCompanies: List<ProductionCompany>
productionCountries: List<ProductionCountry>
spokenLanguages: List<SpokenLanguage>
originCountry: List<String>
belongsToCollection: MovieCollection?
```

### Credits

```kotlin
CastMember: id, name, character, order, profilePath, popularity, ...
CrewMember:  id, name, job, department, profilePath, ...
```

### Watch Provider

```kotlin
WatchProvider: providerId, providerName, logoPath, displayPriority
WatchProviderRegion: link, flatrate[], rent[], buy[], ads[]
```

### Video

```kotlin
MovieVideo: key (YouTube ID), name, type, site, official, publishedAt
// Only "YouTube" site videos with valid 11-char keys are used
```

### UserProfile (local auth)

```kotlin
data class UserProfile(
    val username: String,
    val name: String,
    val pass: String
)
```

---

## 7. Animations & UX Details

| Animation | Where | Detail |
|---|---|---|
| **Login entry** | Login screen | Full view: `translationY +80→0` + `alpha 0→1`, 800ms |
| **Login shake** | Login button (on fail) | `translationX +15 → -15 → 0`, 40ms per frame |
| **Grid card entry** | Home grid | Per-item: `translationY 28→0` + `alpha 0→1`, 380ms, staggered delay by position |
| **Image progress** | All image loads | `ValueAnimator(0→90)` at 900ms loop; completes to "100%" on load |
| **Backdrop parallax** | Detail scroll | `translationY = scrollY * 0.35f` |
| **Backdrop fade** | Detail scroll | Alpha `1.0 → 0.55` over first 500px of scroll |
| **Play button pulse** | Detail, when trailer available | `ObjectAnimator`: scaleX/Y `1→1.22→1`, alpha `1→0.75→1`, 950ms infinite |
| **Play button hide** | Detail, on play tap | `alpha 0, scaleX/Y 0.7`, 260ms, then `GONE` |
| **Scroll listener** | Detail screen | `ScrollView.setOnScrollChangeListener` drives parallax + fade |

---

## 8. Color Palette

| Name | Hex | Usage |
|---|---|---|
| `brand_primary` | `#4A3AFF` | Buttons, metadata labels, accents |
| `brand_gradient_end` | `#9D50BB` | Login header gradient end |
| `bg_light` | `#F5F7FA` | Screen backgrounds |
| `input_fill` | `#F8F9FB` | Input field fill |
| `text_main` | `#222222` | Body text, titles |
| `text_hint` | `#888888` | Hints, subtitles, secondary text |
| `text_label` | `#444444` | Form labels |
| `rating_orange` | `#FF9800` | Star rating chips |
| `black` | `#000000` | Pure black (video backgrounds) |
| `white` | `#FFFFFF` | Cards, overlays, button text |

---

## 9. Dependencies

```toml
# Versions
agp               = "9.1.0"
kotlin            = "2.3.20"
coreKtx           = "1.18.0"
lifecycleRuntime  = "2.10.0"
lifecycleViewmodel = "2.10.0"
activityCompose   = "1.13.0"
recyclerview      = "1.4.0"
retrofit          = "2.11.0"
okhttp            = "4.12.0"
coil              = "2.7.0"
composeBom        = "2026.03.01"
```

| Dependency | Purpose |
|---|---|
| `androidx.core:core-ktx` | Kotlin extensions for Android APIs |
| `androidx.lifecycle:lifecycle-runtime-ktx` | `lifecycleScope` for coroutines |
| `androidx.lifecycle:lifecycle-viewmodel-ktx` | ViewModel with coroutine support |
| `androidx.activity:activity-compose` | `setContent {}` in Activities |
| `androidx.recyclerview:recyclerview` | Grid and list views |
| `com.squareup.retrofit2:retrofit` | REST API client |
| `com.squareup.retrofit2:converter-gson` | JSON → Kotlin data class |
| `com.squareup.okhttp3:logging-interceptor` | Logs all HTTP requests/responses |
| `io.coil-kt:coil` | Async image loading with memory + disk cache |
| `androidx.compose:compose-bom` | Compose version alignment |
| `androidx.compose.ui:ui` | Core Compose UI |
| `androidx.compose.material3:material3` | Material Design 3 theming |

---

## 10. Sample Accounts

The app uses a local dummy authentication system for demo purposes. No backend or network call is made during login.

| Username | Password | Display Name |
|---|---|---|
| `rifky` | `123456` | Rifky Abdul Hanan |
| `jesslyn` | `654321` | Jesslyn Eklesia |

**Rules:**
- Username matching is **case-insensitive** (`RIFKY`, `Rifky`, and `rifky` all work)
- Password matching is **exact** (case-sensitive)
- Failed login shows a toast error ("Username atau Password salah") and shakes the Sign In button
- Successful login passes the user's display name to the home screen via Intent extra `USER_NAME`

---

*Generated from source — cloud.meis / Movie Gallery v1.0*
