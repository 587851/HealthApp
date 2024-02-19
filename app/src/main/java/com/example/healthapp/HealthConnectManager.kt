package com.example.healthapp

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.compose.runtime.mutableStateOf
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import java.time.Instant
import java.time.ZonedDateTime

// The minimum android level that can use Health Connect
const val MIN_SUPPORTED_SDK = Build.VERSION_CODES.O_MR1


class HealthConnectManager(private val context: Context) {
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    var availability = mutableStateOf(HealthConnectAvailability.NOT_SUPPORTED)
        private set

    init {
        checkAvailability()
    }

    private fun isSupported() = Build.VERSION.SDK_INT >= MIN_SUPPORTED_SDK

    fun checkAvailability() {
        availability.value = when {
            HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.INSTALLED
            isSupported() -> HealthConnectAvailability.NOT_INSTALLED
            else -> HealthConnectAvailability.NOT_SUPPORTED
        }
    }

    suspend fun hasAllPermissions(permissions: Set<String>): Boolean {
        return healthConnectClient.permissionController.getGrantedPermissions()
            .containsAll(permissions)
    }

    fun requestPermissionsActivityContract(): ActivityResultContract<Set<String>, Set<String>> {
        return PermissionController.createRequestPermissionResultContract()
    }

    suspend fun writeData(type: String, value: Double, start: ZonedDateTime, end: ZonedDateTime) {
        val timeNow = ZonedDateTime.now().withNano(0)

        // Create records based on the specified type
        val records = when (type) {
            "Steps" -> {
                val stepsRecord = StepsRecord(
                    startTime = start.toInstant(),
                    startZoneOffset = start.offset,
                    endTime = end.toInstant(),
                    endZoneOffset = end.offset,
                    count = value.toLong()
                )
                listOf(stepsRecord)
            }

            "Weigth" -> {
                val weightRecord = WeightRecord(
                    weight = Mass.kilograms(value),
                    time = timeNow.toInstant(),
                    zoneOffset = timeNow.offset
                )
                listOf(weightRecord)
            }

            "Calories burned" -> {
                val caloriesBurnedRecord = TotalCaloriesBurnedRecord(
                    startTime = start.toInstant(),
                    startZoneOffset = start.offset,
                    endTime = end.toInstant(),
                    endZoneOffset = end.offset,
                    energy = Energy.calories(value)
                )
                listOf(caloriesBurnedRecord)
            }

            else -> {
                // Handle other types or provide an error message as needed
                listOf()
            }
        }

        try {
            healthConnectClient.insertRecords(records)
            Toast.makeText(context, "Successfully insert records", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, e.message.toString(), Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun readDataFromLastWeekToString(type: String): String {

        val start = ZonedDateTime.now().minusWeeks(1).toInstant()
        val end = Instant.now();

        val request = when (type) {
            "Steps" -> {
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            }

            "Weigth" -> {
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            }

            "Calories burned" -> {
                ReadRecordsRequest(
                    recordType = TotalCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            }

            else -> {
                throw IllegalArgumentException("Invalid type: $type. Expected 'Steps', 'Weigth', or 'Calories burned'.")
            }
        }

        val response = healthConnectClient.readRecords(request)
        val reversedRecords = response.records.reversed()
        val recordData = StringBuilder()
        for (record in reversedRecords) {
            when (record) {
                is StepsRecord -> recordData.append("Steps: ${record.count}\nStart: ${record.startTime}\nEnd: ${record.endTime}\n\n")
                is WeightRecord -> recordData.append("Weight: ${record.weight} kg\nTime: ${record.time}\n\n")
                is TotalCaloriesBurnedRecord -> recordData.append("Burned: ${record.energy.inKilocalories} kcal\nStart: ${record.startTime}\nEnd: ${record.endTime}\n\n")
            }
        }
        return recordData.toString()
    }

    suspend fun readDataFromLastWeekToRecord(type: String): List<Record> {

        val start = ZonedDateTime.now().minusWeeks(1).toInstant()
        val end = Instant.now();

        val request = when (type) {
            "Steps" -> {
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            }

            "Weigth" -> {
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            }

            "Calories burned" -> {
                ReadRecordsRequest(
                    recordType = TotalCaloriesBurnedRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            }

            else -> {
                throw IllegalArgumentException("Invalid type: $type. Expected 'Steps', 'Weigth', or 'Calories burned'.")
            }
        }
        val response = healthConnectClient.readRecords(request)
        return response.records.reversed()
    }


    enum class HealthConnectAvailability {
        INSTALLED,
        NOT_INSTALLED,
        NOT_SUPPORTED
    }

}