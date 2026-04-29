package com.example.nothingpodcast.di

// AuthModule is intentionally empty — AuthRepository is @Singleton and @Inject constructor,
// so Hilt provides it automatically via its constructor. No explicit @Provides needed.
// FirebaseAuth has been removed; authentication is handled locally via DataStore.
