package com.x8bit.bitwarden.ui.vault.feature.itemlisting.util

import com.bitwarden.send.SendType
import com.bitwarden.vault.CipherType
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.platform.components.model.IconRes
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.VaultItemListingState
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction

/**
 * Create a mock [VaultItemListingState.DisplayItem] with a given [number].
 */
fun createMockDisplayItemForCipher(
    number: Int,
    cipherType: CipherType = CipherType.LOGIN,
    subtitle: String? = "mockUsername-$number",
): VaultItemListingState.DisplayItem =
    when (cipherType) {
        CipherType.LOGIN -> {
            VaultItemListingState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                titleTestTag = "CipherNameLabel",
                secondSubtitle = null,
                secondSubtitleTestTag = null,
                subtitle = subtitle,
                subtitleTestTag = "CipherSubTitleLabel",
                iconData = IconData.Network(
                    "https://vault.bitwarden.com/icons/www.mockuri.com/icon.png",
                    fallbackIconRes = R.drawable.ic_login_item,
                ),
                extraIconList = listOf(
                    IconRes(
                        iconRes = R.drawable.ic_collection,
                        contentDescription = R.string.collections.asText(),
                        testTag = "CipherInCollectionIcon",
                    ),
                    IconRes(
                        iconRes = R.drawable.ic_attachment,
                        contentDescription = R.string.attachments.asText(),
                        testTag = "CipherWithAttachmentsIcon",
                    ),
                ),
                overflowOptions = listOf(
                    ListingItemOverflowAction.VaultAction.ViewClick(cipherId = "mockId-$number"),
                    ListingItemOverflowAction.VaultAction.EditClick(
                        cipherId = "mockId-$number",
                        requiresPasswordReprompt = true,
                    ),
                    ListingItemOverflowAction.VaultAction.CopyUsernameClick(
                        username = "mockUsername-$number",
                    ),
                    ListingItemOverflowAction.VaultAction.CopyPasswordClick(
                        password = "mockPassword-$number",
                        requiresPasswordReprompt = true,
                        cipherId = "mockId-$number",
                    ),
                    ListingItemOverflowAction.VaultAction.CopyTotpClick(
                        totpCode = "mockTotp-$number",
                    ),
                    ListingItemOverflowAction.VaultAction.LaunchClick(
                        url = "www.mockuri$number.com",
                    ),
                ),
                optionsTestTag = "CipherOptionsButton",
                isAutofill = false,
                isFido2Creation = false,
                shouldShowMasterPasswordReprompt = false,
                iconTestTag = "LoginCipherIcon",
            )
        }

        CipherType.SECURE_NOTE -> {
            VaultItemListingState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                titleTestTag = "CipherNameLabel",
                secondSubtitle = null,
                secondSubtitleTestTag = null,
                subtitle = subtitle,
                subtitleTestTag = "CipherSubTitleLabel",
                iconData = IconData.Local(R.drawable.ic_secure_note_item),
                extraIconList = listOf(
                    IconRes(
                        iconRes = R.drawable.ic_collection,
                        contentDescription = R.string.collections.asText(),
                        testTag = "CipherInCollectionIcon",
                    ),
                    IconRes(
                        iconRes = R.drawable.ic_attachment,
                        contentDescription = R.string.attachments.asText(),
                        testTag = "CipherWithAttachmentsIcon",
                    ),
                ),
                overflowOptions = listOf(
                    ListingItemOverflowAction.VaultAction.ViewClick(cipherId = "mockId-$number"),
                    ListingItemOverflowAction.VaultAction.EditClick(
                        cipherId = "mockId-$number",
                        requiresPasswordReprompt = true,
                    ),
                    ListingItemOverflowAction.VaultAction.CopyNoteClick(
                        notes = "mockNotes-$number",
                    ),
                ),
                optionsTestTag = "CipherOptionsButton",
                isAutofill = false,
                isFido2Creation = false,
                shouldShowMasterPasswordReprompt = false,
                iconTestTag = "SecureNoteCipherIcon",
            )
        }

        CipherType.CARD -> {
            VaultItemListingState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                titleTestTag = "CipherNameLabel",
                secondSubtitle = null,
                secondSubtitleTestTag = null,
                subtitle = subtitle,
                subtitleTestTag = "CipherSubTitleLabel",
                iconData = IconData.Local(R.drawable.ic_card_item),
                extraIconList = listOf(
                    IconRes(
                        iconRes = R.drawable.ic_collection,
                        contentDescription = R.string.collections.asText(),
                        testTag = "CipherInCollectionIcon",
                    ),
                    IconRes(
                        iconRes = R.drawable.ic_attachment,
                        contentDescription = R.string.attachments.asText(),
                        testTag = "CipherWithAttachmentsIcon",
                    ),
                ),
                overflowOptions = listOf(
                    ListingItemOverflowAction.VaultAction.ViewClick(cipherId = "mockId-$number"),
                    ListingItemOverflowAction.VaultAction.EditClick(
                        cipherId = "mockId-$number",
                        requiresPasswordReprompt = true,
                    ),
                    ListingItemOverflowAction.VaultAction.CopyNumberClick(
                        number = "mockNumber-$number",
                        requiresPasswordReprompt = true,
                    ),
                    ListingItemOverflowAction.VaultAction.CopySecurityCodeClick(
                        securityCode = "mockCode-$number",
                        cipherId = "mockId-$number",
                        requiresPasswordReprompt = true,
                    ),
                ),
                optionsTestTag = "CipherOptionsButton",
                isAutofill = false,
                isFido2Creation = false,
                shouldShowMasterPasswordReprompt = false,
                iconTestTag = "CardCipherIcon",
            )
        }

        CipherType.IDENTITY -> {
            VaultItemListingState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                titleTestTag = "CipherNameLabel",
                secondSubtitle = null,
                secondSubtitleTestTag = null,
                subtitle = subtitle,
                subtitleTestTag = "CipherSubTitleLabel",
                iconData = IconData.Local(R.drawable.ic_identity_item),
                extraIconList = listOf(
                    IconRes(
                        iconRes = R.drawable.ic_collection,
                        contentDescription = R.string.collections.asText(),
                        testTag = "CipherInCollectionIcon",
                    ),
                    IconRes(
                        iconRes = R.drawable.ic_attachment,
                        contentDescription = R.string.attachments.asText(),
                        testTag = "CipherWithAttachmentsIcon",
                    ),
                ),
                overflowOptions = listOf(
                    ListingItemOverflowAction.VaultAction.ViewClick(cipherId = "mockId-$number"),
                    ListingItemOverflowAction.VaultAction.EditClick(
                        cipherId = "mockId-$number",
                        requiresPasswordReprompt = true,
                    ),
                ),
                optionsTestTag = "CipherOptionsButton",
                isAutofill = false,
                isFido2Creation = false,
                shouldShowMasterPasswordReprompt = false,
                iconTestTag = "IdentityCipherIcon",
            )
        }
    }

