package com.example.n8nmonitor.di

// BuildConfig will be generated at build time
// For now, we'll use a constant for DEBUG mode
import com.example.n8nmonitor.data.api.AuthInterceptor
import com.example.n8nmonitor.data.api.ApiKeyProvider
import com.example.n8nmonitor.data.api.N8nApiService
import com.example.n8nmonitor.data.settings.SettingsDataStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
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
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiKeyProvider(settingsDataStore: SettingsDataStore): ApiKeyProvider {
        return object : ApiKeyProvider {
            override fun getApiKey(): String? {
                // This is a simplified version - in practice you'd need to handle the Flow
                return null // Will be injected properly in the actual implementation
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
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            // Always use BODY level for debugging purposes
            // In a production app, you would use BuildConfig.DEBUG
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
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