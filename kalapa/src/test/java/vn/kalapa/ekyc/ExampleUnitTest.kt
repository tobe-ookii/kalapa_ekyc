package vn.kalapa.ekyc

import org.junit.Test

import org.junit.Assert.*
import vn.kalapa.ekyc.managers.AESCryptor

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun calculateData() {
        println("CalculateData: \n " + AESCryptor.encryptText("d37399d39460473dac39a380bc407f71"))

    }
}