/*
 * SPDX-FileCopyrightText: 2026 Aurora OSS
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.tv.ui.spoof

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ListItem
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.RadioButton
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import com.aurora.store.R
import com.aurora.store.viewmodel.spoof.SpoofViewModel
import java.util.Properties
import kotlinx.coroutines.android.awaitFrame

private enum class SpoofTab(val title: Int) {
    DEVICE(R.string.title_device),
    LANGUAGE(R.string.title_language)
}

/**
 * TV counterpart of the phone SpoofScreen: pick a device or locale spoof with the D-pad.
 *
 * Import/export are left out since they need a document picker, which most TVs lack. As on the
 * phone, a new spoof only takes effect after signing out and back in, so a change surfaces a
 * restart prompt instead of the phone's snackbar.
 */
@Composable
fun SpoofScreen(onRestart: () -> Unit, viewModel: SpoofViewModel = hiltViewModel()) {
    val availableDevices by viewModel.availableDevices.collectAsStateWithLifecycle()
    val currentDevice by viewModel.currentDevice.collectAsStateWithLifecycle()
    val availableLocales by viewModel.availableLocales.collectAsStateWithLifecycle()
    val currentLocale by viewModel.currentLocale.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var restartPending by rememberSaveable { mutableStateOf(false) }

    val tabFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        // Wait a frame so the focus node is attached before requesting focus.
        awaitFrame()
        runCatching { tabFocusRequester.requestFocus() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.tv_content_padding)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
    ) {
        Text(
            text = stringResource(R.string.title_spoof_manager),
            style = MaterialTheme.typography.headlineSmall
        )

        if (restartPending) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    dimensionResource(R.dimen.spacing_medium)
                )
            ) {
                Text(stringResource(R.string.force_restart_snack))
                Button(
                    onClick = {
                        viewModel.logout()
                        onRestart()
                    }
                ) {
                    Text(stringResource(R.string.action_restart))
                }
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.focusRequester(tabFocusRequester)
        ) {
            SpoofTab.entries.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTab == index,
                    onFocus = { selectedTab = index }
                ) {
                    Text(
                        text = stringResource(tab.title),
                        modifier = Modifier.padding(
                            horizontal = dimensionResource(R.dimen.spacing_medium),
                            vertical = dimensionResource(R.dimen.spacing_small)
                        )
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // contentPadding (not Modifier.padding) keeps focused rows from clipping at the edges.
            contentPadding = PaddingValues(vertical = dimensionResource(R.dimen.spacing_small)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall))
        ) {
            when (SpoofTab.entries[selectedTab]) {
                SpoofTab.DEVICE -> {
                    header(R.string.default_spoof)
                    item {
                        DeviceItem(
                            device = viewModel.defaultProperties,
                            selected = currentDevice == viewModel.defaultProperties,
                            onClick = {
                                viewModel.onDeviceSelected(viewModel.defaultProperties)
                                restartPending = true
                            }
                        )
                    }
                    header(R.string.available_spoof)
                    items(
                        items = availableDevices,
                        key = { device -> device.getProperty("Build.PRODUCT") }
                    ) { device ->
                        DeviceItem(
                            device = device,
                            selected = currentDevice == device,
                            onClick = {
                                viewModel.onDeviceSelected(device)
                                restartPending = true
                            }
                        )
                    }
                }

                SpoofTab.LANGUAGE -> {
                    header(R.string.default_spoof)
                    item {
                        SpoofItem(
                            headline = viewModel.defaultLocale.displayName,
                            supporting = viewModel.defaultLocale.getDisplayLanguage(
                                viewModel.defaultLocale
                            ),
                            selected = currentLocale == viewModel.defaultLocale,
                            onClick = {
                                viewModel.onLocaleSelected(viewModel.defaultLocale)
                                restartPending = true
                            }
                        )
                    }
                    header(R.string.available_spoof)
                    items(
                        items = availableLocales,
                        key = { locale -> locale.toLanguageTag() }
                    ) { locale ->
                        SpoofItem(
                            headline = locale.displayName,
                            supporting = locale.getDisplayLanguage(locale),
                            selected = currentLocale == locale,
                            onClick = {
                                viewModel.onLocaleSelected(locale)
                                restartPending = true
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListScope.header(title: Int) {
    item(contentType = "header") {
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = dimensionResource(R.dimen.spacing_small))
        )
    }
}

@Composable
private fun DeviceItem(device: Properties, selected: Boolean, onClick: () -> Unit) {
    SpoofItem(
        headline = device.getProperty("UserReadableName"),
        supporting = stringResource(
            R.string.spoof_property,
            device.getProperty("Build.MANUFACTURER"),
            device.getProperty("Build.VERSION.SDK_INT")
        ),
        selected = selected,
        onClick = onClick
    )
}

@Composable
private fun SpoofItem(
    headline: String,
    supporting: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        selected = selected,
        onClick = onClick,
        headlineContent = {
            Text(text = headline, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(text = supporting, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        trailingContent = { RadioButton(selected = selected, onClick = null) }
    )
}
