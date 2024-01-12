package com.x8bit.bitwarden.ui.tools.feature.send

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.repository.util.baseWebSendUrl
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.SendData
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendStatusIcon
import com.x8bit.bitwarden.ui.tools.feature.send.util.toViewState
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the send screen.
 */
@Suppress("TooManyFunctions")
@HiltViewModel
class SendViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val clipboardManager: BitwardenClipboardManager,
    private val environmentRepo: EnvironmentRepository,
    private val vaultRepo: VaultRepository,
) : BaseViewModel<SendState, SendEvent, SendAction>(
    // We load the state from the savedStateHandle for testing purposes.
    initialState = savedStateHandle[KEY_STATE]
        ?: SendState(
            viewState = SendState.ViewState.Loading,
            dialogState = null,
        ),
) {

    init {
        vaultRepo
            .sendDataStateFlow
            .map { SendAction.Internal.SendDataReceive(it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: SendAction): Unit = when (action) {
        SendAction.AboutSendClick -> handleAboutSendClick()
        SendAction.AddSendClick -> handleAddSendClick()
        SendAction.LockClick -> handleLockClick()
        SendAction.RefreshClick -> handleRefreshClick()
        SendAction.SearchClick -> handleSearchClick()
        SendAction.SyncClick -> handleSyncClick()
        is SendAction.CopyClick -> handleCopyClick(action)
        SendAction.FileTypeClick -> handleFileTypeClick()
        is SendAction.SendClick -> handleSendClick(action)
        is SendAction.ShareClick -> handleShareClick(action)
        SendAction.TextTypeClick -> handleTextTypeClick()
        is SendAction.DeleteSendClick -> handleDeleteSendClick(action)
        is SendAction.RemovePasswordClick -> handleRemovePasswordClick(action)
        is SendAction.Internal -> handleInternalAction(action)
    }

    private fun handleInternalAction(action: SendAction.Internal): Unit = when (action) {
        is SendAction.Internal.SendDataReceive -> handleSendDataReceive(action)
    }

    private fun handleSendDataReceive(action: SendAction.Internal.SendDataReceive) {
        when (val dataState = action.sendDataState) {
            is DataState.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = SendState.ViewState.Error(
                            message = R.string.generic_error_message.asText(),
                        ),
                        dialogState = null,
                    )
                }
            }

            is DataState.Loaded -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = dataState.data.toViewState(
                            baseWebSendUrl = environmentRepo
                                .environment
                                .environmentUrlData
                                .baseWebSendUrl,
                        ),
                        dialogState = null,
                    )
                }
            }

            DataState.Loading -> {
                mutableStateFlow.update {
                    it.copy(viewState = SendState.ViewState.Loading)
                }
            }

            is DataState.NoNetwork -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = SendState.ViewState.Error(
                            message = R.string.internet_connection_required_title
                                .asText()
                                .concat(R.string.internet_connection_required_message.asText()),
                        ),
                        dialogState = null,
                    )
                }
            }

            is DataState.Pending -> {
                mutableStateFlow.update {
                    it.copy(
                        viewState = dataState.data.toViewState(
                            baseWebSendUrl = environmentRepo
                                .environment
                                .environmentUrlData
                                .baseWebSendUrl,
                        ),
                    )
                }
            }
        }
    }

    private fun handleAboutSendClick() {
        sendEvent(SendEvent.NavigateToAboutSend)
    }

    private fun handleAddSendClick() {
        sendEvent(SendEvent.NavigateNewSend)
    }

    private fun handleLockClick() {
        vaultRepo.lockVaultForCurrentUser()
    }

    private fun handleRefreshClick() {
        // No need to update the view state, the vault repo will emit a new state during this time.
        vaultRepo.sync()
    }

    private fun handleSearchClick() {
        // TODO: navigate to send search BIT-594
        sendEvent(SendEvent.ShowToast("Search Not Implemented".asText()))
    }

    private fun handleSyncClick() {
        mutableStateFlow.update {
            it.copy(dialogState = SendState.DialogState.Loading(R.string.syncing.asText()))
        }
        vaultRepo.sync()
    }

    private fun handleCopyClick(action: SendAction.CopyClick) {
        clipboardManager.setText(text = action.sendItem.shareUrl)
    }

    private fun handleSendClick(action: SendAction.SendClick) {
        sendEvent(SendEvent.NavigateToEditSend(action.sendItem.id))
    }

    private fun handleShareClick(action: SendAction.ShareClick) {
        sendEvent(SendEvent.ShowShareSheet(action.sendItem.shareUrl))
    }

    private fun handleFileTypeClick() {
        // TODO: Navigate to the file type send list screen (BIT-1388)
        sendEvent(SendEvent.ShowToast("Not yet implemented".asText()))
    }

    private fun handleTextTypeClick() {
        // TODO: Navigate to the text type send list screen (BIT-1388)
        sendEvent(SendEvent.ShowToast("Not yet implemented".asText()))
    }

    private fun handleDeleteSendClick(action: SendAction.DeleteSendClick) {
        // TODO: Navigate to the text type send list screen (BIT-1388)
        sendEvent(SendEvent.ShowToast("Not yet implemented".asText()))
    }

    private fun handleRemovePasswordClick(action: SendAction.RemovePasswordClick) {
        // TODO: Navigate to the text type send list screen (BIT-1388)
        sendEvent(SendEvent.ShowToast("Not yet implemented".asText()))
    }
}

