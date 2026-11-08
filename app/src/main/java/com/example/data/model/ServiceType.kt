package com.example.data.model

import java.util.Locale

enum class ServiceType(val id: String, val title: String) {
    LOCAL("LOCAL", "LOCAL / METERED"),
    RENTAL("RENTAL", "HOURLY RENTAL"),
    OUTSTATION("OUTSTATION", "OUTSTATION");

    companion object {
        fun fromId(id: String?): ServiceType {
            return when (id?.uppercase(Locale.US)?.trim()) {
                "RENTAL", "HOURLY RENTAL" -> RENTAL
                "OUTSTATION" -> OUTSTATION
                else -> LOCAL
            }
        }
    }
}
