package com.rishabh.gymbros.features.cloud

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudRestorePromptTest {
    @Test fun existingBackupOffersRestoreAfterSignIn() {
        assertTrue(CloudBackupState(signedIn = true, cloudBackupAvailable = true).shouldSuggestRestore)
    }

    @Test fun noPromptWithoutAnAccountOrBackup() {
        assertFalse(CloudBackupState(cloudBackupAvailable = true).shouldSuggestRestore)
        assertFalse(CloudBackupState(signedIn = true).shouldSuggestRestore)
    }

    @Test fun waitForBackupCheckAndRespectNotNow() {
        val existing = CloudBackupState(signedIn = true, cloudBackupAvailable = true)
        assertFalse(existing.copy(backupCheckPending = true).shouldSuggestRestore)
        assertFalse(existing.copy(restoreSuggestionDismissed = true).shouldSuggestRestore)
    }
}
