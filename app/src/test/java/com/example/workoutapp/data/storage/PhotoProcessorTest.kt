package com.example.workoutapp.data.storage

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException

@RunWith(RobolectricTestRunner::class)
class PhotoProcessorTest {

    private val context: Context = mockk(relaxed = true)
    private val resolver: ContentResolver = mockk(relaxed = true)
    private val processor = PhotoProcessor(context)

    @org.junit.Before
    fun setup() {
        every { context.contentResolver } returns resolver
    }

    @Test
    fun `compressToJpeg returns bytes that decode back as a JPEG image`() = runTest {
        val source = Uri.parse("content://media/external/images/42")
        val original = makeTestJpeg(width = 4000, height = 3000)
        every { resolver.openInputStream(source) } returns ByteArrayInputStream(original)

        val bytes = processor.compressToJpeg(source, maxEdgePx = 1080, quality = 85)

        assertNotNull(bytes)
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertNotNull("Result should be a valid JPEG", decoded)
        assertTrue(
            "Longest edge must be <= maxEdgePx but was ${maxOf(decoded!!.width, decoded.height)}",
            maxOf(decoded.width, decoded.height) <= 1080
        )
    }

    @Test
    fun `compressToJpeg shrinks a 12 megapixel input to a small payload`() = runTest {
        val source = Uri.parse("content://media/external/images/42")
        val original = makeTestJpeg(width = 4000, height = 3000)
        every { resolver.openInputStream(source) } returns ByteArrayInputStream(original)

        val bytes = processor.compressToJpeg(source, maxEdgePx = 1080, quality = 85)

        assertTrue(
            "Expected 1080p JPEG re-encode to be well under 1 MB, was ${bytes.size} bytes",
            bytes.size < 1_000_000
        )
    }

    @Test
    fun `compressToJpeg throws SourceUnreadableException when ContentResolver returns null stream`() = runTest {
        val source = Uri.parse("content://media/external/images/missing")
        every { resolver.openInputStream(source) } returns null

        try {
            processor.compressToJpeg(source)
            fail("Expected SourceUnreadableException")
        } catch (e: SourceUnreadableException) {
            // expected
        }
    }

    @Test
    fun `compressToJpeg throws SourceUnreadableException when ContentResolver throws FileNotFoundException`() = runTest {
        val source = Uri.parse("content://media/external/images/missing")
        every { resolver.openInputStream(source) } throws FileNotFoundException("gone")

        try {
            processor.compressToJpeg(source)
            fail("Expected SourceUnreadableException")
        } catch (e: SourceUnreadableException) {
            // expected
        }
    }

    private fun makeTestJpeg(width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        bitmap.recycle()
        return out.toByteArray()
    }
}
