package com.x8bit.bitwarden.ui.platform.feature.search

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.BitwardenBasicDialogRow
import com.x8bit.bitwarden.ui.platform.components.BitwardenListItem
import com.x8bit.bitwarden.ui.platform.components.BitwardenMasterPasswordDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenSelectionDialog
import com.x8bit.bitwarden.ui.platform.components.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.SelectionItemData
import com.x8bit.bitwarden.ui.platform.components.model.toIconResources
import com.x8bit.bitwarden.ui.platform.feature.search.handlers.SearchHandlers
import com.x8bit.bitwarden.ui.platform.feature.search.model.AutofillSelectionOption
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import kotlinx.collections.immutable.toPersistentList

/**
 * The contents state for the search screen.
 */
@Suppress("LongMethod")
@Composable
fun SearchContent(
    viewState: SearchState.ViewState.Content,
    searchHandlers: SearchHandlers,
    modifier: Modifier = Modifier,
) {
    var showConfirmationDialog: ListingItemOverflowAction? by rememberSaveable {
        mutableStateOf(null)
    }
    when (val option = showConfirmationDialog) {
        is ListingItemOverflowAction.SendAction.DeleteClick -> {
            BitwardenTwoButtonDialog(
                title = stringResource(id = R.string.delete),
                message = stringResource(id = R.string.are_you_sure_delete_send),
                confirmButtonText = stringResource(id = R.string.yes),
                dismissButtonText = stringResource(id = R.string.cancel),
                onConfirmClick = {
                    showConfirmationDialog = null
                    searchHandlers.onOverflowItemClick(option)
                },
                onDismissClick = { showConfirmationDialog = null },
                onDismissRequest = { showConfirmationDialog = null },
            )
        }

        is ListingItemOverflowAction.SendAction.CopyUrlClick,
        is ListingItemOverflowAction.SendAction.EditClick,
        is ListingItemOverflowAction.SendAction.RemovePasswordClick,
        is ListingItemOverflowAction.SendAction.ShareUrlClick,
        is ListingItemOverflowAction.VaultAction.CopyNoteClick,
        is ListingItemOverflowAction.VaultAction.CopyNumberClick,
        is ListingItemOverflowAction.VaultAction.CopyPasswordClick,
        is ListingItemOverflowAction.VaultAction.CopyTotpClick,
        is ListingItemOverflowAction.VaultAction.CopySecurityCodeClick,
        is ListingItemOverflowAction.VaultAction.CopyUsernameClick,
        is ListingItemOverflowAction.VaultAction.EditClick,
        is ListingItemOverflowAction.VaultAction.LaunchClick,
        is ListingItemOverflowAction.VaultAction.ViewClick,
        null,
        -> Unit
    }

    var autofillSelectionOptionsItem by rememberSaveable {
        mutableStateOf<SearchState.DisplayItem?>(null)
    }
    var masterPasswordRepromptData by rememberSaveable {
        mutableStateOf<MasterPasswordRepromptData?>(null)
    }
    autofillSelectionOptionsItem?.let { item ->
        AutofillSelectionDialog(
            displayItem = item,
            onAutofillItemClick = searchHandlers.onAutofillItemClick,
            onAutofillAndSaveItemClick = searchHandlers.onAutofillAndSaveItemClick,
            onViewItemClick = searchHandlers.onItemClick,
            onMasterPasswordRepromptRequest = { masterPasswordRepromptData = it },
            onDismissRequest = { autofillSelectionOptionsItem = null },
        )
    }
    masterPasswordRepromptData?.let { data ->
        BitwardenMasterPasswordDialog(
            onConfirmClick = { password ->
                searchHandlers.onMasterPasswordRepromptSubmit(password, data)
                masterPasswordRepromptData = null
            },
            onDismissRequest = {
                masterPasswordRepromptData = null
            },
        )
    }

    LazyColumn(
        modifier = modifier,
    ) {
        items(viewState.displayItems) {
            BitwardenListItem(
                startIcon = it.iconData,
                label = it.title,
                supportingLabel = it.subtitle,
                onClick = {
                    if (it.autofillSelectionOptions.isNotEmpty()) {
                        autofillSelectionOptionsItem = it
                    } else {
                        searchHandlers.onItemClick(it.id)
                    }
                },
                trailingLabelIcons = it
                    .extraIconList
                    .toIconResources()
                    .toPersistentList(),
                selectionDataList = it
                    .overflowOptions
                    .map { option ->
                        SelectionItemData(
                            text = option.title(),
                            onClick = {
                                when (option) {
                                    is ListingItemOverflowAction.SendAction.DeleteClick -> {
                                        showConfirmationDialog = option
                                    }

                                    else -> searchHandlers.onOverflowItemClick(option)
                                }
                            },
                        )
                    }
                    .toPersistentList(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        // There is some built-in padding to the menu button that makes up
                        // the visual difference here.
                        end = 12.dp,
                    ),
            )
        }

        item {
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun AutofillSelectionDialog(
    displayItem: SearchState.DisplayItem,
    onAutofillItemClick: (cipherId: String) -> Unit,
    onAutofillAndSaveItemClick: (cipherId: String) -> Unit,
    onViewItemClick: (cipherId: String) -> Unit,
    onMasterPasswordRepromptRequest: (MasterPasswordRepromptData) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val selectionCallback: (SearchState.DisplayItem, MasterPasswordRepromptData.Type) -> Unit =
        { item, type ->
            onDismissRequest()
            if (item.shouldDisplayMasterPasswordReprompt) {
                onMasterPasswordRepromptRequest(
                    MasterPasswordRepromptData(
                        cipherId = item.id,
                        type = type,
                    ),
                )
            } else {
                when (type) {
                    MasterPasswordRepromptData.Type.AUTOFILL -> {
                        onAutofillItemClick(item.id)
                    }

                    MasterPasswordRepromptData.Type.AUTOFILL_AND_SAVE -> {
                        onAutofillAndSaveItemClick(item.id)
                    }
                }
            }
        }
    BitwardenSelectionDialog(
        title = stringResource(id = R.string.autofill_or_view),
        onDismissRequest = onDismissRequest,
        selectionItems = {
            if (AutofillSelectionOption.AUTOFILL in displayItem.autofillSelectionOptions) {
                BitwardenBasicDialogRow(
                    text = stringResource(id = R.string.autofill),
                    onClick = {
                        selectionCallback(
                            displayItem,
                            MasterPasswordRepromptData.Type.AUTOFILL,
                        )
                    },
                )
            }
            if (AutofillSelectionOption.AUTOFILL_AND_SAVE in displayItem.autofillSelectionOptions) {
                BitwardenBasicDialogRow(
                    text = stringResource(id = R.string.autofill_and_save),
                    onClick = {
                        selectionCallback(
                            displayItem,
                            MasterPasswordRepromptData.Type.AUTOFILL_AND_SAVE,
                        )
                    },
                )
            }
            if (AutofillSelectionOption.VIEW in displayItem.autofillSelectionOptions) {
                BitwardenBasicDialogRow(
                    text = stringResource(id = R.string.view),
                    onClick = {
                        onDismissRequest()
                        onViewItemClick(displayItem.id)
                    },
                )
            }
        },
    )
}
