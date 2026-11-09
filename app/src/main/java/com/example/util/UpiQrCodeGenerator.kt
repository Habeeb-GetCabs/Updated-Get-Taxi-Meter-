package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.abs

/**
 * Offline-first ISO/IEC 18004 QR Code Generator & UPI Payment Helper.
 * Generates genuine, scannable QR codes locally on device without any external network calls.
 */
object UpiQrCodeGenerator {

    /**
     * Builds the standard NPCI UPI deep link URI:
     * upi://pay?pa=YOUR_UPI_ID&pn=DRIVER_NAME&am=FARE_AMOUNT&cu=INR
     */
    fun buildUpiUri(
        upiId: String,
        driverName: String,
        fareAmount: Double,
        currencyCode: String = "INR",
        transactionNote: String = ""
    ): String {
        val cleanUpi = upiId.trim()
        val cleanName = driverName.trim().ifBlank { "GET TAXI Driver" }
        val encodedPa = Uri.encode(cleanUpi)
        val encodedPn = Uri.encode(cleanName)

        return buildString {
            append("upi://pay?pa=")
            append(encodedPa)
            append("&pn=")
            append(encodedPn)
            if (fareAmount > 0.0) {
                append("&am=")
                append(String.format(Locale.US, "%.2f", fareAmount))
            }
            append("&cu=")
            append(currencyCode)
            if (transactionNote.isNotBlank()) {
                append("&tn=")
                append(Uri.encode(transactionNote.trim()))
            }
        }
    }

    /**
     * Generates a scannable QR Code Bitmap for the given text/URI.
     */
    fun generateQrCodeBitmap(
        content: String,
        sizePx: Int = 512,
        darkColor: Int = Color.rgb(15, 23, 42),
        lightColor: Int = Color.WHITE
    ): Bitmap {
        val matrix = encodeIsoQrMatrix(content.toByteArray(Charsets.UTF_8))
        val matrixSize = matrix.size
        val quietZone = 4
        val totalModules = matrixSize + quietZone * 2
        val bitmapSize = sizePx.coerceAtLeast(totalModules * 4)

        val bitmap = Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(lightColor)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = darkColor
            style = Paint.Style.FILL
        }

        val modulePixelSize = bitmapSize.toFloat() / totalModules.toFloat()

