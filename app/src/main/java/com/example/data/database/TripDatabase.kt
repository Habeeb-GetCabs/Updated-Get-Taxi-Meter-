package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.util.VendorLogoRenderer

@Database(
    entities = [
        TripEntity::class,
        DriverProfileEntity::class,
        ActiveTripRecord::class,
        VendorProfileEntity::class
    ],
    version = 10,
    exportSchema = false
)
abstract class TripDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao

    companion object {
        @Volatile
        private var INSTANCE: TripDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE trips ADD COLUMN isOutOfCity INTEGER NOT NULL DEFAULT 0")
                    db.execSQL("ALTER TABLE trips ADD COLUMN outOfCitySurcharge REAL NOT NULL DEFAULT 0.0")
                    db.execSQL("ALTER TABLE trips ADD COLUMN baseFare REAL NOT NULL DEFAULT 0.0")
                    db.execSQL("ALTER TABLE trips ADD COLUMN farePerKm REAL NOT NULL DEFAULT 0.0")
                    db.execSQL("ALTER TABLE trips ADD COLUMN waitFarePerMin REAL NOT NULL DEFAULT 0.0")
                    db.execSQL("ALTER TABLE trips ADD COLUMN currency TEXT NOT NULL DEFAULT '₹'")
                } catch (_: Exception) {}
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try { db.execSQL("ALTER TABLE trips ADD COLUMN tripNumber TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN customerName TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN customerMobile TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN customerAddress TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN driverName TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN driverMobile TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN vehicleNumber TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN vehicleModel TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN vehicleType TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}

                try { db.execSQL("ALTER TABLE active_trip ADD COLUMN tripNumber TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS driver_profile (
                        id INTEGER PRIMARY KEY NOT NULL,
                        driverName TEXT NOT NULL DEFAULT '',
                        driverMobile TEXT NOT NULL DEFAULT '',
                        vehicleNumber TEXT NOT NULL DEFAULT '',
                        vehicleModel TEXT NOT NULL DEFAULT '',
                        vehicleType TEXT NOT NULL DEFAULT 'Sedan Taxi',
                        driverId TEXT NOT NULL DEFAULT '',
                        photoUri TEXT
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try { db.execSQL("ALTER TABLE trips ADD COLUMN serviceType TEXT NOT NULL DEFAULT 'LOCAL'") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN rentalBaseHours REAL NOT NULL DEFAULT 1.0") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN rentalBaseKm REAL NOT NULL DEFAULT 0.0") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN rentalBaseFare REAL NOT NULL DEFAULT 0.0") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN rentalExtraKmRate REAL NOT NULL DEFAULT 0.0") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN rentalExtraHrRate REAL NOT NULL DEFAULT 0.0") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN outstationRatePerKm REAL NOT NULL DEFAULT 0.0") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN outstationMinKm REAL NOT NULL DEFAULT 0.0") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN outstationDriverBata REAL NOT NULL DEFAULT 0.0") } catch (_: Exception) {}

                try { db.execSQL("ALTER TABLE active_trip ADD COLUMN serviceType TEXT NOT NULL DEFAULT 'LOCAL'") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE active_trip ADD COLUMN rentalBaseHours REAL NOT NULL DEFAULT 1.0") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE active_trip ADD COLUMN rentalBaseKm REAL NOT NULL DEFAULT 0.0") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE active_trip ADD COLUMN rentalBaseFare REAL NOT NULL DEFAULT 0.0") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE active_trip ADD COLUMN rentalExtraKmRate REAL NOT NULL DEFAULT 0.0") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE active_trip ADD COLUMN rentalExtraHrRate REAL NOT NULL DEFAULT 0.0") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE active_trip ADD COLUMN outstationRatePerKm REAL NOT NULL DEFAULT 0.0") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE active_trip ADD COLUMN outstationMinKm REAL NOT NULL DEFAULT 0.0") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE active_trip ADD COLUMN outstationDriverBata REAL NOT NULL DEFAULT 0.0") } catch (_: Exception) {}
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try { db.execSQL("ALTER TABLE trips ADD COLUMN billableHours INTEGER NOT NULL DEFAULT 1") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN outstationMinFare REAL NOT NULL DEFAULT 0.0") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN outstationWaitRate REAL NOT NULL DEFAULT 0.0") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN outstationAdditionalCharges REAL NOT NULL DEFAULT 0.0") } catch (_: Exception) {}

                try { db.execSQL("ALTER TABLE active_trip ADD COLUMN billableHours INTEGER NOT NULL DEFAULT 1") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE active_trip ADD COLUMN outstationMinFare REAL NOT NULL DEFAULT 0.0") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE active_trip ADD COLUMN outstationWaitRate REAL NOT NULL DEFAULT 0.0") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE active_trip ADD COLUMN outstationAdditionalCharges REAL NOT NULL DEFAULT 0.0") } catch (_: Exception) {}
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try { db.execSQL("ALTER TABLE driver_profile ADD COLUMN qrCodeUri TEXT") } catch (_: Exception) {}
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try { db.execSQL("ALTER TABLE driver_profile ADD COLUMN upiId TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try { db.execSQL("ALTER TABLE trips ADD COLUMN extraChargesJson TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE active_trip ADD COLUMN extraChargesJson TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add immutable vendor snapshot columns to trips table
                try { db.execSQL("ALTER TABLE trips ADD COLUMN vendorId TEXT NOT NULL DEFAULT 'get_taxi'") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN vendorName TEXT NOT NULL DEFAULT 'GET TAXI'") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN vendorLogoAsset TEXT NOT NULL DEFAULT 'drawable/get_taxi_header_logo'") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN vendorContactNumber TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN vendorBusinessAddress TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN vendorEmail TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN vendorGstin TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN vendorInvoiceDetails TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN vendorInvoiceNumber TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}

                // 2. Add locked vendor snapshot columns to active_trip table
                try { db.execSQL("ALTER TABLE active_trip ADD COLUMN vendorId TEXT NOT NULL DEFAULT 'get_taxi'") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE active_trip ADD COLUMN vendorName TEXT NOT NULL DEFAULT 'GET TAXI'") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE active_trip ADD COLUMN vendorLogoAsset TEXT NOT NULL DEFAULT 'drawable/get_taxi_header_logo'") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE active_trip ADD COLUMN vendorContactNumber TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE active_trip ADD COLUMN vendorBusinessAddress TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE active_trip ADD COLUMN vendorEmail TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE active_trip ADD COLUMN vendorGstin TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE active_trip ADD COLUMN vendorInvoiceDetails TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}

                // 3. Create vendor_profiles table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS vendor_profiles (
                        vendorId TEXT PRIMARY KEY NOT NULL,
                        vendorName TEXT NOT NULL,
                        logoAsset TEXT NOT NULL,
                        contactNumber TEXT NOT NULL DEFAULT '',
                        businessAddress TEXT NOT NULL DEFAULT '',
                        email TEXT NOT NULL DEFAULT '',
                        gstin TEXT NOT NULL DEFAULT '',
                        invoiceDisplayDetails TEXT NOT NULL DEFAULT '',
                        isActive INTEGER NOT NULL DEFAULT 1,
                        isOriginalGetTaxi INTEGER NOT NULL DEFAULT 0,
                        sortOrder INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )

                // 4. Seed GET TAXI + 20 built-in vendors idempotently
                seedDefaultVendorsSql(db)
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                try { db.execSQL("ALTER TABLE trips ADD COLUMN routePointsJson TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN routeMapImagePath TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN invoiceTimestamp INTEGER NOT NULL DEFAULT 0") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN invoiceDateText TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
                try { db.execSQL("ALTER TABLE trips ADD COLUMN invoiceTimeText TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}

                try { db.execSQL("ALTER TABLE active_trip ADD COLUMN routePointsJson TEXT NOT NULL DEFAULT ''") } catch (_: Exception) {}
            }
        }

        fun seedDefaultVendorsSql(db: SupportSQLiteDatabase) {
            try {
                val now = System.currentTimeMillis()
                val seedVendors = VendorLogoRenderer.buildInitialSeedVendors()
                seedVendors.forEach { v ->
                    db.execSQL(
                        """
                        INSERT OR IGNORE INTO vendor_profiles (
                            vendorId, vendorName, logoAsset, contactNumber, businessAddress,
                            email, gstin, invoiceDisplayDetails, isActive, isOriginalGetTaxi, sortOrder, createdAt
                        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """.trimIndent(),
                        arrayOf<Any>(
                            v.vendorId,
                            v.vendorName,
                            v.logoAsset,
                            v.contactNumber,
                            v.businessAddress,
                            v.email,
                            v.gstin,
                            v.invoiceDisplayDetails,
                            if (v.isActive) 1 else 0,
                            if (v.isOriginalGetTaxi) 1 else 0,
                            v.sortOrder,
                            now
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        fun getDatabase(context: Context): TripDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TripDatabase::class.java,
                    "taxi_meter_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10
                    )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            seedDefaultVendorsSql(db)
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            seedDefaultVendorsSql(db)
                        }
                    })
                    .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
