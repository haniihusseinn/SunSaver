package no.uio.ifi.in2000.team54

import no.uio.ifi.in2000.team54.util.isNumeric
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StringUtilUnitTest {

    @Test
    fun isNumericShouldReturnTrue() {
        //TODO arrange
        val string = "123034123412341235123585"

        //TODO act
        val result = string.isNumeric()

        //TODO assert
        assertTrue(result)
    }

    @Test
    fun isNumericShouldReturnFalseEdge() {
        //TODO arrange
        val string = "%"

        //TODO act
        val result = string.isNumeric()

        //TODO assert
        assertFalse(result)
    }

    @Test
    fun isNumericShouldReturnFalse() {
        //TODO arrange
        val string = "te1st"

        //TODO act
        val result = string.isNumeric()

        //TODO assert
        assertFalse(result)
    }
}