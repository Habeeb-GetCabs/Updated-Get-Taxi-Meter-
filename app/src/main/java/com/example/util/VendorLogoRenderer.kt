package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.database.VendorProfileEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Reusable vendor logo template engine, color theme & typography customizer,
 * and catalog for GET TAXI and all 20 supported vendors.
 */
object VendorLogoRenderer {

    const val GET_TAXI_VENDOR_ID = "get_taxi"
    const val GET_TAXI_VENDOR_NAME = "GET TAXI"
    const val GET_TAXI_LOGO_ASSET = "res/drawable/get_taxi_header_logo.png"

    const val DEFAULT_VENDOR_ID = GET_TAXI_VENDOR_ID
    const val DEFAULT_VENDOR_NAME = GET_TAXI_VENDOR_NAME

    // Updated Default Brand Colors: Royal Sapphire Blue + Crisp White + Electric Gold
    const val BRAND_PRIMARY_BG_HEX = "#0F296B"
    const val BRAND_SECONDARY_BG_HEX = "#1E3A8A"
    const val BRAND_WHITE_HEX = "#FFFFFF"
    const val BRAND_ACCENT_HEX = "#FFD700"

    // Legacy constants retained for backward compatibility
    const val BRAND_RED_HEX = BRAND_PRIMARY_BG_HEX
    const val BRAND_YELLOW_HEX = BRAND_ACCENT_HEX

    const val BRAND_RED_INT: Int = 0xFF0F296B.toInt()
    const val BRAND_WHITE_INT: Int = 0xFFFFFFFF.toInt()
    const val BRAND_YELLOW_INT: Int = 0xFFFFD700.toInt()

    private const val PREFS_NAME = "get_taxi_logo_style_prefs"
    private const val KEY_GLOBAL_COLOR_THEME = "global_color_theme_id"
    private const val KEY_GLOBAL_FONT_STYLE = "global_font_style_id"
    private const val KEY_USE_Distinct_VENDOR_COLORS = "use_distinct_vendor_colors"
    private const val KEY_VENDOR_COLOR_PREFIX = "vendor_color_"
    private const val KEY_VENDOR_FONT_PREFIX = "vendor_font_"

