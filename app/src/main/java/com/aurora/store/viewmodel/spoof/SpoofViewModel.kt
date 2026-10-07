/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.viewmodel.spoof

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aurora.extensions.TAG
import com.aurora.store.data.providers.AuthProvider
import com.aurora.store.data.providers.NativeDeviceInfoProvider
import com.aurora.store.data.providers.SpoofProvider
import com.aurora.store.util.PathUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.Normalizer
import java.util.Locale
import java.util.Properties
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class SpoofViewModel @Inject constructor(
    private val spoofProvider: SpoofProvider,
    private val authProvider: AuthProvider,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private companion object {
        val DIACRITICS = Regex("\\p{M}+")
        val SEPARATORS = Regex("[\\-_()]")
        val WHITESPACE = Regex("\\s+")
    }

    /** Full sign-out, clearing both the account DB rows and the legacy prefs. */
    fun logout() = authProvider.logout()

    val defaultLocale: Locale = Locale.getDefault()
    val defaultProperties = NativeDeviceInfoProvider.getNativeDeviceProperties(context)

    private val _currentLocale = MutableStateFlow(spoofProvider.locale)
    val currentLocale = _currentLocale.asStateFlow()

    private val _availableLocales = MutableStateFlow(spoofProvider.availableSpoofLocales)
    val availableLocales = _availableLocales.asStateFlow()

    private val searchQuery = MutableStateFlow("")

    /**
     * Pre-computed, normalized text per locale that is matched against the search query. Covers
     * the name in the UI language, the native name, region/language names and their codes.
     * Built once off the main thread as resolving display names is comparatively expensive.
     */
    private val localeSearchIndex = viewModelScope.async(Dispatchers.Default) {
        (_availableLocales.value + defaultLocale).distinct().associateWith { locale ->
            listOf(
                locale.displayName,
                locale.getDisplayName(locale),
                locale.displayLanguage,
                locale.displayCountry,
                locale.toLanguageTag(),
                locale.language,
                locale.country
            ).joinToString(" ").normalizeForSearch()
        }
    }

    /** Available locales matching the current search query */
    val filteredLocales = combine(_availableLocales, searchQuery) { locales, query ->
        val tokens = query.toSearchTokens()
        if (tokens.isEmpty()) {
            locales
        } else {
            val index = localeSearchIndex.await()
            locales.filter { index.matches(it, tokens) }
        }
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = _availableLocales.value
    )

    /** Whether the default locale matches the current search query */
    val isDefaultLocaleVisible = searchQuery.map { query ->
        val tokens = query.toSearchTokens()
        tokens.isEmpty() || localeSearchIndex.await().matches(defaultLocale, tokens)
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = true
    )

    private val _currentDevice = MutableStateFlow(spoofProvider.deviceProperties)
    val currentDevice = _currentDevice.asStateFlow()

    private val _availableDevices = MutableStateFlow(spoofProvider.availableSpoofDeviceProperties)
    val availableDevices = _availableDevices.asStateFlow()

    fun onDeviceSelected(properties: Properties) {
        _currentDevice.value = properties

        if (properties == defaultProperties) {
            spoofProvider.removeSpoofDeviceProperties()
        } else {
            spoofProvider.setSpoofDeviceProperties(properties)
        }
    }

    fun onLocaleSelected(locale: Locale) {
        _currentLocale.value = locale

        if (locale == defaultLocale) {
            spoofProvider.removeSpoofLocale()
        } else {
            spoofProvider.setSpoofLocale(locale)
        }
    }

    fun searchLocales(query: String) {
        searchQuery.value = query
    }

    private fun Map<Locale, String>.matches(locale: Locale, tokens: List<String>): Boolean {
        val haystack = this[locale] ?: return false
        return tokens.all { haystack.contains(it) }
    }

    private fun String.toSearchTokens(): List<String> =
        normalizeForSearch().split(' ').filter { it.isNotEmpty() }

    /**
     * Lowercases, strips diacritics and treats separators (-, _, (, )) as spaces so that
     * "Türkçe", "turkce", "tr-TR" and "tr TR" all match the same way. Turkish dotless i is
     * folded explicitly as it has no decomposition.
     */
    private fun String.normalizeForSearch(): String = Normalizer
        .normalize(this, Normalizer.Form.NFD)
        .replace(DIACRITICS, "")
        .lowercase(Locale.ROOT)
        .replace('ı', 'i')
        .replace(SEPARATORS, " ")
        .replace(WHITESPACE, " ")
        .trim()

    fun importDeviceSpoof(uri: Uri) {
        try {
            context.contentResolver?.openInputStream(uri)?.use { input ->
                PathUtil.getNewEmptySpoofConfig(context).outputStream().use {
                    input.copyTo(it)
                }
            }
            _availableDevices.value = spoofProvider.availableSpoofDeviceProperties
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to import device config", exception)
        }
    }

    fun exportDeviceSpoof(uri: Uri) {
        try {
            NativeDeviceInfoProvider.getNativeDeviceProperties(context, true)
                .store(context.contentResolver?.openOutputStream(uri), "DEVICE_CONFIG")
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to export device config", exception)
        }
    }
}
