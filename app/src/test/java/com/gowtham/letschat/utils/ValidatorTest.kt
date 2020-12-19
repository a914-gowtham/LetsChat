package com.gowtham.letschat.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test


class ValidatorTest {

    @Test
    fun invalidNo_returnsFalse() {
        val result = Validator.isValidNo("IN","950561160")
        assertThat(result).isFalse()
    }

    @Test
    fun validNo_returnsTrue() {
        val result = Validator.isValidNo("IN","9500561160")
        assertThat(result).isTrue()
    }

    @Test
    fun isMobileNumberEmpty_returnsTrue() {
        val result=Validator.isMobileNumberEmpty("")
        assertThat(result).isTrue()
    }
}