    enum class LogoColorTheme(
        val id: String,
        val displayName: String,
        val bgStartHex: String,
        val bgEndHex: String,
        val bgStartInt: Int,
        val bgEndInt: Int,
        val primaryTextHex: String,
        val primaryTextInt: Int,
        val accentTextHex: String,
        val accentTextInt: Int
    ) {
        ROYAL_SAPPHIRE_GOLD(
            id = "royal_sapphire_gold",
            displayName = "Royal Sapphire & Gold",
            bgStartHex = "#0F296B",
            bgEndHex = "#1E40AF",
            bgStartInt = 0xFF0F296B.toInt(),
            bgEndInt = 0xFF1E40AF.toInt(),
            primaryTextHex = "#FFFFFF",
            primaryTextInt = 0xFFFFFFFF.toInt(),
            accentTextHex = "#FFD700",
            accentTextInt = 0xFFFFD700.toInt()
        ),
        EMERALD_LUXE_GOLD(
            id = "emerald_luxe_gold",
            displayName = "Emerald Luxe & Gold",
            bgStartHex = "#064E3B",
            bgEndHex = "#047857",
            bgStartInt = 0xFF064E3B.toInt(),
            bgEndInt = 0xFF047857.toInt(),
            primaryTextHex = "#FFFFFF",
            primaryTextInt = 0xFFFFFFFF.toInt(),
            accentTextHex = "#FBBF24",
            accentTextInt = 0xFFFBBF24.toInt()
        ),
        MIDNIGHT_NAVY_CYAN(
            id = "midnight_navy_cyan",
            displayName = "Midnight Navy & Cyan",
            bgStartHex = "#0F172A",
            bgEndHex = "#1E293B",
            bgStartInt = 0xFF0F172A.toInt(),
            bgEndInt = 0xFF1E293B.toInt(),
            primaryTextHex = "#FFFFFF",
            primaryTextInt = 0xFFFFFFFF.toInt(),
            accentTextHex = "#38BDF8",
            accentTextInt = 0xFF38BDF8.toInt()
        ),
        IMPERIAL_PURPLE_GOLD(
            id = "imperial_purple_gold",
            displayName = "Imperial Purple & Gold",
            bgStartHex = "#3B0764",
            bgEndHex = "#6B21A8",
            bgStartInt = 0xFF3B0764.toInt(),
            bgEndInt = 0xFF6B21A8.toInt(),
            primaryTextHex = "#FFFFFF",
            primaryTextInt = 0xFFFFFFFF.toInt(),
            accentTextHex = "#FACC15",
            accentTextInt = 0xFFFACC15.toInt()
        ),
        OBSIDIAN_BLACK_AMBER(
            id = "obsidian_black_amber",
            displayName = "Obsidian Black & Amber",
            bgStartHex = "#18181B",
            bgEndHex = "#27272A",
            bgStartInt = 0xFF18181B.toInt(),
            bgEndInt = 0xFF27272A.toInt(),
            primaryTextHex = "#FFFFFF",
            primaryTextInt = 0xFFFFFFFF.toInt(),
            accentTextHex = "#F59E0B",
            accentTextInt = 0xFFF59E0B.toInt()
        ),
        TEAL_OCEAN_SUNBURST(
            id = "teal_ocean_sunburst",
            displayName = "Ocean Teal & Sunburst",
            bgStartHex = "#115E59",
            bgEndHex = "#0F766E",
            bgStartInt = 0xFF115E59.toInt(),
            bgEndInt = 0xFF0F766E.toInt(),
            primaryTextHex = "#FFFFFF",
            primaryTextInt = 0xFFFFFFFF.toInt(),
            accentTextHex = "#FDE047",
            accentTextInt = 0xFFFDE047.toInt()
        ),
        BURGUNDY_CHAMPAGNE(
            id = "burgundy_champagne",
            displayName = "Burgundy & Champagne",
            bgStartHex = "#7F1D1D",
            bgEndHex = "#991B1B",
            bgStartInt = 0xFF7F1D1D.toInt(),
            bgEndInt = 0xFF991B1B.toInt(),
            primaryTextHex = "#FFFFFF",
            primaryTextInt = 0xFFFFFFFF.toInt(),
            accentTextHex = "#FDE68A",
            accentTextInt = 0xFFFDE68A.toInt()
        ),
        COBALT_ELECTRIC_GOLD(
            id = "cobalt_electric_gold",
            displayName = "Electric Cobalt & Gold",
            bgStartHex = "#1D4ED8",
            bgEndHex = "#2563EB",
            bgStartInt = 0xFF1D4ED8.toInt(),
            bgEndInt = 0xFF2563EB.toInt(),
            primaryTextHex = "#FFFFFF",
            primaryTextInt = 0xFFFFFFFF.toInt(),
            accentTextHex = "#FACC15",
            accentTextInt = 0xFFFACC15.toInt()
        ),
        CLASSIC_CRIMSON_YELLOW(
            id = "classic_crimson_yellow",
            displayName = "Classic Red & Yellow",
            bgStartHex = "#CA1F2F",
            bgEndHex = "#B91C1C",
            bgStartInt = 0xFFCA1F2F.toInt(),
            bgEndInt = 0xFFB91C1C.toInt(),
            primaryTextHex = "#FFFFFF",
            primaryTextInt = 0xFFFFFFFF.toInt(),
            accentTextHex = "#F8B814",
            accentTextInt = 0xFFF8B814.toInt()
        );

        companion object {
            fun fromId(id: String?): LogoColorTheme {
                return entries.firstOrNull { it.id == id } ?: ROYAL_SAPPHIRE_GOLD
            }
        }
    }

