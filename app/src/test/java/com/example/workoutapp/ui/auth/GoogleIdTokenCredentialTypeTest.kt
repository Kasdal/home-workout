package com.example.workoutapp.ui.auth

import android.os.Bundle
import androidx.credentials.CustomCredential
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GoogleIdTokenCredentialTypeTest {

    @Test
    fun `accepts the documented google id token credential type`() {
        val credential = CustomCredential(
            "com.google.android.libraries.identity.googleid.TYPE_GOOGLE_ID_TOKEN",
            Bundle()
        )

        assertTrue(isGoogleIdTokenCredential(credential))
    }

    @Test
    fun `accepts the option type string returned by newer gms builds`() {
        val credential = CustomCredential(
            "com.google.android.libraries.identity.googleid.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL",
            Bundle()
        )

        assertTrue(isGoogleIdTokenCredential(credential))
    }

    @Test
    fun `rejects other credential types`() {
        val credential = CustomCredential("com.example.other.CREDENTIAL", Bundle())

        assertFalse(isGoogleIdTokenCredential(credential))
    }
}
