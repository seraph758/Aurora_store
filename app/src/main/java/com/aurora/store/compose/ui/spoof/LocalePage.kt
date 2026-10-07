/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.spoof

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.LocalFocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aurora.store.R
import com.aurora.store.compose.composable.LocaleListItem
import com.aurora.store.compose.composable.Placeholder
import com.aurora.store.compose.composable.TextDividerComposable
import com.aurora.store.compose.preview.ThemePreviewProvider
import com.aurora.store.viewmodel.spoof.SpoofViewModel
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest

@Composable
fun LocalePage(onRequestNavigateToSplash: () -> Unit, viewModel: SpoofViewModel = hiltViewModel()) {
    val filteredLocales by viewModel.filteredLocales.collectAsStateWithLifecycle()
    val isDefaultLocaleVisible by viewModel.isDefaultLocaleVisible.collectAsStateWithLifecycle()
    val currentLocale by viewModel.currentLocale.collectAsStateWithLifecycle()

    PageContent(
        defaultLocale = viewModel.defaultLocale,
        isDefaultLocaleVisible = isDefaultLocaleVisible,
        locales = filteredLocales,
        isLocaleSelected = { locale -> currentLocale == locale },
        onLocaleSelected = { locale ->
            viewModel.onLocaleSelected(locale)
            onRequestNavigateToSplash()
        },
        onSearch = { query -> viewModel.searchLocales(query) }
    )
}

@Composable
private fun PageContent(
    defaultLocale: Locale = Locale.getDefault(),
    isDefaultLocaleVisible: Boolean = true,
    locales: List<Locale> = emptyList(),
    isLocaleSelected: (locale: Locale) -> Boolean = { false },
    onLocaleSelected: (locale: Locale) -> Unit = {},
    onSearch: (query: String) -> Unit = {}
) {
    val focusManager = LocalFocusManager.current
    val textFieldState = rememberTextFieldState()

    LaunchedEffect(key1 = textFieldState) {
        snapshotFlow { textFieldState.text.toString() }
            .collectLatest { query -> onSearch(query) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = dimensionResource(R.dimen.spacing_medium),
                    vertical = dimensionResource(R.dimen.spacing_small)
                ),
            state = textFieldState,
            lineLimits = TextFieldLineLimits.SingleLine,
            placeholder = {
                Text(
                    text = stringResource(R.string.spoof_locale_search_hint),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_round_search),
                    contentDescription = stringResource(R.string.action_search)
                )
            },
            trailingIcon = {
                if (textFieldState.text.isNotEmpty()) {
                    IconButton(onClick = { textFieldState.clearText() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_cancel),
                            contentDescription = stringResource(R.string.action_clear)
                        )
                    }
                }
            },
            shape = CircleShape,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            onKeyboardAction = KeyboardActionHandler { focusManager.clearFocus() }
        )

        if (!isDefaultLocaleVisible && locales.isEmpty()) {
            Box(modifier = Modifier.weight(1f)) {
                Placeholder(
                    painter = painterResource(R.drawable.ic_round_search),
                    message = stringResource(R.string.spoof_locale_no_results)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.spacing_xsmall)
                )
            ) {
                if (isDefaultLocaleVisible) {
                    stickyHeader(key = "header_default") {
                        Surface(modifier = Modifier.fillMaxWidth()) {
                            TextDividerComposable(
                                title = stringResource(R.string.default_spoof)
                            )
                        }
                    }

                    item(key = "item_default") {
                        LocaleListItem(
                            displayName = defaultLocale.displayName,
                            displayLanguage = defaultLocale.getDisplayLanguage(defaultLocale),
                            isChecked = isLocaleSelected(defaultLocale),
                            onClick = { onLocaleSelected(defaultLocale) }
                        )
                    }
                }

                if (locales.isNotEmpty()) {
                    stickyHeader(key = "header_available") {
                        Surface(modifier = Modifier.fillMaxWidth()) {
                            TextDividerComposable(
                                title = stringResource(R.string.available_spoof)
                            )
                        }
                    }

                    items(items = locales, key = { locale -> locale.hashCode() }) { locale ->
                        LocaleListItem(
                            displayName = locale.displayName,
                            displayLanguage = locale.getDisplayLanguage(locale),
                            isChecked = isLocaleSelected(locale),
                            onClick = { onLocaleSelected(locale) }
                        )
                    }
                }
            }
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun LocalePagePreview() {
    PageContent(
        locales = Locale.getAvailableLocales().toList().filter { it.displayName.isNotBlank() },
        isLocaleSelected = { locale -> locale == Locale.getDefault() }
    )
}