    enum class LogoFontStyle(
        val id: String,
        val displayName: String,
        val fontResId: Int,
        val svgFontFamily: String,
        val skewX: Float,
        val isAllCaps: Boolean = false
    ) {
        RIGHTEOUS(
            id = "righteous",
            displayName = "Righteous Modern",
            fontResId = R.font.righteous,
            svgFontFamily = "'Righteous', 'Montserrat', sans-serif",
            skewX = -0.08f
        ),
        LOBSTER(
            id = "lobster",
            displayName = "Lobster Bold Script",
            fontResId = R.font.lobster,
            svgFontFamily = "'Lobster', 'Kaushan Script', cursive",
            skewX = -0.04f
        ),
        KAUSHAN_SCRIPT(
            id = "kaushan_script",
            displayName = "Kaushan Brush Script",
            fontResId = R.font.kaushan_script,
            svgFontFamily = "'Kaushan Script', cursive",
            skewX = -0.06f
        ),
        MONTSERRAT(
            id = "montserrat",
            displayName = "Montserrat Geometric",
            fontResId = R.font.montserrat,
            svgFontFamily = "'Montserrat', sans-serif",
            skewX = -0.10f
        ),
        BEBAS_NEUE(
            id = "bebas_neue",
            displayName = "Bebas Sport Display",
            fontResId = R.font.bebas_neue,
            svgFontFamily = "'Bebas Neue', sans-serif",
            skewX = -0.08f,
            isAllCaps = true
        ),
        CINZEL(
            id = "cinzel",
            displayName = "Cinzel Luxury Serif",
            fontResId = R.font.cinzel,
            svgFontFamily = "'Cinzel', Georgia, serif",
            skewX = -0.05f
        ),
        PLAYFAIR(
            id = "playfair",
            displayName = "Playfair Editorial",
            fontResId = R.font.playfair_display,
            svgFontFamily = "'Playfair Display', Georgia, serif",
            skewX = -0.10f
        ),
        PACIFICO(
            id = "pacifico",
            displayName = "Pacifico Flowing Script",
            fontResId = R.font.pacifico,
            svgFontFamily = "'Pacifico', cursive",
            skewX = -0.04f
        );

        companion object {
            fun fromId(id: String?): LogoFontStyle {
                return entries.firstOrNull { it.id == id } ?: RIGHTEOUS
            }
        }
    }

    data class LogoStyleState(
        val globalColorTheme: LogoColorTheme = LogoColorTheme.ROYAL_SAPPHIRE_GOLD,
        val globalFontStyle: LogoFontStyle = LogoFontStyle.RIGHTEOUS,
        val useDistinctVendorColors: Boolean = true,
        val revision: Int = 0
    )

    private val _logoStyleState = MutableStateFlow(LogoStyleState())
    val logoStyleState: StateFlow<LogoStyleState> = _logoStyleState.asStateFlow()

    private val cachedTypefaces = mutableMapOf<Int, Typeface>()
    private val vendorColorOverrides = mutableMapOf<String, LogoColorTheme>()
    private val vendorFontOverrides = mutableMapOf<String, LogoFontStyle>()
    private var isInitialized = false

    fun ensureInitialized(context: Context?) {
        if (isInitialized || context == null) return
        synchronized(this) {
            if (isInitialized) return
            runCatching {
                val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val theme = LogoColorTheme.fromId(prefs.getString(KEY_GLOBAL_COLOR_THEME, LogoColorTheme.ROYAL_SAPPHIRE_GOLD.id))
                val font = LogoFontStyle.fromId(prefs.getString(KEY_GLOBAL_FONT_STYLE, LogoFontStyle.RIGHTEOUS.id))
                val distinct = prefs.getBoolean(KEY_USE_Distinct_VENDOR_COLORS, true)
                prefs.all.forEach { (k, v) ->
                    if (k.startsWith(KEY_VENDOR_COLOR_PREFIX) && v is String) {
                        val vId = k.removePrefix(KEY_VENDOR_COLOR_PREFIX)
                        vendorColorOverrides[vId] = LogoColorTheme.fromId(v)
                    } else if (k.startsWith(KEY_VENDOR_FONT_PREFIX) && v is String) {
                        val vId = k.removePrefix(KEY_VENDOR_FONT_PREFIX)
                        vendorFontOverrides[vId] = LogoFontStyle.fromId(v)
                    }
                }
                _logoStyleState.value = LogoStyleState(
                    globalColorTheme = theme,
                    globalFontStyle = font,
                    useDistinctVendorColors = distinct,
                    revision = _logoStyleState.value.revision + 1
                )
                isInitialized = true
            }
        }
    }

