/*
 * SPDX-FileCopyrightText: 2025 The Calyx Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.aurora.store.compose.ui.installed.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import com.aurora.store.R
import com.aurora.store.compose.preview.ThemePreviewProvider

/**
 * Menu for the installed apps & games screen
 * @param modifier The modifier to be applied to the composable
 * @param onMenuItemClicked Callback when a menu item has been clicked
 * @see MenuItem
 */
@Composable
fun InstalledMenu(
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    onMenuItemClicked: (menuItem: MenuItem) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(isExpanded) }
    fun onClick(menuItem: MenuItem) {
        onMenuItemClicked(menuItem)
        expanded = false
    }

    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vert),
                contentDescription = stringResource(R.string.menu)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.action_select_all)) },
                onClick = { onClick(MenuItem.SELECT_ALL) }
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.action_remove_all)) },
                onClick = { onClick(MenuItem.REMOVE_ALL) }
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.action_mark_favourite)) },
                onClick = { onClick(MenuItem.MARK_FAVOURITE) }
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.action_unmark_favourite)) },
                onClick = { onClick(MenuItem.UNMARK_FAVOURITE) }
            )
            DropdownMenuItem(
                text = { Text(text = stringResource(R.string.action_uninstall)) },
                onClick = { onClick(MenuItem.UNINSTALL) }
            )
        }
    }
}

@PreviewWrapper(ThemePreviewProvider::class)
@Preview(showBackground = true)
@Composable
private fun InstalledMenuPreview() {
    InstalledMenu(isExpanded = true)
}
