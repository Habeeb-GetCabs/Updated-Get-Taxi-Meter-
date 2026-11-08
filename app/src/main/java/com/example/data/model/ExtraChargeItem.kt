package com.example.data.model

import java.util.Locale
import java.util.UUID

/**
 * Represents a single itemized extra charge added during a trip or in bill editing
 * (e.g., Tolls, Parking, Permit, Airport Fee, Waiting/Custom).
 */
data class ExtraChargeItem(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val amount: Double
) {
    companion object {
        private const val ITEM_SEPARATOR = "||"
        private const val FIELD_SEPARATOR = "::"

        /**
         * Pure-Kotlin serialization that works identically in Android runtime and JVM unit tests
         * without relying on android.jar org.json stubs.
         */
        fun serialize(items: List<ExtraChargeItem>): String {
            val validItems = items.filter { it.amount > 0.0 }
            if (validItems.isEmpty()) return ""
            return validItems.joinToString(ITEM_SEPARATOR) { item ->
                val safeId = item.id.replace(FIELD_SEPARATOR, "_").replace(ITEM_SEPARATOR, "_").ifBlank { UUID.randomUUID().toString() }
                val safeLabel = item.label.trim().replace(FIELD_SEPARATOR, " - ").replace(ITEM_SEPARATOR, " ").ifBlank { "Extra Charge" }
                val formattedAmount = String.format(Locale.US, "%.2f", item.amount)
                "$safeId$FIELD_SEPARATOR$safeLabel$FIELD_SEPARATOR$formattedAmount"
            }
        }

        fun deserialize(serialized: String?): List<ExtraChargeItem> {
            if (serialized.isNullOrBlank()) return emptyList()
            return serialized.split(ITEM_SEPARATOR).mapNotNull { entry ->
                val parts = entry.split(FIELD_SEPARATOR)
                when {
                    parts.size >= 3 -> {
                        val id = parts[0].ifBlank { UUID.randomUUID().toString() }
                        val label = parts[1].trim().ifBlank { "Extra Charge" }
                        val amount = parts[2].trim().toDoubleOrNull() ?: 0.0
                        if (amount > 0.0) ExtraChargeItem(id = id, label = label, amount = amount) else null
                    }
                    parts.size == 2 -> {
                        val label = parts[0].trim().ifBlank { "Extra Charge" }
                        val amount = parts[1].trim().toDoubleOrNull() ?: 0.0
                        if (amount > 0.0) ExtraChargeItem(label = label, amount = amount) else null
                    }
                    else -> null
                }
            }
        }

        fun totalAmount(items: List<ExtraChargeItem>): Double {
            return items.sumOf { it.amount.coerceAtLeast(0.0) }
        }
    }
}