        for (r in 0 until matrixSize) {
            for (c in 0 until matrixSize) {
                if (matrix[r][c] == 1) {
                    val left = (c + quietZone) * modulePixelSize
                    val top = (r + quietZone) * modulePixelSize
                    canvas.drawRect(
                        left,
                        top,
                        left + modulePixelSize + 0.4f,
                        top + modulePixelSize + 0.4f,
                        paint
                    )
                }
            }
        }
        return bitmap
    }

    /**
     * Generates a complete, shareable UPI Payment Card Bitmap with the QR code,
     * final fare amount, UPI ID, driver name, and "GET TAXI" branding.
     */
    fun generatePaymentCardBitmap(
        upiId: String,
        driverName: String,
        vehicleNumber: String,
        fareAmount: Double,
        currencySymbol: String = "₹",
        tripNumber: String = ""
    ): Bitmap {
        val width = 720
        val height = 960
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Background
        canvas.drawColor(Color.rgb(248, 250, 252))

        // Main Card
        val cardRect = RectF(32f, 32f, width - 32f, height - 32f)
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(cardRect, 36f, 36f, paint)

        paint.color = Color.rgb(226, 232, 240)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRoundRect(cardRect, 36f, 36f, paint)
        paint.style = Paint.Style.FILL

        // Top Red Header Banner
        val headerRect = RectF(32f, 32f, width - 32f, 170f)
        paint.color = Color.rgb(229, 57, 53)
        canvas.drawRoundRect(headerRect, 36f, 36f, paint)
        canvas.drawRect(32f, 120f, width - 32f, 170f, paint)

        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 34f
        canvas.drawText("GET TAXI", width / 2f, 98f, paint)

        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("SCAN & PAY VIA ANY UPI APP (GPay • PhonePe • Paytm)", width / 2f, 138f, paint)

        // Fare Display
        var currentY = 235f
        if (fareAmount > 0.0) {
            paint.color = Color.rgb(100, 116, 139)
            paint.textSize = 18f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("FINAL BILL AMOUNT", width / 2f, currentY, paint)

            currentY += 62f
            paint.color = Color.rgb(15, 23, 42)
            paint.textSize = 58f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("$currencySymbol${String.format(Locale.US, "%.2f", fareAmount)}", width / 2f, currentY, paint)
            currentY += 30f
        } else {
            currentY += 20f
        }

        // QR Code Box
        val qrSize = 420
        val upiUri = buildUpiUri(
            upiId = upiId,
            driverName = driverName,
            fareAmount = fareAmount,
            transactionNote = if (tripNumber.isNotBlank()) "Trip $tripNumber" else "Taxi Fare"
        )
        val qrBitmap = generateQrCodeBitmap(upiUri, qrSize)
        val qrLeft = (width - qrSize) / 2f
        val qrTop = currentY + 10f

        val qrBorderRect = RectF(qrLeft - 14f, qrTop - 14f, qrLeft + qrSize + 14f, qrTop + qrSize + 14f)
        paint.color = Color.rgb(226, 232, 240)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRoundRect(qrBorderRect, 24f, 24f, paint)
        paint.style = Paint.Style.FILL

        canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null)

        currentY = qrTop + qrSize + 52f

        // UPI ID & Driver Details
        paint.color = Color.rgb(229, 57, 53)
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("UPI ID: ${upiId.trim()}", width / 2f, currentY, paint)

        currentY += 34f
        paint.color = Color.rgb(51, 65, 85)
        paint.textSize = 19f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val drvLabel = buildString {
            append(driverName.ifBlank { "Licensed Driver" })
            if (vehicleNumber.isNotBlank()) {
                append(" • ")
                append(vehicleNumber)
            }
        }
        canvas.drawText(drvLabel, width / 2f, currentY, paint)

        if (tripNumber.isNotBlank()) {
            currentY += 28f
            paint.color = Color.rgb(100, 116, 139)
            paint.textSize = 16f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Trip ID: $tripNumber", width / 2f, currentY, paint)
        }

        // Bottom Credit
        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Designed & Developed by Basheer", width / 2f, height - 54f, paint)

        return bitmap
    }

    /**
     * Saves the generated QR Payment Card Bitmap into the device Gallery (Pictures/TaxiMeterPro).
     */
    fun saveQrBitmapToGallery(context: Context, bitmap: Bitmap, fileName: String = "TaxiMeterPro_QR_${System.currentTimeMillis()}.png"): Boolean {
        return try {
            val cleanName = if (fileName.endsWith(".png")) fileName else "$fileName.png"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, cleanName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/TaxiMeterPro")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
                resolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                true
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "TaxiMeterPro")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, cleanName)
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                true
            }
        } catch (e: Exception) {
            android.util.Log.e("UpiQrCodeGenerator", "Failed saving QR to gallery", e)
            false
        }
    }

    /**
     * Shares the QR Payment Card image along with optional bill text via WhatsApp, Telegram, SMS, etc.
     */
    fun shareQrBitmap(context: Context, bitmap: Bitmap, captionText: String, title: String = "Share Payment QR & Bill") {
        try {
            val file = File(context.cacheDir, "TaxiMeterPro_Payment_QR.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                if (captionText.isNotBlank()) {
                    putExtra(Intent.EXTRA_TEXT, captionText)
                }
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            android.util.Log.e("UpiQrCodeGenerator", "Failed sharing QR bitmap", e)
        }
    }

    // =========================================================================
    // ISO/IEC 18004 QR Code Encoder (Byte Mode, ECC Level L, Versions 1..10)
    // =========================================================================

    private data class VersionSpec(
        val version: Int,
        val totalDataCodewords: Int,
        val ecCodewordsPerBlock: Int,
        val numBlocksGroup1: Int,
        val dataCodewordsGroup1: Int,
        val numBlocksGroup2: Int = 0,
        val dataCodewordsGroup2: Int = 0,
        val alignmentCenters: IntArray = intArrayOf()
    )

    // Official ISO/IEC 18004 Table 9 for Error Correction Level L (Versions 1 to 10)
    private val VERSION_SPECS_L = arrayOf(
        VersionSpec(1, 19, 7, 1, 19, 0, 0, intArrayOf()),
        VersionSpec(2, 34, 10, 1, 34, 0, 0, intArrayOf(6, 18)),
        VersionSpec(3, 55, 15, 1, 55, 0, 0, intArrayOf(6, 22)),
        VersionSpec(4, 80, 20, 1, 80, 0, 0, intArrayOf(6, 26)),
        VersionSpec(5, 108, 26, 1, 108, 0, 0, intArrayOf(6, 30)),
        VersionSpec(6, 136, 18, 2, 68, 0, 0, intArrayOf(6, 34)),
        VersionSpec(7, 156, 20, 2, 78, 0, 0, intArrayOf(6, 22, 38)),
        VersionSpec(8, 194, 24, 2, 97, 0, 0, intArrayOf(6, 24, 42)),
        VersionSpec(9, 232, 30, 2, 116, 0, 0, intArrayOf(6, 26, 46)),
        VersionSpec(10, 274, 18, 2, 68, 2, 69, intArrayOf(6, 28, 50))
    )

    internal fun encodeIsoQrMatrix(payload: ByteArray): Array<IntArray> {
        // Determine minimum version (1..10) that fits Byte mode payload
        val dataBytes = if (payload.size > 271) payload.copyOf(271) else payload
        val spec = VERSION_SPECS_L.firstOrNull { v ->
            val lenBits = if (v.version <= 9) 8 else 16
            val availableBits = v.totalDataCodewords * 8
            4 + lenBits + dataBytes.size * 8 <= availableBits
        } ?: VERSION_SPECS_L.last()

        val version = spec.version
        val size = 17 + version * 4

        // 1. Build Bit Stream (Byte Mode = 0100)
        val bits = ArrayList<Int>(spec.totalDataCodewords * 8)
        fun appendBits(value: Int, count: Int) {
            for (i in count - 1 downTo 0) {
                bits.add((value ushr i) and 1)
            }
        }

        appendBits(0b0100, 4) // Byte mode indicator
        val countBits = if (version <= 9) 8 else 16
        appendBits(dataBytes.size, countBits)
        for (b in dataBytes) {
            appendBits(b.toInt() and 0xFF, 8)
        }

        val maxBits = spec.totalDataCodewords * 8
        val terminatorCount = (maxBits - bits.size).coerceIn(0, 4)
        appendBits(0, terminatorCount)

        while (bits.size % 8 != 0) {
            bits.add(0)
        }

        val dataCodewords = IntArray(spec.totalDataCodewords)
        val currentBytes = bits.size / 8
        for (i in 0 until currentBytes) {
            var byteVal = 0
            for (bitIdx in 0 until 8) {
                byteVal = (byteVal shl 1) or bits[i * 8 + bitIdx]
            }
            dataCodewords[i] = byteVal
        }

        val padBytes = intArrayOf(0xEC, 0x11)
        for (i in currentBytes until spec.totalDataCodewords) {
            dataCodewords[i] = padBytes[(i - currentBytes) % 2]
        }

        // 2. Split into blocks and compute Reed-Solomon EC codewords
        val dataBlocks = ArrayList<IntArray>()
        val ecBlocks = ArrayList<IntArray>()
        var offset = 0

        for (b in 0 until spec.numBlocksGroup1) {
            val block = dataCodewords.copyOfRange(offset, offset + spec.dataCodewordsGroup1)
            offset += spec.dataCodewordsGroup1
            dataBlocks.add(block)
            ecBlocks.add(computeReedSolomonEc(block, spec.ecCodewordsPerBlock))
        }
        for (b in 0 until spec.numBlocksGroup2) {
            val block = dataCodewords.copyOfRange(offset, offset + spec.dataCodewordsGroup2)
            offset += spec.dataCodewordsGroup2
            dataBlocks.add(block)
            ecBlocks.add(computeReedSolomonEc(block, spec.ecCodewordsPerBlock))
        }

        // Interleave data and EC codewords
        val finalCodewords = ArrayList<Int>()
        val maxDataBlockLen = maxOf(spec.dataCodewordsGroup1, spec.dataCodewordsGroup2)
        for (i in 0 until maxDataBlockLen) {
            for (block in dataBlocks) {
                if (i < block.size) finalCodewords.add(block[i])
            }
        }
        for (i in 0 until spec.ecCodewordsPerBlock) {
            for (ec in ecBlocks) {
                if (i < ec.size) finalCodewords.add(ec[i])
            }
        }

        val finalBits = ArrayList<Int>(finalCodewords.size * 8 + 7)
        for (cw in finalCodewords) {
            for (i in 7 downTo 0) {
                finalBits.add((cw ushr i) and 1)
            }
        }
        // Remainder bits for versions 2..6
        val remainderBits = if (version in 2..6) 7 else 0
        repeat(remainderBits) { finalBits.add(0) }

        // 3. Construct Matrix & Function Patterns
        val matrix = Array(size) { IntArray(size) }
        val isFunction = Array(size) { BooleanArray(size) }

        fun setFunctionModule(r: Int, c: Int, isDark: Boolean) {
            if (r in 0 until size && c in 0 until size) {
                matrix[r][c] = if (isDark) 1 else 0
                isFunction[r][c] = true
            }
        }

        // Finder patterns + separators
        fun drawFinderPattern(topR: Int, leftC: Int) {
            for (dr in -1..7) {
                for (dc in -1..7) {
                    val r = topR + dr
                    val c = leftC + dc
                    if (r in 0 until size && c in 0 until size) {
                        val inOuter = dr in 0..6 && dc in 0..6 && (dr == 0 || dr == 6 || dc == 0 || dc == 6)
                        val inInner = dr in 2..4 && dc in 2..4
                        setFunctionModule(r, c, inOuter || inInner)
                    }
                }
            }
        }

        drawFinderPattern(0, 0)
        drawFinderPattern(0, size - 7)
        drawFinderPattern(size - 7, 0)

        // Timing patterns
        for (i in 8 until size - 8) {
            val dark = (i % 2 == 0)
            setFunctionModule(6, i, dark)
            setFunctionModule(i, 6, dark)
        }

        // Alignment patterns
        val centers = spec.alignmentCenters
        for (cr in centers) {
            for (cc in centers) {
                // Skip if overlapping with finder patterns
                if ((cr == 6 && cc == 6) ||
                    (cr == 6 && cc == size - 7) ||
                    (cr == size - 7 && cc == 6)
                ) continue
                for (dr in -2..2) {
                    for (dc in -2..2) {
                        val dist = maxOf(abs(dr), abs(dc))
                        setFunctionModule(cr + dr, cc + dc, dist != 1)
                    }
                }
            }
        }

        // Dark module
        setFunctionModule(4 * version + 9, 8, true)

        // Reserve Format Information areas
        for (i in 0..8) {
            if (!isFunction[8][i]) setFunctionModule(8, i, false)
            if (!isFunction[i][8]) setFunctionModule(i, 8, false)
        }
        for (i in 0..7) {
            if (!isFunction[8][size - 1 - i]) setFunctionModule(8, size - 1 - i, false)
            if (!isFunction[size - 1 - i][8]) setFunctionModule(size - 1 - i, 8, false)
        }

        // Reserve Version Information areas (for version >= 7)
        if (version >= 7) {
            var vInfo = version
            for (i in 0 until 12) {
                vInfo = (vInfo shl 1) xor ((vInfo ushr 11) * 0x1F25)
            }
            val versionBits = (version shl 12) or vInfo
            var bitIndex = 0
            for (i in 0 until 6) {
                for (j in 0 until 3) {
                    val dark = ((versionBits ushr bitIndex) and 1) == 1
                    bitIndex++
                    setFunctionModule(size - 11 + j, i, dark)
                    setFunctionModule(i, size - 11 + j, dark)
                }
            }
        }

        // 4. Zigzag Data Placement
        var bitIdx = 0
        var upward = true
        var col = size - 1
        while (col > 0) {
            if (col == 6) col-- // Skip vertical timing line
            for (step in 0 until size) {
                val row = if (upward) size - 1 - step else step
                for (cOffset in 0..1) {
                    val c = col - cOffset
                    if (!isFunction[row][c]) {
                        val bit = if (bitIdx < finalBits.size) finalBits[bitIdx++] else 0
                        matrix[row][c] = bit
                    }
                }
            }
            upward = !upward
            col -= 2
        }

        // 5. Apply Mask 0 ((row + col) % 2 == 0) and write Format Information bits
        val maskPattern = 0
        for (r in 0 until size) {
            for (c in 0 until size) {
                if (!isFunction[r][c]) {
                    if ((r + c) % 2 == 0) {
                        matrix[r][c] = matrix[r][c] xor 1
                    }
                }
            }
        }

        // Format Information for ECC Level L (01) and Mask 0 (000) -> data = 01000
        val formatData = (0b01 shl 3) or maskPattern
        var rem = formatData
        for (i in 0 until 10) {
            rem = (rem shl 1) xor ((rem ushr 9) * 0x537)
        }
        val formatBits = ((formatData shl 10) or rem) xor 0x5412

        fun getFormatBit(idx: Int): Int = (formatBits ushr idx) and 1

        // Place Format Bits around top-left finder
        for (i in 0..5) matrix[8][i] = getFormatBit(i)
        matrix[8][7] = getFormatBit(6)
        matrix[8][8] = getFormatBit(7)
        matrix[7][8] = getFormatBit(8)
        for (i in 9..14) matrix[14 - i][8] = getFormatBit(i)

        // Place Format Bits along top-right and bottom-left
        for (i in 0..7) matrix[8][size - 1 - i] = getFormatBit(i)
        for (i in 8..14) matrix[size - 15 + i][8] = getFormatBit(i)

        return matrix
    }

    // GF(256) with primitive polynomial 0x11D
    private val GF_EXP = IntArray(512)
    private val GF_LOG = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            GF_EXP[i] = x
            GF_LOG[x] = i
            x = x shl 1
            if ((x and 0x100) != 0) {
                x = x xor 0x11D
            }
        }
        for (i in 255 until 512) {
            GF_EXP[i] = GF_EXP[i - 255]
        }
    }

    private fun gfMultiply(a: Int, b: Int): Int {
        if (a == 0 || b == 0) return 0
        return GF_EXP[GF_LOG[a] + GF_LOG[b]]
    }

    private fun computeReedSolomonEc(data: IntArray, ecCount: Int): IntArray {
        // Build generator polynomial
        var gen = IntArray(1) { 1 }
        for (i in 0 until ecCount) {
            val next = IntArray(gen.size + 1)
            val root = GF_EXP[i]
            for (j in gen.indices) {
                next[j] = next[j] xor gen[j]
                next[j + 1] = next[j + 1] xor gfMultiply(gen[j], root)
            }
            gen = next
        }

        val remainder = IntArray(ecCount)
        for (byteVal in data) {
            val factor = byteVal xor remainder[0]
            System.arraycopy(remainder, 1, remainder, 0, ecCount - 1)
            remainder[ecCount - 1] = 0
            for (j in 0 until ecCount) {
                remainder[j] = remainder[j] xor gfMultiply(gen[j + 1], factor)
            }
        }
        return remainder
    }
}
