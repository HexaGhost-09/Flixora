# 🎬 Flixora

> **Stream Everything** — A premium media streaming Android app built with Kotlin & Jetpack Compose

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-2024.08-blue)](https://developer.android.com/jetpack/compose)
[![TMDB](https://img.shields.io/badge/TMDB%20API-v3-green)](https://www.themoviedb.org/documentation/api)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

---

## ✨ Features

- 🏠 **Home** — Auto-scrolling hero banner with trending movies & shows
- 🔍 **Search** — Real-time multi-search with debounce
- 🎭 **Browse** — Discover by genre (Movies & TV)
- 📄 **Detail** — Full detail page with cast, trailers, ratings & similar content
- ▶️ **Trailer Player** — YouTube trailer playback via WebView
- 🔖 **Watchlist** — Save movies/shows locally with Room DB

---

## 🏗 Architecture

```
Clean Architecture + MVVM
├── data/           → API (Retrofit), Room DB, Repository implementations
├── domain/         → Models, Repository interfaces, Use cases
├── presentation/   → ViewModels + Jetpack Compose Screens
├── navigation/     → Compose Navigation + Bottom Nav
└── ui/             → Theme (Color, Typography), Reusable components
```

**Tech Stack:**
| Component | Technology |
|---|---|
| UI | Jetpack Compose + Material 3 |
| DI | Hilt |
| Networking | Retrofit 2 + OkHttp |
| Images | Coil |
| Database | Room |
| Video | WebView (YouTube embeds) |
| Async | Kotlin Coroutines + StateFlow |

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK 26+
- TMDB API Key (free at [themoviedb.org](https://www.themoviedb.org/settings/api))

### Setup

1. **Clone the repo**
   ```bash
   git clone https://github.com/HexaGhost-09/Flixora.git
   cd Flixora
   ```

2. **Add your TMDB API key** to `local.properties`:
   ```properties
   TMDB_API_KEY=your_api_key_here
   ```
   > ⚠️ The API key is already baked into `BuildConfig` for development. Replace it with your own for production.

3. **Open in Android Studio** and sync Gradle

4. **Run on device** (minSdk 26)

---

## 📱 Screenshots

*Coming soon*

---

## 🎨 Design

- **Theme:** Deep space cinema — dark navy backgrounds, electric purple & cyan accents
- **Animations:** Spring-bounce card presses, shimmer loading skeletons, auto-pager hero banner
- **Typography:** Material 3 with bold weight hierarchy

---

## 📄 License

This project is licensed under the [GNU General Public License v3.0](LICENSE).

---

*Built with ❤️ using Kotlin + Jetpack Compose*
