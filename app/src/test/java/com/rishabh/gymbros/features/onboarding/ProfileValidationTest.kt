package com.rishabh.gymbros.features.onboarding

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class ProfileValidationTest {
    @Test fun numericFieldsRejectTypingAndPastingLetters() {
        for (value in listOf("test", "72kg", "1e2", "-5", "5 6", "NaN")) {
            assertFalse(acceptsNumericInput(value))
            assertFalse(acceptsNumericInput(value, decimal = true))
        }
        assertTrue(acceptsNumericInput(""))
        assertTrue(acceptsNumericInput("25"))
        assertFalse(acceptsNumericInput("25.5"))
        assertTrue(acceptsNumericInput("72.5", decimal = true))
        assertFalse(acceptsNumericInput("72.5.1", decimal = true))
    }

    @Test fun feetAndInchesConvertToStoredCentimeters() {
        assertEquals(178, heightFromFeet("5", "10"))
        assertEquals(183, heightFromFeet("6", ""))
        assertNull(heightFromFeet("", ""))
        assertNull(heightFromFeet("", "10"))
        assertNull(heightFromFeet("5", "12"))
        assertNull(heightFromFeet("5.5", ""))
        assertNull(heightFromFeet("9", "0"))
        assertNull(heightFromFeet("999999999", "0"))
    }

    @Test fun requiredProfileAllowsMissingOptionalDetails() {
        assertNull(profileInputError("Alex", "25", "", ""))
    }

    @Test fun nameAndAgeAreRequired() {
        assertNotNull(profileInputError("  ", "25", "", ""))
        for (age in listOf("", "abc", "25.5", "0", "9", "121")) {
            assertNotNull(profileInputError("Alex", age, "", ""))
        }
    }

    @Test fun optionalMeasurementsCannotBeSilentlyClampedOrNonFinite() {
        for (height in listOf("abc", "79", "251")) {
            assertNotNull(profileInputError("Alex", "25", height, ""))
        }
        for (weight in listOf("abc", "19", "501", "NaN", "Infinity")) {
            assertNotNull(profileInputError("Alex", "25", "", weight))
        }
        assertNull(profileInputError("Alex", "25", "180", "72.5"))
    }
}
