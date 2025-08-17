package com.example.n8nmonitor.di

// BuildConfig will be generated at build time
// For now, we'll use a constant for DEBUG mode
import com.example.n8nmonitor.BuildConfig
import com.example.n8nmonitor.data.api.AuthInterceptor
import com.example.n8nmonitor.data.api.ApiKeyProvider
import com.example.n8nmonitor.data.api.N8nApiService
import com.example.n8nmonitor.data.settings.SettingsDataStore
import com.squareup.moshi.Moshi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .build()
    }

    @Provides
    @Singleton
    fun provideApiKeyProvider(settingsDataStore: SettingsDataStore): ApiKeyProvider {
        return object : ApiKeyProvider {
            override suspend fun getApiKey(): String? {
                return settingsDataStore.apiKey.first()
            }
            
            override suspend fun getBaseUrl(): String? {
                return settingsDataStore.baseUrl.first()
            }
        }
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(apiKeyProvider: ApiKeyProvider): AuthInterceptor {
        return AuthInterceptor(apiKeyProvider)
    }

    @Provides
    @Singleton
    fun provideCertificatePinner(): CertificatePinner? {
        return if (BuildConfig.DEBUG) {
            null  // Désactivé en debug
        } else {
            CertificatePinner.Builder()
                .add("your-n8n-domain.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
                .build()
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor, certificatePinner: CertificatePinner?): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // Use BODY level only for debugging
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val builder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            
        // Appliquer le certificate pinning uniquement en production
        if (certificatePinner != null) {
            builder.certificatePinner(certificatePinner)
        }
            
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://placeholder.com/") // Will be set dynamically
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    @Provides
    @Singleton
    fun provideN8nApiService(retrofit: Retrofit): N8nApiService {
        return retrofit.create(N8nApiService::class.java)
    }
}