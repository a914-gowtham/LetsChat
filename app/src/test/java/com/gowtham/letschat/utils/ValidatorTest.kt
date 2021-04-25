package com.gowtham.letschat.utils

import android.util.Log
import com.google.common.truth.Truth.assertThat
import org.junit.Test


class ValidatorTest {

    @Test
    fun invalidNo_returnsFalse() {
        val result = Validator.isValidNo("IN", "950561160")
        assertThat(result).isFalse()
    }

    @Test
    fun validNo_returnsTrue() {
        val result = Validator.isValidNo("IN", "9500561160")
        assertThat(result).isTrue()
    }

    @Test
    fun isMobileNumberEmpty_returnsTrue() {
        val result=Validator.isMobileNumberEmpty("")
        assertThat(result).isTrue()
    }
}

fun main() {
    print("main: ${findSum(4)}")
}

fun findSum(n: Int): Int {
    var sum = 0 // ---------------------> constant time
    for (i in 1..n)
        for (j in 1..i)
            sum++ // -------------------> it will run [n * (n + 1) / 2]
    return sum // ----------------------> constant time
}