    fun updateGlobalLogoStyle(
        context: Context,
        colorTheme: LogoColorTheme,
        fontStyle: LogoFontStyle,
        useDistinctVendorColors: Boolean = _logoStyleState.value.useDistinctVendorColors,
        clearVendorOverrides: Boolean = true
    ) {
        ensureInitialized(context)
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
            .putString(KEY_GLOBAL_COLOR_THEME, colorTheme.id)
            .putString(KEY_GLOBAL_FONT_STYLE, fontStyle.id)
            .putBoolean(KEY_USE_Distinct_VENDOR_COLORS, useDistinctVendorColors)

        if (clearVendorOverrides) {
            vendorColorOverrides.clear()
            vendorFontOverrides.clear()
            prefs.all.keys
                .filter { it.startsWith(KEY_VENDOR_COLOR_PREFIX) || it.startsWith(KEY_VENDOR_FONT_PREFIX) }
                .forEach { editor.remove(it) }
        }
        editor.apply()

        _logoStyleState.value = LogoStyleState(
            globalColorTheme = colorTheme,
            globalFontStyle = fontStyle,
            useDistinctVendorColors = useDistinctVendorColors,
            revision = _logoStyleState.value.revision + 1
        )
    }

    fun setVendorLogoStyle(
        context: Context,
        vendorId: String,
        colorTheme: LogoColorTheme,
        fontStyle: LogoFontStyle
    ) {
        ensureInitialized(context)
        val cleanId = vendorId.ifBlank { DEFAULT_VENDOR_ID }
        vendorColorOverrides[cleanId] = colorTheme
        vendorFontOverrides[cleanId] = fontStyle

        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("$KEY_VENDOR_COLOR_PREFIX$cleanId", colorTheme.id)
            .putString("$KEY_VENDOR_FONT_PREFIX$cleanId", fontStyle.id)
            .apply()

        _logoStyleState.value = _logoStyleState.value.copy(
            revision = _logoStyleState.value.revision + 1
        )
    }

    fun resolveColorThemeForVendor(vendorId: String?, vendorName: String): LogoColorTheme {
        val cleanId = vendorId?.takeIf { it.isNotBlank() } ?: slugForName(vendorName)
        vendorColorOverrides[cleanId]?.let { return it }

        val currentState = _logoStyleState.value
        if (isGetTaxiVendor(cleanId, vendorName) || !currentState.useDistinctVendorColors) {
            return currentState.globalColorTheme
        }

        val palette = listOf(
            LogoColorTheme.ROYAL_SAPPHIRE_GOLD,
            LogoColorTheme.EMERALD_LUXE_GOLD,
            LogoColorTheme.MIDNIGHT_NAVY_CYAN,
            LogoColorTheme.IMPERIAL_PURPLE_GOLD,
            LogoColorTheme.OBSIDIAN_BLACK_AMBER,
            LogoColorTheme.TEAL_OCEAN_SUNBURST,
            LogoColorTheme.BURGUNDY_CHAMPAGNE,
            LogoColorTheme.COBALT_ELECTRIC_GOLD
        )
        val idx = TWENTY_VENDOR_NAMES.indexOfFirst { it.equals(vendorName.trim(), ignoreCase = true) }
        val hashIndex = if (idx >= 0) idx else kotlin.math.abs(cleanId.hashCode())
        return palette[hashIndex % palette.size]
    }

    fun resolveFontStyleForVendor(vendorId: String?, vendorName: String): LogoFontStyle {
        val cleanId = vendorId?.takeIf { it.isNotBlank() } ?: slugForName(vendorName)
        vendorFontOverrides[cleanId]?.let { return it }
        return _logoStyleState.value.globalFontStyle
    }