/**
 * Models state for the Send screen.
 */
@Parcelize
data class SendState(
    val viewState: ViewState,
    val dialogState: DialogState?,
) : Parcelable {

    /**
     * Represents the specific view states for the send screen.
     */
    sealed class ViewState : Parcelable {
        /**
         * Indicates if the FAB should be displayed.
         */
        abstract val shouldDisplayFab: Boolean

        /**
         * Show the populated state.
         */
        @Parcelize
        data class Content(
            val textTypeCount: Int,
            val fileTypeCount: Int,
            val sendItems: List<SendItem>,
        ) : ViewState() {
            override val shouldDisplayFab: Boolean get() = true

            /**
             * Represents the an individual send item to be displayed.
             */
            @Parcelize
            data class SendItem(
                val id: String,
                val name: String,
                val deletionDate: String,
                val type: Type,
                val iconList: List<SendStatusIcon>,
                val shareUrl: String,
                val hasPassword: Boolean,
            ) : Parcelable {
                /**
                 * Indicates the type of send this, a text or file.
                 */
                enum class Type(@DrawableRes val iconRes: Int) {
                    FILE(iconRes = R.drawable.ic_send_file),
                    TEXT(iconRes = R.drawable.ic_send_text),
                }
            }
        }

        /**
         * Show the empty state.
         */
        @Parcelize
        data object Empty : ViewState() {
            override val shouldDisplayFab: Boolean get() = true
        }

        /**
         * Represents an error state for the [VaultItemScreen].
         */
        @Parcelize
        data class Error(
            val message: Text,
        ) : ViewState() {
            override val shouldDisplayFab: Boolean get() = false
        }

        /**
         * Show the loading state.
         */
        @Parcelize
        data object Loading : ViewState() {
            override val shouldDisplayFab: Boolean get() = false
        }
    }

    /**
     * Represents the current state of any dialogs on the screen.
     */
    sealed class DialogState : Parcelable {

        /**
         * Represents a loading dialog with the given [message].
         */
        @Parcelize
        data class Loading(
            val message: Text,
        ) : DialogState()
    }
}

/**
 * Models actions for the send screen.
 */
sealed class SendAction {
    /**
     * User clicked the about send button.
     */
    data object AboutSendClick : SendAction()

    /**
     * User clicked add a send.
     */
    data object AddSendClick : SendAction()

    /**
     * User clicked the lock button.
     */
    data object LockClick : SendAction()

    /**
     * User clicked the refresh button.
     */
    data object RefreshClick : SendAction()

    /**
     * User clicked search button.
     */
    data object SearchClick : SendAction()

    /**
     * User clicked the sync button.
     */
    data object SyncClick : SendAction()

    /**
     * User clicked the file type button.
     */
    data object FileTypeClick : SendAction()

    /**
     * User clicked the text type button.
     */
    data object TextTypeClick : SendAction()

    /**
     * User clicked the item row.
     */
    data class SendClick(
        val sendItem: SendState.ViewState.Content.SendItem,
    ) : SendAction()

    /**
     * User clicked the copy item button.
     */
    data class CopyClick(
        val sendItem: SendState.ViewState.Content.SendItem,
    ) : SendAction()

    /**
     * User clicked the share item button.
     */
    data class ShareClick(
        val sendItem: SendState.ViewState.Content.SendItem,
    ) : SendAction()

    /**
     * User clicked the delete item button.
     */
    data class DeleteSendClick(
        val sendItem: SendState.ViewState.Content.SendItem,
    ) : SendAction()

    /**
     * User clicked the remove password item button.
     */
    data class RemovePasswordClick(
        val sendItem: SendState.ViewState.Content.SendItem,
    ) : SendAction()

    /**
     * Models actions that the [SendViewModel] itself will send.
     */
    sealed class Internal : SendAction() {
        /**
         * Indicates that the send data has been received.
         */
        data class SendDataReceive(
            val sendDataState: DataState<SendData>,
        ) : Internal()
    }
}

/**
 * Models events for the send screen.
 */
sealed class SendEvent {
    /**
     * Navigate to the new send screen.
     */
    data object NavigateNewSend : SendEvent()

    /**
     * Navigate to the edit send screen.
     */
    data class NavigateToEditSend(val sendId: String) : SendEvent()

    /**
     * Navigate to the about send screen.
     */
    data object NavigateToAboutSend : SendEvent()

    /**
     * Show a share sheet with the given content.
     */
    data class ShowShareSheet(val url: String) : SendEvent()

    /**
     * Show a toast to the user.
     */
    data class ShowToast(val message: Text) : SendEvent()
}
