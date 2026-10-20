package com.jian.pillreminder

import com.jian.pillreminder.data.FamilyContact
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyContactTest {

    @Test
    fun `手机号至少七位才可用于短信预警`() {
        assertFalse(FamilyContact(phone = "123456").isUsable)
        assertTrue(FamilyContact(phone = "13800138000").isUsable)
    }

    @Test
    fun `国际号码中的加号和空格不影响有效性判断`() {
        assertTrue(FamilyContact(phone = "+86 13800138000").isUsable)
    }
}
