package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.core.CipherListView
import com.bitwarden.core.CipherRepromptType
import com.bitwarden.core.CipherType
import java.time.ZonedDateTime

/**
 * Create a mock [CipherListView] with a given [number].
 */
fun createMockCipherListView(number: Int): CipherListView =
    CipherListView(
        id = "mockId-$number",
        organizationId = "mockOrganizationId-$number",
        folderId = "mockFolderId-$number",
        collectionIds = listOf("mockCollectionId-$number"),
        name = "mockName-$number",
        type = CipherType.LOGIN,
        creationDate = ZonedDateTime
            .parse("2023-10-27T12:00:00Z")
            .toInstant(),
        deletedDate = ZonedDateTime
            .parse("2023-10-27T12:00:00Z")
            .toInstant(),
        revisionDate = ZonedDateTime
            .parse("2023-10-27T12:00:00Z")
            .toInstant(),
        attachments = 1U,
        favorite = false,
        reprompt = CipherRepromptType.NONE,
        edit = false,
        viewPassword = false,
        subTitle = "",
    )
