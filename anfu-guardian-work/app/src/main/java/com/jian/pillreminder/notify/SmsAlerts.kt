package com.jian.pillreminder.notify

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.jian.pillreminder.data.MedRepository
import com.jian.pillreminder.data.Medication
import com.jian.pillreminder.data.TimeOfDay

/**
 * 单手机方案的家属介入出口。
 *
 * App 不建立账号、联系人也不需要安装应用；在得到 SEND_SMS 授权后，老人手机直接
 * 给预设号码发送一条「请联系核实」或「请准备补药」短信。发送前始终检查开关、号码
 * 和权限，失败时保留 false，让下一次有效触发仍可重试。
 */
object SmsAlerts {
    private const val TAG = "PillSmsAlerts"

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

    fun sendMissedDose(
        context: Context,
        medication: Medication,
        date: String,
        time: TimeOfDay
    ): Boolean {
        val contact = MedRepository.get(context).data.value.familyContact
        if (!contact.missedDoseSmsEnabled || !contact.isUsable || !hasPermission(context)) return false
        val subject = if (contact.includeMedicationName) "${medication.name} 的" else ""
        val body = "【药时安心】${date} ${time.format()} 的${subject}服药提醒尚未收到确认，请联系老人核实。"
        return send(context, contact.phone, body)
    }

    fun sendLowStock(context: Context, medication: Medication): Boolean {
        val contact = MedRepository.get(context).data.value.familyContact
        val remaining = medication.stockRemaining ?: return false
        if (!contact.lowStockSmsEnabled || !contact.isUsable || !hasPermission(context)) return false
        if (medication.lowStockSmsSent) return false
        val subject = if (contact.includeMedicationName) medication.name else "老人常用药"
        val body = "【药时安心】${subject}库存不足，当前约剩 ${Reminders.formatDosage(remaining)}${medication.unit}，请留意补充。"
        return send(context, contact.phone, body).also { sent ->
            if (sent) MedRepository.get(context).markLowStockSmsSent(medication.id, syncWrite = true)
        }
    }

    private fun send(context: Context, phone: String, body: String): Boolean = runCatching {
        SmsManager.getDefault().sendTextMessage(phone.filter { it.isDigit() || it == '+' }, null, body, null, null)
        android.util.Log.i(TAG, "短信已交给系统发送")
        true
    }.getOrElse { error ->
        android.util.Log.e(TAG, "短信发送失败", error)
        false
    }
}
