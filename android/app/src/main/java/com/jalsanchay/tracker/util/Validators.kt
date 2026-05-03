package com.jalsanchay.tracker.util

import java.time.LocalDate
import java.time.format.DateTimeParseException

sealed class ValidationResult {
    data object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()
}

val ValidationResult.isValid get() = this is ValidationResult.Valid
val ValidationResult.message get() = (this as? ValidationResult.Invalid)?.message

fun validateRainfallMm(input: String): ValidationResult {
    if (input.isBlank()) return ValidationResult.Invalid("Rainfall amount is required")
    val value = input.toDoubleOrNull() ?: return ValidationResult.Invalid("Please enter a valid number")
    if (value < 0) return ValidationResult.Invalid("Rainfall cannot be negative")
    if (value > 500) return ValidationResult.Invalid("Value seems too high — maximum is 500mm per day")
    return ValidationResult.Valid
}

fun validateRoofArea(input: String, unit: String): ValidationResult {
    if (input.isBlank()) return ValidationResult.Invalid("Roof area is required")
    val value = input.toDoubleOrNull() ?: return ValidationResult.Invalid("Please enter a valid number")
    if (value <= 0) return ValidationResult.Invalid("Roof area must be greater than zero")
    if (unit == "sqft" && value > 50000) return ValidationResult.Invalid("Area seems too large — please check")
    if (unit == "sqm" && value > 4645) return ValidationResult.Invalid("Area seems too large — please check")
    return ValidationResult.Valid
}

fun validateTankCapacity(input: String): ValidationResult {
    if (input.isBlank()) return ValidationResult.Invalid("Tank capacity is required")
    val value = input.toDoubleOrNull() ?: return ValidationResult.Invalid("Please enter a valid number")
    if (value <= 0) return ValidationResult.Invalid("Tank capacity must be greater than zero")
    if (value > 1000000) return ValidationResult.Invalid("Capacity seems too large — please check")
    return ValidationResult.Valid
}

fun validateDate(dateString: String): ValidationResult {
    if (dateString.isBlank()) return ValidationResult.Invalid("Date is required")
    val date = try {
        LocalDate.parse(dateString)
    } catch (_: DateTimeParseException) {
        return ValidationResult.Invalid("Invalid date format")
    }
    val today = LocalDate.now()
    if (date.isAfter(today)) return ValidationResult.Invalid("Date cannot be in the future")
    if (date.isBefore(today.minusDays(365))) return ValidationResult.Invalid("Date cannot be more than one year ago")
    return ValidationResult.Valid
}
