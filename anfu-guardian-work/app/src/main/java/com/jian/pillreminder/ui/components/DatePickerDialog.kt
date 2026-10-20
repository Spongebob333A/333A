package com.jian.pillreminder.ui.components

import android.app.DatePickerDialog as AndroidDatePickerDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.ZoneId

/** 使用系统日历控件选择开始或结束日期，避免手动录入日期格式。 */
@Composable
fun DatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    title: String = "选择日期",
    supportingText: String? = null,
    minDate: LocalDate? = null,
    maxDate: LocalDate? = null
) {
    val context = LocalContext.current
    val latestConfirm = rememberUpdatedState(onConfirm)
    val latestDismiss = rememberUpdatedState(onDismiss)
    val zone = ZoneId.systemDefault()

    DisposableEffect(context, initialDate, minDate, maxDate) {
        val dialog = AndroidDatePickerDialog(
            context,
            { _, year, month, day -> latestConfirm.value(LocalDate.of(year, month + 1, day)) },
            initialDate.year,
            initialDate.monthValue - 1,
            initialDate.dayOfMonth
        ).apply {
            setTitle(title)
            minDate?.let { datePicker.minDate = it.atStartOfDay(zone).toInstant().toEpochMilli() }
            maxDate?.let { datePicker.maxDate = it.atStartOfDay(zone).toInstant().toEpochMilli() }
            setOnDismissListener { latestDismiss.value() }
        }
        dialog.show()
        onDispose {
            dialog.setOnDismissListener(null)
            dialog.dismiss()
        }
    }
}
