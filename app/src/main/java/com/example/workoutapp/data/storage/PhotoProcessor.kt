package com.example.workoutapp.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class PhotoProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun compressToJpeg(
        source: Uri,
        maxEdgePx: Int = 1080,
        quality: Int = 85
    ): ByteArray = withContext(Dispatchers.IO) {
        val stream = try {
            context.contentResolver.openInputStream(source)
                ?: throw SourceUnreadableException("ContentResolver returned null for $source")
        } catch (e: SourceUnreadableException) {
            throw e
        } catch (e: IOException) {
            throw SourceUnreadableException("Could not open $source: ${e.message}").initCauseCompat(e)
        }

        stream.use { input ->
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, bounds)

            val (rawW, rawH) = bounds.outWidth to bounds.outHeight
            require(rawW > 0 && rawH > 0) { "Image bounds invalid: ${rawW}x$rawH" }

            val inSampleSize = computeInSampleSize(rawW, rawH, maxEdgePx)
            val decodeOpts = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val decoded = BitmapFactory.decodeStream(
                context.contentResolver.openInputStream(source)
                    ?: throw SourceUnreadableException("ContentResolver returned null on re-open of $source"),
                null,
                decodeOpts
            ) ?: throw SourceUnreadableException("BitmapFactory returned null for $source")

            val scaled = scaleDownIfNeeded(decoded, maxEdgePx)
            val out = java.io.ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            scaled.recycle()
            if (scaled !== decoded) decoded.recycle()
            out.toByteArray()
        }
    }

    private fun computeInSampleSize(width: Int, height: Int, maxEdgePx: Int): Int {
        var sample = 1
        val longest = max(width, height)
        while (longest / (sample * 2) >= maxEdgePx) {
            sample *= 2
        }
        return sample
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxEdgePx: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxEdgePx) return bitmap
        val ratio = maxEdgePx.toFloat() / longest.toFloat()
        val targetW = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val targetH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
    }

    private fun <E : Throwable> E.initCauseCompat(cause: Throwable): E {
        return try {
            initCause(cause); this
        } catch (_: IllegalStateException) {
            this
        } catch (_: IllegalArgumentException) {
            this
        }
    }
}
