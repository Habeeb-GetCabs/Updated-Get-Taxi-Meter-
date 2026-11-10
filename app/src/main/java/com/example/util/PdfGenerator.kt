package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.database.TripEntity
import com.example.data.model.ExtraChargeItem
import com.example.data.model.ServiceType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfGenerator {

    private const val PAGE_WIDTH = 595 // A4 standard width in points
    private const val PAGE_HEIGHT = 842 // A4 standard height in points
    private const val MARGIN = 36f

    data class InvoiceVendorHeaderMetadata(
        val vendorId: String,
        val vendorName: String,
        val vendorLogoAsset: String,
        val invoiceNumber: String,
        val tripNumber: String,
        val displayedBusinessLines: List<String>
    )

    /**
     * Resolves the immutable vendor snapshot stored on the TripEntity without fabricating
     * any unconfigured contact, address, email, tax, or legal fields.
     */
    fun resolveVendorHeaderMetadata(trip: TripEntity): InvoiceVendorHeaderMetadata {
        val brandName = trip.vendorName.ifBlank { VendorLogoRenderer.DEFAULT_VENDOR_NAME }
        val vendorId = trip.vendorId.ifBlank { VendorLogoRenderer.slugForVendorName(brandName) }
        val logoAsset = trip.vendorLogoAsset.ifBlank {
            VendorLogoRenderer.assetPathForVendor(vendorId, brandName)
        }
        val tripNumDisplay = if (trip.tripNumber.isNotBlank()) {
            trip.tripNumber
        } else {
            String.format(
                Locale.US,
                "GTM-%s-%04d",
                SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(trip.startTime)),
                trip.id
            )
        }
        val invoiceNo = trip.vendorInvoiceNumber.ifBlank {
            VendorLogoRenderer.buildVendorInvoiceNumber(brandName, tripNumDisplay)
        }

        // Only include fields that actually exist on the trip's vendor snapshot — never fabricate
        val businessLines = mutableListOf<String>()
        if (trip.vendorBusinessAddress.isNotBlank()) {
            businessLines.add(trip.vendorBusinessAddress.trim())
        }
        val contactParts = mutableListOf<String>()
        if (trip.vendorContactNumber.isNotBlank()) {
            contactParts.add("Tel: ${trip.vendorContactNumber.trim()}")
        }
        if (trip.vendorEmail.isNotBlank()) {
            contactParts.add("Email: ${trip.vendorEmail.trim()}")
        }
        if (contactParts.isNotEmpty()) {
            businessLines.add(contactParts.joinToString(" • "))
        }
        if (trip.vendorGstin.isNotBlank()) {
            businessLines.add("GSTIN / Tax ID: ${trip.vendorGstin.trim()}")
        }
        if (trip.vendorInvoiceDetails.isNotBlank()) {
            businessLines.add(trip.vendorInvoiceDetails.trim())
        }

        return InvoiceVendorHeaderMetadata(
            vendorId = vendorId,
            vendorName = brandName,
            vendorLogoAsset = logoAsset,
            invoiceNumber = invoiceNo,
            tripNumber = tripNumDisplay,
            displayedBusinessLines = businessLines
        )
    }

    fun generateCustomerInvoicePdf(context: Context, trip: TripEntity): File {
        val vendorMeta = resolveVendorHeaderMetadata(trip)
        val safeSlug = vendorMeta.vendorId.uppercase(Locale.US).replace(Regex("[^A-Z0-9_]"), "_")
        val file = File(context.cacheDir, "${safeSlug}_Customer_Invoice_${trip.id}.pdf")

        val pdfDocument = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            drawCustomerInvoiceCanvas(context, page.canvas, trip, vendorMeta)
            pdfDocument.finishPage(page)
            FileOutputStream(file).use { pdfDocument.writeTo(it) }
        } catch (_: IllegalStateException) {
            val fallbackBitmap = Bitmap.createBitmap(PAGE_WIDTH, PAGE_HEIGHT, Bitmap.Config.ARGB_8888)
            drawCustomerInvoiceCanvas(context, Canvas(fallbackBitmap), trip, vendorMeta)
            file.writeText("%PDF-1.4\n% ${vendorMeta.vendorName} | ${vendorMeta.invoiceNumber} | ${vendorMeta.tripNumber}\n%%EOF\n")
        } finally {
            runCatching { pdfDocument.close() }
        }
        return file
    }

    private fun drawCustomerInvoiceCanvas(
        context: Context,
        canvas: Canvas,
        trip: TripEntity,
        vendorMeta: InvoiceVendorHeaderMetadata
    ) {
        val paint = Paint().apply { isAntiAlias = true }
        val textPaint = TextPaint().apply {
            textSize = 9.5f
            color = Color.rgb(30, 41, 59)
            isAntiAlias = true
        }

        var currentY = MARGIN

        // 1. TOP HEADER BAR
        paint.color = Color.rgb(202, 31, 47) // #CA1F2F Brand Red
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 12f, paint)

        // 2. VENDOR BRANDING & LOGO SECTION (FROM TRIP SNAPSHOT)
        currentY += 10f
        val logoBitmap = VendorLogoRenderer.renderVendorLogoBitmap(
            context = context,
            vendorId = vendorMeta.vendorId,
            vendorName = vendorMeta.vendorName,
            widthPx = 320,
            heightPx = 122
        )
        val logoDestRect = RectF(MARGIN, currentY, MARGIN + 102f, currentY + 39f)
        canvas.drawBitmap(logoBitmap, null, logoDestRect, Paint(Paint.FILTER_BITMAP_FLAG))

        val brandTextX = MARGIN + 112f
        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        if (vendorMeta.displayedBusinessLines.isEmpty()) {
            canvas.drawText("${vendorMeta.vendorName} • Official Passenger Trip Invoice", brandTextX, currentY + 16f, paint)
        } else {
            var lineOffset = 14f
            vendorMeta.displayedBusinessLines.take(3).forEach { line ->
                canvas.drawText(line, brandTextX, currentY + lineOffset, paint)
                lineOffset += 11f
            }
        }

        paint.textAlign = Paint.Align.RIGHT
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CUSTOMER INVOICE", PAGE_WIDTH - MARGIN, currentY + 12f, paint)

        paint.textSize = 9f
        paint.color = Color.rgb(71, 85, 105)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Invoice No: ${vendorMeta.invoiceNumber}", PAGE_WIDTH - MARGIN, currentY + 25f, paint)
        canvas.drawText("Trip ID: ${vendorMeta.tripNumber}", PAGE_WIDTH - MARGIN, currentY + 36f, paint)

        val dateFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val startTimeStr = timeFormatter.format(Date(trip.startTime))
        val endTimeStr = if (trip.endTime > 0) timeFormatter.format(Date(trip.endTime)) else timeFormatter.format(Date(trip.startTime + (trip.durationSeconds * 1000)))

        val displayInvoiceDate = if (trip.invoiceDateText.isNotBlank()) {
            if (trip.invoiceTimeText.isNotBlank()) "${trip.invoiceDateText}, ${trip.invoiceTimeText}" else trip.invoiceDateText
        } else if (trip.invoiceTimestamp > 0) {
            dateFormatter.format(Date(trip.invoiceTimestamp))
        } else {
            dateFormatter.format(Date(trip.startTime))
        }
        canvas.drawText("Invoice Date: $displayInvoiceDate", PAGE_WIDTH - MARGIN, currentY + 47f, paint)

        val mode = ServiceType.fromId(trip.serviceType)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = when (mode) {
            ServiceType.RENTAL -> Color.rgb(180, 83, 9)
            ServiceType.OUTSTATION -> Color.rgb(67, 56, 202)
            ServiceType.LOCAL -> Color.rgb(202, 31, 47)
        }
        canvas.drawText("Service Mode: ${mode.title}", PAGE_WIDTH - MARGIN, currentY + 59f, paint)
        paint.textAlign = Paint.Align.LEFT

        currentY += 68f

        // Thin Separator Line
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 1f
        canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, paint)

        currentY += 16f

        // 3. TWO COLUMN SUMMARY CARDS: Customer Details & Driver Details
        val colWidth = (PAGE_WIDTH - (MARGIN * 2) - 16f) / 2f
        val cardHeight = 90f

        // Customer Details Box
        drawRoundedCard(canvas, MARGIN, currentY, colWidth, cardHeight, Color.rgb(248, 250, 252), Color.rgb(226, 232, 240))
        paint.color = Color.rgb(202, 31, 47)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CUSTOMER DETAILS", MARGIN + 12f, currentY + 16f, paint)

        val custName = if (trip.customerName.isNotBlank()) trip.customerName else "Passenger (Direct Hire)"
        val custMobile = if (trip.customerMobile.isNotBlank()) trip.customerMobile else "-"
        val custAddr = if (trip.customerAddress.isNotBlank()) trip.customerAddress else "-"

        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(custName, MARGIN + 12f, currentY + 32f, paint)

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Mobile: $custMobile", MARGIN + 12f, currentY + 48f, paint)
        drawWrappedText(canvas, textPaint, "Address: $custAddr", MARGIN + 12f, currentY + 62f, colWidth - 24f, 2)

        // Driver & Vehicle Box
        val driverX = MARGIN + colWidth + 16f
        drawRoundedCard(canvas, driverX, currentY, colWidth, cardHeight, Color.rgb(248, 250, 252), Color.rgb(226, 232, 240))
        paint.color = Color.rgb(14, 165, 233)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DRIVER & VEHICLE DETAILS", driverX + 12f, currentY + 16f, paint)

        val drvName = if (trip.driverName.isNotBlank()) trip.driverName else "Licensed Driver"
        val drvMobile = if (trip.driverMobile.isNotBlank()) trip.driverMobile else "-"
        val vehNo = if (trip.vehicleNumber.isNotBlank()) trip.vehicleNumber else "Taxi"
        val vehTypeModel = buildString {
            if (trip.vehicleType.isNotBlank()) append(trip.vehicleType)
            if (trip.vehicleModel.isNotBlank()) {
                if (isNotEmpty()) append(" • ")
                append(trip.vehicleModel)
            }
        }.ifEmpty { "Commercial Taxi" }

        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(drvName, driverX + 12f, currentY + 32f, paint)

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Vehicle Reg: $vehNo", driverX + 12f, currentY + 48f, paint)
        canvas.drawText("Type / Model: $vehTypeModel", driverX + 12f, currentY + 62f, paint)
        if (drvMobile != "-") {
            canvas.drawText("Contact: $drvMobile", driverX + 12f, currentY + 76f, paint)
        }

        currentY += cardHeight + 14f

        // 4. ROUTE & TRIP METRICS SECTION
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TRIP JOURNEY & METRICS", MARGIN, currentY, paint)

        currentY += 6f
        val routeBoxHeight = 94f
        drawRoundedCard(canvas, MARGIN, currentY, PAGE_WIDTH - (MARGIN * 2), routeBoxHeight, Color.WHITE, Color.rgb(226, 232, 240))

        val pAddr = if (trip.pickupAddress.isNotBlank()) trip.pickupAddress else "GPS Location"
        val dAddr = if (trip.dropAddress.isNotBlank()) trip.dropAddress else "GPS Location"

        paint.color = Color.rgb(34, 197, 94)
        canvas.drawCircle(MARGIN + 16f, currentY + 20f, 4f, paint)
        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PICKUP ($startTimeStr):", MARGIN + 26f, currentY + 22f, paint)
        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        drawWrappedText(canvas, textPaint, pAddr, MARGIN + 140f, currentY + 22f, PAGE_WIDTH - MARGIN - 160f, 1)

        paint.color = Color.rgb(202, 31, 47)
        canvas.drawCircle(MARGIN + 16f, currentY + 42f, 4f, paint)
        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DROP-OFF ($endTimeStr):", MARGIN + 26f, currentY + 44f, paint)
        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        drawWrappedText(canvas, textPaint, dAddr, MARGIN + 140f, currentY + 44f, PAGE_WIDTH - MARGIN - 160f, 1)

        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRect(MARGIN + 8f, currentY + 58f, PAGE_WIDTH - MARGIN - 8f, currentY + 86f, paint)

        val durationMin = trip.durationSeconds / 60
        val durationSec = trip.durationSeconds % 60
        val waitMin = trip.waitingSeconds / 60
        val waitSec = trip.waitingSeconds % 60

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        val metricColW = (PAGE_WIDTH - (MARGIN * 2) - 16f) / 3f
        canvas.drawText("Distance: ", MARGIN + 16f, currentY + 74f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawText("${String.format(Locale.US, "%.2f", trip.distanceKm)} km", MARGIN + 68f, currentY + 74f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.rgb(71, 85, 105)
        canvas.drawText("Actual Duration: ", MARGIN + metricColW + 16f, currentY + 74f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawText("${durationMin}m ${durationSec}s", MARGIN + metricColW + 90f, currentY + 74f, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.rgb(71, 85, 105)
        val metricLabel3 = if (mode == ServiceType.RENTAL) "Billable Hours: " else "Waiting Delay: "
        canvas.drawText(metricLabel3, MARGIN + (metricColW * 2) + 16f, currentY + 74f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = Color.rgb(15, 23, 42)
        val metricValue3 = if (mode == ServiceType.RENTAL) "${trip.billableHours} hr (5-min grace rule)" else "${waitMin}m ${waitSec}s"
        canvas.drawText(metricValue3, MARGIN + (metricColW * 2) + 84f, currentY + 74f, paint)

        currentY += routeBoxHeight + 16f

        // 5. ITEMIZED CHARGES TABLE (SERVICE SPECIFIC)
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ITEMIZED FARE BREAKDOWN (TARIFF APPLIED)", MARGIN, currentY, paint)

        currentY += 8f

        paint.color = Color.rgb(15, 23, 42)
        canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 24f, paint)

        paint.color = Color.WHITE
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DESCRIPTION", MARGIN + 12f, currentY + 16f, paint)
        canvas.drawText("RATE / DETAILS", MARGIN + 220f, currentY + 16f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("AMOUNT (${trip.currency})", PAGE_WIDTH - MARGIN - 12f, currentY + 16f, paint)
        paint.textAlign = Paint.Align.LEFT

        currentY += 24f
        val tableTopY = currentY

        val itemizedExtras = ExtraChargeItem.deserialize(trip.extraChargesJson)
        val itemizedExtrasSum = ExtraChargeItem.totalAmount(itemizedExtras)

        when (mode) {
            ServiceType.LOCAL -> {
                val distCharge = trip.distanceKm * trip.farePerKm
                val waitMinutes = trip.waitingSeconds / 60.0
                val waitCharge = waitMinutes * trip.waitFarePerMin

                drawTableRow(canvas, paint, "Base Fare (Flag Drop)", "1 Fixed Flag Drop Charge", String.format(Locale.US, "%.2f", trip.baseFare), MARGIN, currentY, isEven = false)
                currentY += 22f

                val distUnit = "${String.format(Locale.US, "%.2f", trip.distanceKm)} km @ ${trip.currency}${String.format(Locale.US, "%.2f", trip.farePerKm)}/km"
                drawTableRow(canvas, paint, "Distance Fare", distUnit, String.format(Locale.US, "%.2f", distCharge), MARGIN, currentY, isEven = true)
                currentY += 22f

                val waitUnit = "${waitMin}m ${waitSec}s @ ${trip.currency}${String.format(Locale.US, "%.2f", trip.waitFarePerMin)}/min"
                drawTableRow(canvas, paint, "Waiting Charge", waitUnit, String.format(Locale.US, "%.2f", waitCharge), MARGIN, currentY, isEven = false)
                currentY += 22f

                if (itemizedExtras.isNotEmpty()) {
                    itemizedExtras.forEachIndexed { idx, item ->
                        drawTableRow(
                            canvas, paint,
                            "Extra Charge: ${item.label}",
                            "Itemized Additional Charge",
                            String.format(Locale.US, "%.2f", item.amount),
                            MARGIN, currentY, isEven = (idx % 2 == 0)
                        )
                        currentY += 22f
                    }
                    val rem = trip.outstationAdditionalCharges - itemizedExtrasSum
                    if (rem > 0.01) {
                        drawTableRow(canvas, paint, "Additional Charges", "Other Extra Charges", String.format(Locale.US, "%.2f", rem), MARGIN, currentY, isEven = true)
                        currentY += 22f
                    }
                } else if (trip.outstationAdditionalCharges > 0.0) {
                    drawTableRow(canvas, paint, "Extra Charges", "Tolls / Parking / Additional", String.format(Locale.US, "%.2f", trip.outstationAdditionalCharges), MARGIN, currentY, isEven = true)
                    currentY += 22f
                }
            }
            ServiceType.RENTAL -> {
                val calc = FareCalculator.calculateHourlyRentalFare(
                    durationSeconds = trip.durationSeconds,
                    distanceKm = trip.distanceKm,
                    hourlyRate = trip.rentalBaseFare,
                    includedKmPerHour = if (trip.rentalBaseKm > 0.0) trip.rentalBaseKm else 10.0,
                    extraKmRate = trip.rentalExtraKmRate
                )

                drawTableRow(
                    canvas, paint, "Hourly Rental Charge",
                    "${calc.billableHours} billable hr(s) @ ${trip.currency}${String.format(Locale.US, "%.2f", calc.hourlyRate)}/hr (5-min grace rule)",
                    String.format(Locale.US, "%.2f", calc.hourlyCharge),
                    MARGIN, currentY, isEven = false
                )
                currentY += 22f

                drawTableRow(
                    canvas, paint, "Included Distance Allowance",
                    "${String.format(Locale.US, "%.2f", calc.includedKmPerHour)} km/hr (${String.format(Locale.US, "%.2f", calc.totalIncludedKm)} km total included)",
                    "Included",
                    MARGIN, currentY, isEven = true
                )
                currentY += 22f

                if (calc.extraDistanceKm > 0.0) {
                    val distUnit = "${String.format(Locale.US, "%.2f", calc.extraDistanceKm)} km extra @ ${trip.currency}${String.format(Locale.US, "%.2f", calc.extraKmRate)}/km"
                    drawTableRow(canvas, paint, "Additional Distance Charge", distUnit, String.format(Locale.US, "%.2f", calc.extraDistanceCharge), MARGIN, currentY, isEven = false)
                    currentY += 22f
                } else {
                    val distUnit = "${String.format(Locale.US, "%.2f", calc.actualDistanceKm)} km actual (Within included ${String.format(Locale.US, "%.2f", calc.totalIncludedKm)} km)"
                    drawTableRow(canvas, paint, "Distance Driven", distUnit, "${trip.currency}0.00", MARGIN, currentY, isEven = false)
                    currentY += 22f
                }

                if (itemizedExtras.isNotEmpty()) {
                    itemizedExtras.forEachIndexed { idx, item ->
                        drawTableRow(
                            canvas, paint,
                            "Extra Charge: ${item.label}",
                            "Itemized Additional Charge",
                            String.format(Locale.US, "%.2f", item.amount),
                            MARGIN, currentY, isEven = (idx % 2 == 0)
                        )
                        currentY += 22f
                    }
                    val rem = trip.outstationAdditionalCharges - itemizedExtrasSum
                    if (rem > 0.01) {
                        drawTableRow(canvas, paint, "Additional Charges", "Other Extra Charges", String.format(Locale.US, "%.2f", rem), MARGIN, currentY, isEven = true)
                        currentY += 22f
                    }
                } else if (trip.outstationAdditionalCharges > 0.0) {
                    drawTableRow(canvas, paint, "Extra Charges", "Tolls / Parking / Additional", String.format(Locale.US, "%.2f", trip.outstationAdditionalCharges), MARGIN, currentY, isEven = true)
                    currentY += 22f
                }
            }
            ServiceType.OUTSTATION -> {
                val kmCost = trip.distanceKm * trip.outstationRatePerKm
                val distUnit = "${String.format(Locale.US, "%.2f", trip.distanceKm)} km @ ${trip.currency}${String.format(Locale.US, "%.2f", trip.outstationRatePerKm)}/km"
                drawTableRow(canvas, paint, "Outstation Distance Charge", distUnit, String.format(Locale.US, "%.2f", kmCost), MARGIN, currentY, isEven = false)
                currentY += 22f

                if (trip.outstationDriverBata > 0.0) {
                    drawTableRow(canvas, paint, "Driver Allowance (Bata)", "Driver Day Allowance", String.format(Locale.US, "%.2f", trip.outstationDriverBata), MARGIN, currentY, isEven = true)
                    currentY += 22f
                }

                if (trip.outstationWaitRate > 0.0) {
                    val waitHrs = trip.waitingSeconds / 3600.0
                    val waitCost = waitHrs * trip.outstationWaitRate
                    val waitUnit = "${String.format(Locale.US, "%.2f", waitHrs)} hr @ ${trip.currency}${String.format(Locale.US, "%.2f", trip.outstationWaitRate)}/hr"
                    drawTableRow(canvas, paint, "Waiting / Halt Charge", waitUnit, String.format(Locale.US, "%.2f", waitCost), MARGIN, currentY, isEven = false)
                    currentY += 22f
                }

                if (itemizedExtras.isNotEmpty()) {
                    itemizedExtras.forEachIndexed { idx, item ->
                        drawTableRow(
                            canvas, paint,
                            "Extra Charge: ${item.label}",
                            "Itemized Additional Charge",
                            String.format(Locale.US, "%.2f", item.amount),
                            MARGIN, currentY, isEven = (idx % 2 == 0)
                        )
                        currentY += 22f
                    }
                    val rem = trip.outstationAdditionalCharges - itemizedExtrasSum
                    if (rem > 0.01) {
                        drawTableRow(canvas, paint, "Additional Charges", "Other Extra Charges", String.format(Locale.US, "%.2f", rem), MARGIN, currentY, isEven = true)
                        currentY += 22f
                    }
                } else if (trip.outstationAdditionalCharges > 0.0) {
                    drawTableRow(canvas, paint, "Additional Charges", "Tolls / Parking / Night fees", String.format(Locale.US, "%.2f", trip.outstationAdditionalCharges), MARGIN, currentY, isEven = true)
                    currentY += 22f
                }

                if (trip.outstationMinFare > 0.0) {
                    drawTableRow(canvas, paint, "Minimum Guaranteed Fare Check", "Minimum floor ${trip.currency}${String.format(Locale.US, "%.2f", trip.outstationMinFare)}", String.format(Locale.US, "%.2f", trip.totalFare), MARGIN, currentY, isEven = false)
                    currentY += 22f
                }
            }
        }

        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 1f
        canvas.drawRect(MARGIN, tableTopY - 24f, PAGE_WIDTH - MARGIN, currentY, paint)
        paint.style = Paint.Style.FILL

        currentY += 16f

        // 6. TOTAL AMOUNT BOX
        val totalBoxWidth = 240f
        val totalBoxHeight = 52f
        val totalBoxX = PAGE_WIDTH - MARGIN - totalBoxWidth
        drawRoundedCard(canvas, totalBoxX, currentY, totalBoxWidth, totalBoxHeight, Color.rgb(254, 242, 242), Color.rgb(254, 226, 226))

        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TOTAL PAYABLE:", totalBoxX + 16f, currentY + 22f, paint)

        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Tax & All Charges Included", totalBoxX + 16f, currentY + 36f, paint)

        paint.textAlign = Paint.Align.RIGHT
        paint.color = Color.rgb(202, 31, 47)
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("${trip.currency}${String.format(Locale.US, "%.2f", trip.totalFare)}", PAGE_WIDTH - MARGIN - 16f, currentY + 33f, paint)
        paint.textAlign = Paint.Align.LEFT

        currentY += totalBoxHeight + 24f

        // 7. FOOTER SECTION
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, paint)

        currentY += 16f
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(vendorMeta.vendorName, MARGIN, currentY, paint)

        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Electronically generated trip invoice.", MARGIN, currentY + 12f, paint)
    }

    fun generateVendorStatementPdf(context: Context, trip: TripEntity): File {
        val vendorMeta = resolveVendorHeaderMetadata(trip)
        val safeSlug = vendorMeta.vendorId.uppercase(Locale.US).replace(Regex("[^A-Z0-9_]"), "_")
        val file = File(context.cacheDir, "${safeSlug}_Vendor_Statement_${trip.id}.pdf")

        val pdfDocument = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            drawVendorStatementCanvas(context, page.canvas, trip, vendorMeta)
            pdfDocument.finishPage(page)
            FileOutputStream(file).use { pdfDocument.writeTo(it) }
        } catch (_: IllegalStateException) {
            val fallbackBitmap = Bitmap.createBitmap(PAGE_WIDTH, PAGE_HEIGHT, Bitmap.Config.ARGB_8888)
            drawVendorStatementCanvas(context, Canvas(fallbackBitmap), trip, vendorMeta)
            file.writeText("%PDF-1.4\n% ${vendorMeta.vendorName} | ${vendorMeta.invoiceNumber} | ${vendorMeta.tripNumber}\n%%EOF\n")
        } finally {
            runCatching { pdfDocument.close() }
        }
        return file
    }

    private fun drawVendorStatementCanvas(
        context: Context,
        canvas: Canvas,
        trip: TripEntity,
        vendorMeta: InvoiceVendorHeaderMetadata
    ) {
        val paint = Paint().apply { isAntiAlias = true }
        val textPaint = TextPaint().apply {
            textSize = 9.5f
            color = Color.rgb(30, 41, 59)
            isAntiAlias = true
        }

        var currentY = MARGIN

        // TOP NAVY ACCENT BAR
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 12f, paint)

        currentY += 10f
        val logoBitmap = VendorLogoRenderer.renderVendorLogoBitmap(
            context = context,
            vendorId = vendorMeta.vendorId,
            vendorName = vendorMeta.vendorName,
            widthPx = 320,
            heightPx = 122
        )
        val logoDestRect = RectF(MARGIN, currentY, MARGIN + 102f, currentY + 39f)
        canvas.drawBitmap(logoBitmap, null, logoDestRect, Paint(Paint.FILTER_BITMAP_FLAG))

        val brandTextX = MARGIN + 112f
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(vendorMeta.vendorName, brandTextX, currentY + 16f, paint)

        paint.color = Color.rgb(202, 31, 47)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("${vendorMeta.vendorName} • VENDOR & OPERATOR AUDIT STATEMENT", brandTextX, currentY + 29f, paint)

        if (vendorMeta.displayedBusinessLines.isNotEmpty()) {
            paint.color = Color.rgb(100, 116, 139)
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(vendorMeta.displayedBusinessLines.joinToString(" | "), brandTextX, currentY + 41f, paint)
        }

        paint.textAlign = Paint.Align.RIGHT
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TRIP AUDIT REPORT", PAGE_WIDTH - MARGIN, currentY + 12f, paint)

        paint.textSize = 9f
        paint.color = Color.rgb(71, 85, 105)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Invoice No: ${vendorMeta.invoiceNumber}", PAGE_WIDTH - MARGIN, currentY + 25f, paint)
        canvas.drawText("Trip Number: ${vendorMeta.tripNumber}", PAGE_WIDTH - MARGIN, currentY + 36f, paint)

        val dateFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val startTimeStr = timeFormatter.format(Date(trip.startTime))
        val endTimeStr = if (trip.endTime > 0) timeFormatter.format(Date(trip.endTime)) else timeFormatter.format(Date(trip.startTime + (trip.durationSeconds * 1000)))

        val displayInvoiceDate = if (trip.invoiceDateText.isNotBlank()) {
            if (trip.invoiceTimeText.isNotBlank()) "${trip.invoiceDateText}, ${trip.invoiceTimeText}" else trip.invoiceDateText
        } else if (trip.invoiceTimestamp > 0) {
            dateFormatter.format(Date(trip.invoiceTimestamp))
        } else {
            dateFormatter.format(Date(trip.startTime))
        }
        canvas.drawText("Invoice Date: $displayInvoiceDate", PAGE_WIDTH - MARGIN, currentY + 47f, paint)
        paint.textAlign = Paint.Align.LEFT

        currentY += 60f
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, paint)
        currentY += 16f

        // DRIVER & VEHICLE STATEMENT BOX
        val driverCardH = 70f
        drawRoundedCard(canvas, MARGIN, currentY, PAGE_WIDTH - (MARGIN * 2), driverCardH, Color.rgb(248, 250, 252), Color.rgb(226, 232, 240))

        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("OPERATOR & VEHICLE IDENTIFICATION", MARGIN + 14f, currentY + 18f, paint)

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val colW3 = (PAGE_WIDTH - (MARGIN * 2) - 28f) / 3f

        canvas.drawText("Driver: ${if (trip.driverName.isNotBlank()) trip.driverName else "Licensed Driver"}", MARGIN + 14f, currentY + 36f, paint)
        canvas.drawText("Mobile: ${if (trip.driverMobile.isNotBlank()) trip.driverMobile else "-"}", MARGIN + 14f, currentY + 52f, paint)

        canvas.drawText("Vehicle Reg: ${if (trip.vehicleNumber.isNotBlank()) trip.vehicleNumber else "Taxi"}", MARGIN + colW3 + 14f, currentY + 36f, paint)
        canvas.drawText("Model: ${if (trip.vehicleModel.isNotBlank()) trip.vehicleModel else "Taxi"}", MARGIN + colW3 + 14f, currentY + 52f, paint)

        val mode = ServiceType.fromId(trip.serviceType)
        canvas.drawText("Service Mode: ${mode.title}", MARGIN + (colW3 * 2) + 14f, currentY + 36f, paint)
        canvas.drawText("Vendor Brand: ${vendorMeta.vendorName}", MARGIN + (colW3 * 2) + 14f, currentY + 52f, paint)

        currentY += driverCardH + 14f

        // CUSTOMER PARTICULARS BOX
        val custCardH = 56f
        drawRoundedCard(canvas, MARGIN, currentY, PAGE_WIDTH - (MARGIN * 2), custCardH, Color.rgb(248, 250, 252), Color.rgb(226, 232, 240))
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CUSTOMER PARTICULARS", MARGIN + 14f, currentY + 18f, paint)

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Name: ${if (trip.customerName.isNotBlank()) trip.customerName else "Passenger (Direct Hire)"}", MARGIN + 14f, currentY + 36f, paint)
        canvas.drawText("Mobile: ${if (trip.customerMobile.isNotBlank()) trip.customerMobile else "-"}", MARGIN + colW3 + 14f, currentY + 36f, paint)
        val cAddrText = if (trip.customerAddress.isNotBlank()) trip.customerAddress else "-"
        drawWrappedText(canvas, textPaint, "Address: $cAddrText", MARGIN + (colW3 * 2) + 14f, currentY + 36f, colW3, 1)

        currentY += custCardH + 14f

        // ROUTE & OPERATIONAL TIMINGS BOX
        val tripCardH = 88f
        drawRoundedCard(canvas, MARGIN, currentY, PAGE_WIDTH - (MARGIN * 2), tripCardH, Color.WHITE, Color.rgb(226, 232, 240))

        val pAddr = if (trip.pickupAddress.isNotBlank()) trip.pickupAddress else "GPS Pickup Coordinates"
        val dAddr = if (trip.dropAddress.isNotBlank()) trip.dropAddress else "GPS Drop Coordinates"

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Pickup Location:", MARGIN + 14f, currentY + 20f, paint)
        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        drawWrappedText(canvas, textPaint, "$pAddr (At: $startTimeStr)", MARGIN + 110f, currentY + 20f, PAGE_WIDTH - MARGIN - 130f, 1)

        paint.color = Color.rgb(71, 85, 105)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Drop Location:", MARGIN + 14f, currentY + 40f, paint)
        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        drawWrappedText(canvas, textPaint, "$dAddr (At: $endTimeStr)", MARGIN + 110f, currentY + 40f, PAGE_WIDTH - MARGIN - 130f, 1)

        val durationMin = trip.durationSeconds / 60
        val durationSec = trip.durationSeconds % 60
        val waitMin = trip.waitingSeconds / 60
        val waitSec = trip.waitingSeconds % 60

        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRect(MARGIN + 8f, currentY + 56f, PAGE_WIDTH - MARGIN - 8f, currentY + 80f, paint)

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Total Distance: ${String.format(Locale.US, "%.2f", trip.distanceKm)} km", MARGIN + 16f, currentY + 72f, paint)
        canvas.drawText("Ride Duration: ${durationMin}m ${durationSec}s", MARGIN + colW3 + 16f, currentY + 72f, paint)
        val m3Label = if (mode == ServiceType.RENTAL) "Billable Slabs: ${trip.billableHours} hr" else "Waiting Delay: ${waitMin}m ${waitSec}s"
        canvas.drawText(m3Label, MARGIN + (colW3 * 2) + 16f, currentY + 72f, paint)

        currentY += tripCardH + 12f

        // TRIP ROUTE MAP SECTION (ACTUAL RECORDED GPS JOURNEY)
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TRIP ROUTE MAP (ACTUAL GPS-RECORDED JOURNEY)", MARGIN, currentY, paint)

        currentY += 6f
        val mapBoxHeight = 82f
        val routeBitmap = TripRouteMapGenerator.getOrGenerateRouteMapBitmap(context, trip)
        if (routeBitmap != null) {
            val mapDestRect = RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + mapBoxHeight)
            canvas.drawBitmap(routeBitmap, null, mapDestRect, Paint(Paint.FILTER_BITMAP_FLAG))
            currentY += mapBoxHeight + 12f
        } else {
            drawRoundedCard(canvas, MARGIN, currentY, PAGE_WIDTH - (MARGIN * 2), 26f, Color.rgb(248, 250, 252), Color.rgb(226, 232, 240))
            paint.color = Color.rgb(100, 116, 139)
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Route map unavailable for this trip (insufficient GPS coordinate history)", MARGIN + 12f, currentY + 16f, paint)
            currentY += 32f
        }

        // TARIFF & SETTLEMENT TABLE
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TARIFF & SETTLEMENT BREAKDOWN", MARGIN, currentY, paint)

        currentY += 8f
        paint.color = Color.rgb(30, 41, 59)
        canvas.drawRect(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + 22f, paint)

        paint.color = Color.WHITE
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BILLING ITEM", MARGIN + 12f, currentY + 15f, paint)
        canvas.drawText("RATE APPLIED", MARGIN + 220f, currentY + 15f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("PAYABLE (${trip.currency})", PAGE_WIDTH - MARGIN - 12f, currentY + 15f, paint)
        paint.textAlign = Paint.Align.LEFT

        currentY += 22f
        val tableTopY = currentY

        val itemizedExtras = ExtraChargeItem.deserialize(trip.extraChargesJson)
        val itemizedExtrasSum = ExtraChargeItem.totalAmount(itemizedExtras)

        when (mode) {
            ServiceType.LOCAL -> {
                val distCharge = trip.distanceKm * trip.farePerKm
                val waitMinutes = trip.waitingSeconds / 60.0
                val waitCharge = waitMinutes * trip.waitFarePerMin

                drawTableRow(canvas, paint, "Base Flag Drop Fare", "1 Fixed Flag Drop", String.format(Locale.US, "%.2f", trip.baseFare), MARGIN, currentY, false)
                currentY += 22f

                drawTableRow(canvas, paint, "Distance Driven (${String.format(Locale.US, "%.2f", trip.distanceKm)} km)", "@ ${trip.currency}${String.format(Locale.US, "%.2f", trip.farePerKm)} / km", String.format(Locale.US, "%.2f", distCharge), MARGIN, currentY, true)
                currentY += 22f

                drawTableRow(canvas, paint, "Wait Charges (${waitMin}m ${waitSec}s)", "@ ${trip.currency}${String.format(Locale.US, "%.2f", trip.waitFarePerMin)} / min", String.format(Locale.US, "%.2f", waitCharge), MARGIN, currentY, false)
                currentY += 22f

                if (itemizedExtras.isNotEmpty()) {
                    itemizedExtras.forEachIndexed { idx, item ->
                        drawTableRow(canvas, paint, "Extra Charge: ${item.label}", "Itemized Additional Charge", String.format(Locale.US, "%.2f", item.amount), MARGIN, currentY, idx % 2 == 0)
                        currentY += 22f
                    }
                    val rem = trip.outstationAdditionalCharges - itemizedExtrasSum
                    if (rem > 0.01) {
                        drawTableRow(canvas, paint, "Additional Charges", "Other Extra Charges", String.format(Locale.US, "%.2f", rem), MARGIN, currentY, true)
                        currentY += 22f
                    }
                } else if (trip.outstationAdditionalCharges > 0.0) {
                    drawTableRow(canvas, paint, "Extra Charges", "Tolls / Parking / Additional", String.format(Locale.US, "%.2f", trip.outstationAdditionalCharges), MARGIN, currentY, true)
                    currentY += 22f
                }
            }
            ServiceType.RENTAL -> {
                val calc = FareCalculator.calculateHourlyRentalFare(
                    durationSeconds = trip.durationSeconds,
                    distanceKm = trip.distanceKm,
                    hourlyRate = trip.rentalBaseFare,
                    includedKmPerHour = if (trip.rentalBaseKm > 0.0) trip.rentalBaseKm else 10.0,
                    extraKmRate = trip.rentalExtraKmRate
                )

                drawTableRow(canvas, paint, "Hourly Rental Charge", "${calc.billableHours} billable hr(s) @ ${trip.currency}${String.format(Locale.US, "%.2f", calc.hourlyRate)}/hr", String.format(Locale.US, "%.2f", calc.hourlyCharge), MARGIN, currentY, false)
                currentY += 22f

                drawTableRow(canvas, paint, "Included Distance Allowance", "${String.format(Locale.US, "%.2f", calc.includedKmPerHour)} km/hr (${String.format(Locale.US, "%.2f", calc.totalIncludedKm)} km included)", "Included", MARGIN, currentY, true)
                currentY += 22f

                if (calc.extraDistanceKm > 0.0) {
                    val distCost = calc.extraDistanceCharge
                    drawTableRow(canvas, paint, "Additional Distance Charge", "${String.format(Locale.US, "%.2f", calc.extraDistanceKm)} km extra @ ${trip.currency}${String.format(Locale.US, "%.2f", calc.extraKmRate)}/km", String.format(Locale.US, "%.2f", distCost), MARGIN, currentY, false)
                    currentY += 22f
                } else {
                    drawTableRow(canvas, paint, "Distance Driven", "${String.format(Locale.US, "%.2f", calc.actualDistanceKm)} km (Included in package)", "${trip.currency}0.00", MARGIN, currentY, false)
                    currentY += 22f
                }

                if (itemizedExtras.isNotEmpty()) {
                    itemizedExtras.forEachIndexed { idx, item ->
                        drawTableRow(canvas, paint, "Extra Charge: ${item.label}", "Itemized Additional Charge", String.format(Locale.US, "%.2f", item.amount), MARGIN, currentY, idx % 2 == 0)
                        currentY += 22f
                    }
                    val rem = trip.outstationAdditionalCharges - itemizedExtrasSum
                    if (rem > 0.01) {
                        drawTableRow(canvas, paint, "Additional Charges", "Other Extra Charges", String.format(Locale.US, "%.2f", rem), MARGIN, currentY, true)
                        currentY += 22f
                    }
                } else if (trip.outstationAdditionalCharges > 0.0) {
                    drawTableRow(canvas, paint, "Extra Charges", "Tolls / Parking / Additional", String.format(Locale.US, "%.2f", trip.outstationAdditionalCharges), MARGIN, currentY, true)
                    currentY += 22f
                }
            }
            ServiceType.OUTSTATION -> {
                val kmCost = trip.distanceKm * trip.outstationRatePerKm
                drawTableRow(canvas, paint, "Outstation Distance Charge", "${String.format(Locale.US, "%.2f", trip.distanceKm)} km @ ${trip.currency}${String.format(Locale.US, "%.2f", trip.outstationRatePerKm)}/km", String.format(Locale.US, "%.2f", kmCost), MARGIN, currentY, false)
                currentY += 22f

                if (trip.outstationDriverBata > 0.0) {
                    drawTableRow(canvas, paint, "Driver Allowance (Bata)", "Driver Day Allowance", String.format(Locale.US, "%.2f", trip.outstationDriverBata), MARGIN, currentY, true)
                    currentY += 22f
                }

                if (trip.outstationWaitRate > 0.0) {
                    val waitHrs = trip.waitingSeconds / 3600.0
                    val waitCost = waitHrs * trip.outstationWaitRate
                    drawTableRow(canvas, paint, "Waiting / Halt Charge", "${String.format(Locale.US, "%.2f", waitHrs)} hr @ ${trip.currency}${String.format(Locale.US, "%.2f", trip.outstationWaitRate)}/hr", String.format(Locale.US, "%.2f", waitCost), MARGIN, currentY, false)
                    currentY += 22f
                }

                if (itemizedExtras.isNotEmpty()) {
                    itemizedExtras.forEachIndexed { idx, item ->
                        drawTableRow(canvas, paint, "Extra Charge: ${item.label}", "Itemized Additional Charge", String.format(Locale.US, "%.2f", item.amount), MARGIN, currentY, idx % 2 == 0)
                        currentY += 22f
                    }
                    val rem = trip.outstationAdditionalCharges - itemizedExtrasSum
                    if (rem > 0.01) {
                        drawTableRow(canvas, paint, "Additional Charges", "Other Extra Charges", String.format(Locale.US, "%.2f", rem), MARGIN, currentY, true)
                        currentY += 22f
                    }
                } else if (trip.outstationAdditionalCharges > 0.0) {
                    drawTableRow(canvas, paint, "Additional Charges", "Tolls / Parking fees", String.format(Locale.US, "%.2f", trip.outstationAdditionalCharges), MARGIN, currentY, true)
                    currentY += 22f
                }
            }
        }

        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 1f
        canvas.drawRect(MARGIN, tableTopY - 22f, PAGE_WIDTH - MARGIN, currentY, paint)
        paint.style = Paint.Style.FILL

        currentY += 14f

        val boxW = 240f
        val boxH = 48f
        val boxX = PAGE_WIDTH - MARGIN - boxW
        drawRoundedCard(canvas, boxX, currentY, boxW, boxH, Color.rgb(241, 245, 249), Color.rgb(203, 213, 225))

        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("AUDITED SETTLEMENT TOTAL:", boxX + 14f, currentY + 20f, paint)

        paint.textAlign = Paint.Align.RIGHT
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("${trip.currency}${String.format(Locale.US, "%.2f", trip.totalFare)}", PAGE_WIDTH - MARGIN - 14f, currentY + 33f, paint)
        paint.textAlign = Paint.Align.LEFT

        currentY += boxH + 24f
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, paint)

        currentY += 14f
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(vendorMeta.vendorName, MARGIN, currentY, paint)

        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Electronically generated vendor audit statement.", MARGIN, currentY + 12f, paint)
    }

    private fun drawRoundedCard(canvas: android.graphics.Canvas, x: Float, y: Float, width: Float, height: Float, bgColor: Int, borderColor: Int) {
        val paint = Paint().apply { isAntiAlias = true }
        paint.color = bgColor
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(x, y, x + width, y + height, 8f, 8f, paint)

        paint.color = borderColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(x, y, x + width, y + height, 8f, 8f, paint)
        paint.style = Paint.Style.FILL
    }

    private fun drawTableRow(canvas: android.graphics.Canvas, paint: Paint, col1: String, col2: String, col3: String, startX: Float, startY: Float, isEven: Boolean) {
        if (isEven) {
            paint.color = Color.rgb(248, 250, 252)
            canvas.drawRect(startX, startY, PAGE_WIDTH - MARGIN, startY + 22f, paint)
        }
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(col1, startX + 12f, startY + 15f, paint)

        paint.color = Color.rgb(71, 85, 105)
        canvas.drawText(col2, startX + 220f, startY + 15f, paint)

        paint.textAlign = Paint.Align.RIGHT
        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(col3, PAGE_WIDTH - MARGIN - 12f, startY + 15f, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    private fun drawWrappedText(canvas: android.graphics.Canvas, textPaint: TextPaint, text: String, x: Float, y: Float, width: Float, maxLines: Int) {
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, width.toInt())
            .setMaxLines(maxLines)
            .build()
        canvas.save()
        canvas.translate(x, y - textPaint.textSize + 2f)
        layout.draw(canvas)
        canvas.restore()
    }
}
