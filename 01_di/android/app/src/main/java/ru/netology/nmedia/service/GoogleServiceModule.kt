package ru.netology.nmedia.service

import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessaging
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent


@InstallIn(SingletonComponent::class)
@Module

class GoogleServiceModule {


    @Provides
    fun provideFirebaseMessagingModule(): FirebaseMessaging {
        return FirebaseMessaging.getInstance()
    }


    @Provides
    fun provideGoogleApi(): GoogleApiAvailability {
        return GoogleApiAvailability.getInstance()
    }
}