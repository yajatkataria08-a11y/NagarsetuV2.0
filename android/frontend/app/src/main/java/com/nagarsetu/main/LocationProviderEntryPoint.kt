package com.nagarsetu.main

import com.nagarsetu.core.utils.LocationProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt EntryPoint that lets Composables (which aren't injected by Hilt
 * directly) retrieve the [LocationProvider] singleton from the application
 * component without needing a ViewModel intermediary.
 *
 * Usage:
 *   val lp = EntryPointAccessors.fromApplication(
 *       context.applicationContext,
 *       LocationProviderEntryPoint::class.java
 *   ).locationProvider()
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface LocationProviderEntryPoint {
    fun locationProvider(): LocationProvider
}