/**
 * Create a mock [VaultItemListingState.DisplayItem] with a given [number].
 */
@Suppress("MaxLineLength")
fun createMockDisplayItemForSend(
    number: Int,
    sendType: SendType = SendType.FILE,
): VaultItemListingState.DisplayItem =
    when (sendType) {
        SendType.FILE -> {
            VaultItemListingState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                titleTestTag = "SendNameLabel",
                secondSubtitle = null,
                secondSubtitleTestTag = null,
                subtitle = "Oct 27, 2023, 12:00 PM",
                subtitleTestTag = "SendDateLabel",
                iconData = IconData.Local(R.drawable.ic_send_file),
                extraIconList = listOf(
                    IconRes(
                        iconRes = R.drawable.ic_send_password,
                        contentDescription = R.string.password.asText(),
                        testTag = "PasswordProtectedSendIcon",
                    ),
                    IconRes(
                        iconRes = R.drawable.ic_send_max_access_count_reached,
                        contentDescription = R.string.maximum_access_count_reached.asText(),
                        testTag = "MaxAccessSendIcon",
                    ),
                ),
                overflowOptions = listOf(
                    ListingItemOverflowAction.SendAction.EditClick(sendId = "mockId-$number"),
                    ListingItemOverflowAction.SendAction.CopyUrlClick(
                        sendUrl = "https://vault.bitwarden.com/#/send/mockAccessId-$number/mockKey-$number",
                    ),
                    ListingItemOverflowAction.SendAction.ShareUrlClick(
                        sendUrl = "https://vault.bitwarden.com/#/send/mockAccessId-$number/mockKey-$number",
                    ),
                    ListingItemOverflowAction.SendAction.RemovePasswordClick(sendId = "mockId-$number"),
                    ListingItemOverflowAction.SendAction.DeleteClick(sendId = "mockId-$number"),
                ),
                optionsTestTag = "SendOptionsButton",
                isAutofill = false,
                isFido2Creation = false,
                shouldShowMasterPasswordReprompt = false,
                iconTestTag = null,
            )
        }

        SendType.TEXT -> {
            VaultItemListingState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                titleTestTag = "SendNameLabel",
                secondSubtitle = null,
                secondSubtitleTestTag = null,
                subtitle = "Oct 27, 2023, 12:00 PM",
                subtitleTestTag = "SendDateLabel",
                iconData = IconData.Local(R.drawable.ic_send_text),
                extraIconList = listOf(
                    IconRes(
                        iconRes = R.drawable.ic_send_password,
                        contentDescription = R.string.password.asText(),
                        testTag = "PasswordProtectedSendIcon",
                    ),
                    IconRes(
                        iconRes = R.drawable.ic_send_max_access_count_reached,
                        contentDescription = R.string.maximum_access_count_reached.asText(),
                        testTag = "MaxAccessSendIcon",
                    ),
                ),
                overflowOptions = listOf(
                    ListingItemOverflowAction.SendAction.EditClick(sendId = "mockId-$number"),
                    ListingItemOverflowAction.SendAction.CopyUrlClick(
                        sendUrl = "https://vault.bitwarden.com/#/send/mockAccessId-$number/mockKey-$number",
                    ),
                    ListingItemOverflowAction.SendAction.ShareUrlClick(
                        sendUrl = "https://vault.bitwarden.com/#/send/mockAccessId-$number/mockKey-$number",
                    ),
                    ListingItemOverflowAction.SendAction.RemovePasswordClick(sendId = "mockId-$number"),
                    ListingItemOverflowAction.SendAction.DeleteClick(sendId = "mockId-$number"),
                ),
                optionsTestTag = "SendOptionsButton",
                isAutofill = false,
                isFido2Creation = false,
                shouldShowMasterPasswordReprompt = false,
                iconTestTag = null,
            )
        }
    }