    private fun resolveTypeface(context: Context?, fontStyle: LogoFontStyle): Typeface {
        cachedTypefaces[fontStyle.fontResId]?.let { return it }
        if (context != null) {
            val loaded = runCatching {
                ResourcesCompat.getFont(context, fontStyle.fontResId)
            }.getOrNull()
            if (loaded != null) {
                val boldStyled = Typeface.create(loaded, Typeface.BOLD)
                cachedTypefaces[fontStyle.fontResId] = boldStyled
                return boldStyled
            }
        }
        return Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD_ITALIC)
    }

    data class BuiltInVendorSpec(
        val vendorId: String,
        val vendorName: String,
        val logoAsset: String,
        val sortOrder: Int
    )

    data class SplitVendorParts(
        val firstPartWhite: String,
        val secondPartYellow: String
    )

    /**
     * Exact 20 vendor names in Title Case as specified by the user.
     * No suffixes are added or removed.
     */
    val TWENTY_VENDOR_NAMES: List<String> = listOf(
        "Hello City",
        "Sri Vaari",
        "Quick Taxi",
        "SGS Travels",
        "Yellow Go Taxi",
        "Get Cabs",
        "Sandhya Cabs",
        "Shobi Travels",
        "Sivam Taxi",
        "C Taxi",
        "B Taxi",
        "Nashita Cabs",
        "Kovai Holidays",
        "Covai Call Taxi",
        "Surya Cabs",
        "Om Muruga Taxi",
        "Helen Cabs",
        "I Taxi",
        "Anandhaa Cabs",
        "Sai Cabs"
    )

    fun slugForName(name: String): String {
        return name.trim()
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
    }

    fun slugForVendorName(name: String): String = slugForName(name)

    fun assetPathForVendorName(name: String): String {
        if (name.equals(GET_TAXI_VENDOR_NAME, ignoreCase = true)) {
            return GET_TAXI_LOGO_ASSET
        }
        val slug = slugForName(name)
        return "assets/vendor_logos/vendor_logo_${slug}.svg"
    }

    fun assetPathForVendor(vendorId: String?, vendorName: String): String {
        return if (isGetTaxiVendor(vendorId, vendorName)) {
            GET_TAXI_LOGO_ASSET
        } else {
            assetPathForVendorName(vendorName)
        }
    }

    val BUILT_IN_TWENTY_VENDORS: List<BuiltInVendorSpec> = TWENTY_VENDOR_NAMES.mapIndexed { index, name ->
        val slug = slugForName(name)
        BuiltInVendorSpec(
            vendorId = slug,
            vendorName = name,
            logoAsset = "assets/vendor_logos/vendor_logo_${slug}.svg",
            sortOrder = index + 1
        )
    }

    fun defaultGetTaxiProfile(): VendorProfileEntity {
        return VendorProfileEntity(
            vendorId = GET_TAXI_VENDOR_ID,
            vendorName = GET_TAXI_VENDOR_NAME,
            logoAsset = GET_TAXI_LOGO_ASSET,
            contactNumber = "",
            businessAddress = "",
            email = "",
            gstin = "",
            invoiceDisplayDetails = "",
            isActive = true,
            isOriginalGetTaxi = true,
            sortOrder = 0
        )
    }

    /**
     * Returns the complete initial seed list: GET TAXI (sortOrder 0) + all 20 vendors (sortOrder 1..20).
     * Does not fabricate contact numbers, addresses, emails, or GSTINs.
     */
    fun buildInitialSeedVendors(): List<VendorProfileEntity> {
        val getTaxiDefault = defaultGetTaxiProfile()

        val twentyEntities = BUILT_IN_TWENTY_VENDORS.map { spec ->
            VendorProfileEntity(
                vendorId = spec.vendorId,
                vendorName = spec.vendorName,
                logoAsset = spec.logoAsset,
                contactNumber = "",
                businessAddress = "",
                email = "",
                gstin = "",
                invoiceDisplayDetails = "",
                isActive = true,
                isOriginalGetTaxi = false,
                sortOrder = spec.sortOrder
            )
        }

        return listOf(getTaxiDefault) + twentyEntities
    }

    fun buildInitialVendorEntities(): List<VendorProfileEntity> = buildInitialSeedVendors()

    fun isGetTaxiVendor(vendorId: String?, vendorName: String?): Boolean {
        if (vendorId == GET_TAXI_VENDOR_ID) return true
        val cleaned = vendorName?.trim() ?: ""
        return cleaned.equals(GET_TAXI_VENDOR_NAME, ignoreCase = true)
    }

    fun buildVendorInvoiceNumber(vendorName: String, tripNumber: String): String {
        val initials = vendorName.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString("") { it.first().uppercaseChar().toString() }
            .ifBlank { "GT" }
        val suffix = tripNumber.removePrefix("GTM-").ifBlank { "0001" }
        return "$initials-$suffix"
    }

    /**
     * Splits a vendor name into (primaryPart, accentPart) for the unified badge logo.
     */
    fun splitBrandWords(vendorName: String): Pair<String, String> {
        val trimmed = vendorName.trim().ifBlank { GET_TAXI_VENDOR_NAME }
        val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            words.size >= 2 -> {
                val firstPart = words.dropLast(1).joinToString(" ")
                val lastPart = words.last()
                Pair(firstPart, lastPart)
            }
            words.size == 1 -> {
                val word = words[0]
                if (word.length <= 2) {
                    Pair(word, "")
                } else {
                    val mid = (word.length + 1) / 2
                    Pair(word.substring(0, mid), word.substring(mid))
                }
            }
            else -> Pair("GET", "TAXI")
        }
    }

    fun splitVendorNameParts(vendorName: String): SplitVendorParts {
        val (first, second) = splitBrandWords(vendorName)
        return SplitVendorParts(first, second)
    }

    fun generateSvgMarkup(vendorName: String): String = generateVendorSvgXml(vendorName)

    /**
     * Creates the horizontal rounded-rectangle badge path for the logo.
     */
    fun createBadgeTrapezoidPath(width: Float, height: Float): Path {
        val path = Path()
        val cornerRadius = height * 0.22f
        val rectF = android.graphics.RectF(
            width * 0.02f,
            height * 0.04f,
            width * 0.98f,
            height * 0.96f
        )
        path.addRoundRect(rectF, cornerRadius, cornerRadius, Path.Direction.CW)
        return path
    }

    /**
     * Renders a Bitmap for PDF invoices or image export using the active color theme and font style.
     */
    fun renderVendorLogoBitmap(
        context: Context,
        vendorId: String?,
        vendorName: String,
        widthPx: Int = 440,
        heightPx: Int = 180
    ): Bitmap {
        ensureInitialized(context)
        val bmp = Bitmap.createBitmap(widthPx.coerceAtLeast(120), heightPx.coerceAtLeast(50), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawTemplatedVendorLogoOnCanvas(
            canvas = canvas,
            vendorName = vendorName,
            width = bmp.width.toFloat(),
            height = bmp.height.toFloat(),
            context = context,
            vendorId = vendorId
        )
        return bmp
    }

    /**
     * Draws the vendor/brand logo onto an Android Canvas using the configured Color Theme and Font Style.
     */
    fun drawTemplatedVendorLogoOnCanvas(
        canvas: Canvas,
        vendorName: String,
        width: Float,
        height: Float,
        context: Context? = null,
        vendorId: String? = null,
        overrideColorTheme: LogoColorTheme? = null,
        overrideFontStyle: LogoFontStyle? = null
    ) {
        if (context != null) {
            ensureInitialized(context)
        }
        val colorTheme = overrideColorTheme ?: resolveColorThemeForVendor(vendorId, vendorName)
        val fontStyle = overrideFontStyle ?: resolveFontStyleForVendor(vendorId, vendorName)

        val badgePath = createBadgeTrapezoidPath(width, height)

        // Rich gradient fill for the badge
        val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                0f,
                0f,
                width,
                height,
                colorTheme.bgStartInt,
                colorTheme.bgEndInt,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(badgePath, badgePaint)

        // Subtle metallic accent border around the badge
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTheme.accentTextInt
            alpha = 90
            style = Paint.Style.STROKE
            strokeWidth = (height * 0.025f).coerceAtLeast(1.5f)
        }
        canvas.drawPath(badgePath, borderPaint)

        val (rawWhitePart, rawYellowPart) = splitBrandWords(vendorName)
        val whitePart = if (fontStyle.isAllCaps) rawWhitePart.uppercase(Locale.US) else rawWhitePart
        val yellowPart = if (fontStyle.isAllCaps) rawYellowPart.uppercase(Locale.US) else rawYellowPart

        val spaceGap = if (yellowPart.isNotEmpty()) width * 0.035f else 0f

        val resolvedTypeface = resolveTypeface(context, fontStyle)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = resolvedTypeface
            textSkewX = fontStyle.skewX
            style = Paint.Style.FILL
            isFakeBoldText = true
        }

        // Auto-fit font size so long names never overflow the badge
        val maxUsableWidth = width * 0.78f
        var fontSize = height * 0.37f
        val minFontSize = height * 0.16f

        while (fontSize > minFontSize) {
            textPaint.textSize = fontSize
            val w1 = textPaint.measureText(whitePart)
            val w2 = if (yellowPart.isNotEmpty()) textPaint.measureText(yellowPart) else 0f
            if (w1 + spaceGap + w2 <= maxUsableWidth) {
                break
            }
            fontSize -= 1.5f
        }
        textPaint.textSize = fontSize

        val w1 = textPaint.measureText(whitePart)
        val w2 = if (yellowPart.isNotEmpty()) textPaint.measureText(yellowPart) else 0f
        val totalTextWidth = w1 + spaceGap + w2

        val startX = (width - totalTextWidth) / 2f
        val baselineY = height * 0.57f

        // 1. Primary lettering
        textPaint.color = colorTheme.primaryTextInt
        canvas.drawText(whitePart, startX, baselineY, textPaint)

        // 2. Accent lettering
        if (yellowPart.isNotEmpty()) {
            textPaint.color = colorTheme.accentTextInt
            canvas.drawText(yellowPart, startX + w1 + spaceGap, baselineY, textPaint)
        }

        // 3. Curved accent script underline
        val underlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorTheme.accentTextInt
            style = Paint.Style.STROKE
            strokeWidth = (height * 0.05f).coerceAtLeast(2.5f)
            strokeCap = Paint.Cap.ROUND
        }

        val lineStartX = (startX + totalTextWidth * 0.04f).coerceAtLeast(width * 0.16f)
        val lineEndX = (startX + totalTextWidth * 0.98f).coerceAtMost(width * 0.86f)
        val lineY = height * 0.74f
        val curvePeakY = height * 0.695f

        val underlinePath = Path().apply {
            moveTo(lineStartX, lineY + height * 0.015f)
            quadTo((lineStartX + lineEndX) / 2f, curvePeakY, lineEndX, lineY)
        }
        canvas.drawPath(underlinePath, underlinePaint)
    }

    /**
     * Generates a standalone SVG string for a vendor logo using its color theme and font family.
     */
    fun generateVendorSvgXml(
        vendorName: String,
        colorTheme: LogoColorTheme = resolveColorThemeForVendor(null, vendorName),
        fontStyle: LogoFontStyle = resolveFontStyleForVendor(null, vendorName)
    ): String {
        val (whitePart, yellowPart) = splitBrandWords(vendorName)
        val charCount = vendorName.trim().length
        val fontSize = when {
            charCount <= 7 -> 76
            charCount <= 10 -> 66
            charCount <= 13 -> 56
            else -> 48
        }
        val escapedWhite = whitePart
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
        val escapedYellow = yellowPart
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")

        return """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 600 260" width="600" height="260">
              <defs>
                <linearGradient id="badgeGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" stop-color="${colorTheme.bgStartHex}"/>
                  <stop offset="100%" stop-color="${colorTheme.bgEndHex}"/>
                </linearGradient>
              </defs>
              <rect x="15" y="10" width="570" height="240" rx="48" ry="48" fill="url(#badgeGrad)" stroke="${colorTheme.accentTextHex}" stroke-opacity="0.35" stroke-width="4"/>
              <text x="300" y="148" text-anchor="middle" font-family="${fontStyle.svgFontFamily}" font-weight="bold" font-style="italic" font-size="$fontSize">
                <tspan fill="${colorTheme.primaryTextHex}">$escapedWhite</tspan><tspan fill="${colorTheme.accentTextHex}"> $escapedYellow</tspan>
              </text>
              <path d="M 110 194 Q 305 180 512 190" fill="none" stroke="${colorTheme.accentTextHex}" stroke-width="12" stroke-linecap="round"/>
            </svg>
        """.trimIndent()
    }
}
