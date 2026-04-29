# Nothing Podcast 🔴

[![Android CI](https://github.com/LuigiPzz/NothingPodcast/actions/workflows/android.yml/badge.svg)](https://github.com/LuigiPzz/NothingPodcast/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-red.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Android-12%2B-black.svg?logo=android&logoColor=white)](https://www.android.com/)

A minimalist, high-performance podcast client designed with the **Nothing OS** aesthetic. Built for those who value clean typography, dot-matrix aesthetics, and a distraction-free listening experience.

> [!IMPORTANT]
> **AI-Powered & Community Driven**: This application was generated **entirely through Artificial Intelligence** by a Nothing enthusiast. It is a tribute from a fan to the Nothing community, exploring the boundaries of AI-assisted coding and minimalist design.

![Nothing Podcast Header](https://raw.githubusercontent.com/LuigiPzz/NothingPodcast/main/art/header.png) *(Placeholder for app header)*

## 📱 Design Philosophy
This app strictly follows the **Nothing Design Language**:
- **Typography**: Uses *Space Mono* for technical details and *Outfit* for a clean body text experience.
- **Visuals**: Signature red accents, glassmorphism, and dot-matrix patterns.
- **Interactivity**: Custom dot-based progress bars and interactive widgets.

## ✨ Key Features
- **Legacy Widget (4x2)**: A powerful, wide-format widget featuring:
    - Dynamic **Dotted Progress Bar** (custom Bitmap rendering).
    - Auto-scrolling (marquee) titles in **Space Mono**.
    - One-tap access to the media player screen.
- **Nothing UI**: Complete Compose-based interface with dark mode optimization.
- **Smartwatch Integration**: Full support for **CMF Watch (by Nothing)** and other MediaSession-compatible devices.
- **Media3 Playback**: Stable audio streaming and local playback using the latest Android Media3/ExoPlayer APIs.
- **Deep Linking**: Seamless navigation from system widgets directly to the active episode.

## 🛠️ Tech Stack
- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Architecture**: MVVM + Clean Architecture
- **Dependency Injection**: Hilt
- **Database**: Room
- **Audio**: Media3 / ExoPlayer
- **Persistence**: DataStore

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17
- Android SDK 34+

### Build
1. Clone the repository:
   ```bash
   git clone https://github.com/LuigiPzz/NothingPodcast.git
   ```
2. Open the project in Android Studio.
3. Sync Gradle and run the `app` module on a device (preferably a Nothing Phone for the full experience).

## 📄 License
This project is for educational and design-study purposes. All Nothing-related design elements are inspired by Nothing Technology Ltd.

---
*Created with ❤️ by the Nothing Podcast Team.*
