package com.gowtham.letschat.utils

import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber

object Validator {

    fun isValidNo(code: String, mobileNo: String): Boolean {
        try {
            val phoneUtil = PhoneNumberUtil.getInstance()
            val phNumberProto: PhoneNumber = phoneUtil.parse(
                mobileNo, code
            )
            return phoneUtil.isValidNumber(phNumberProto)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun isMobileNumberEmpty(mobileNo: String?): Boolean{
        return mobileNo.isNullOrEmpty()
    }

}