package com.autoguard.vpn.di

import android.content.Context
import com.autoguard.vpn.data.api.ServerApiService
import com.autoguard.vpn.data.api.VpnGateApiService
import com.autoguard.vpn.data.repository.ServerRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Application dependency injection module
 * Provides instances for core dependencies like network, repository, etc.
 *
 * Supported data sources:
 * - VPN Gate (vpngate.net) - CSV format API
 * - Custom API - JSON format API
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    /**
     * Provides OkHttpClient instance
     * Configured with logging interceptor and timeout settings
     */
    @Provides
    @Singleton
    @DefaultClient
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(VpnGateApiService.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(VpnGateApiService.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(VpnGateApiService.WRITE_TIMEOUT, TimeUnit.SECONDS)
            // Allow HTTP to HTTPS redirects
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * Provides Retrofit instance (for custom API)
     */
    @Provides
    @Singleton
    @DefaultRetrofit
    fun provideRetrofit(@DefaultClient okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.autoguard-vpn.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Provides specialized OkHttpClient for VPN Gate
     * VPN Gate uses HTTPS to comply with modern network security policies
     */
    @Provides
    @Singleton
    @VpnGateClient
    fun provideVpnGateOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(VpnGateApiService.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(VpnGateApiService.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(VpnGateApiService.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * Provides specialized Retrofit instance for VPN Gate
     */
    @Provides
    @Singleton
    @VpnGateRetrofit
    fun provideVpnGateRetrofit(@VpnGateClient okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            // Use HTTPS to avoid cleartext traffic errors
            .baseUrl("https://www.vpngate.net/")
            .client(okHttpClient)
            // VPN Gate returns CSV, use StringConverter
            .addConverterFactory(StringConverterFactory())
            .build()
    }

    /**
     * Provides ServerApiService instance (for custom API)
     */
    @Provides
    @Singleton
    fun provideServerApiService(@DefaultRetrofit retrofit: Retrofit): ServerApiService {
        return retrofit.create(ServerApiService::class.java)
    }

    /**
     * Provides VpnGateApiService instance
     * Used to fetch server list from vpngate.net
     */
    @Provides
    @Singleton
    fun provideVpnGateApiService(@VpnGateRetrofit retrofit: Retrofit): VpnGateApiService {
        return retrofit.create(VpnGateApiService::class.java)
    }

    /**
     * Provides server repository instance
     * Contains two API services: Custom API and VPN Gate API
     */
    @Provides
    @Singleton
    fun provideServerRepository(
        @ApplicationContext context: Context,
        serverApiService: ServerApiService,
        vpnGateApiService: VpnGateApiService,
        @DefaultClient okHttpClient: OkHttpClient
    ): ServerRepository {
        return ServerRepository(context, serverApiService, vpnGateApiService, okHttpClient)
    }
}
