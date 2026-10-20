package com.jian.pillreminder.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jian.pillreminder.data.AppData
import com.jian.pillreminder.data.FamilyContact
import com.jian.pillreminder.data.DoseStatus
import com.jian.pillreminder.data.MedRepository
import com.jian.pillreminder.data.Medication
import com.jian.pillreminder.data.TimeOfDay
import com.jian.pillreminder.domain.DoseItem
import com.jian.pillreminder.domain.ScheduleEngine
import com.jian.pillreminder.notify.Reminders
import com.jian.pillreminder.notify.SmsAlerts
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class MedViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = MedRepository.get(app)

    val data: StateFlow<AppData> = repo.data

    /** 今日清单 / 历史页当前查看的日期。 */
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    /** 用于把"错过"判定刷新，每次 UI 主动调用 refreshNow() 时更新。 */
    private val _now = MutableStateFlow(LocalDateTime.now())
    val now: StateFlow<LocalDateTime> = _now.asStateFlow()

    val dosesForSelectedDate: StateFlow<List<DoseItem>> =
        combine(repo.data, _selectedDate) { d, date ->
            ScheduleEngine.dosesForDate(d.medications, d.logs, date, d.doseOverrides)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activeMedications: StateFlow<List<Medication>> =
        combine(repo.data, _selectedDate) { d, _ ->
            d.medications.sortedWith(compareBy({ it.archived }, { it.name }))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 库存告警：启用了库存管理且已低于阈值的药。 */
    val lowStockMedications: StateFlow<List<Medication>> =
        combine(repo.data, _selectedDate) { d, _ ->
            d.medications.filter { m ->
                !m.archived && m.stockRemaining != null && m.stockRemaining <= m.stockThreshold
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun refreshNow() {
        _now.value = LocalDateTime.now()
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun medicationById(id: String?): Medication? =
        id?.let { mid -> repo.data.value.medications.firstOrNull { it.id == mid } }

    fun newMedicationDraft(): Medication = Medication(
        id = UUID.randomUUID().toString(),
        name = "",
        startDate = LocalDate.now().toString(),
        colorIndex = repo.data.value.medications.size % 8
    )

    fun saveMedication(med: Medication) {
        // 修改进行中的药品时，旧时间对应的 15/30 分钟预警也必须撤销；
        // 否则用户改完提醒时间后，旧剂量仍可能继续触发二次提醒或短信检查。
        repo.data.value.medications.firstOrNull { it.id == med.id }?.times?.forEach {
            Reminders.cancelEscalationChecks(getApplication(), med.id, it)
        }
        val normalized = med.copy(
            name = med.name.trim(),
            times = med.times.distinct().sorted().ifEmpty { listOf(TimeOfDay(8, 0)) },
            // 用户动手改过并保存 → 说明他要真用这条，转为正式药品。
            // 不清这个标记的话，这条药会一直不排闹钟、永远不提醒。
            isSample = false
        )
        repo.upsertMedication(normalized)
        Reminders.scheduleFor(getApplication(), normalized)
        medicationById(normalized.id)?.let(::notifyIfLowStock)
    }

    fun deleteMedication(med: Medication) {
        med.times.forEach { Reminders.cancelEscalationChecks(getApplication(), med.id, it) }
        Reminders.cancelFor(getApplication(), med)
        Reminders.cancelResume(getApplication(), med.id)
        // 通知栏上可能还挂着这条药的提醒，不收掉的话点它没有任何反应
        Reminders.dismissAllFor(getApplication(), med)
        repo.deleteMedication(med.id)
    }

    fun toggleArchived(med: Medication) {
        val updated = med.copy(archived = !med.archived)
        repo.setArchived(med.id, updated.archived)
        if (updated.archived) {
            med.times.forEach { Reminders.cancelEscalationChecks(getApplication(), med.id, it) }
            Reminders.cancelFor(getApplication(), med)
            Reminders.cancelResume(getApplication(), med.id)
            Reminders.dismissAllFor(getApplication(), med)
        } else {
            Reminders.scheduleFor(getApplication(), updated)
            // 重新启用时，如果还在暂停期，恢复闹钟也要补回来
            if (updated.pausedUntil != null) {
                Reminders.scheduleResume(getApplication(), updated, updated.pausedUntil)
            }
        }
    }

    fun setStock(med: Medication, stock: Double?, threshold: Double?) {
        repo.setStock(med.id, stock, threshold)
        medicationById(med.id)?.let(::notifyIfLowStock)
    }

    /** 点一下切换：待服用 → 已服用 → 待服用。 */
    fun toggleTaken(item: DoseItem) {
        val next = if (item.status == DoseStatus.TAKEN) DoseStatus.PENDING else DoseStatus.TAKEN
        mark(item, next)
    }

    fun markSkipped(item: DoseItem) {
        val next = if (item.status == DoseStatus.SKIPPED) DoseStatus.PENDING else DoseStatus.SKIPPED
        mark(item, next)
    }

    private fun mark(item: DoseItem, status: DoseStatus) {
        repo.logDose(
            medicationId = item.medication.id,
            date = item.date.toString(),
            time = item.time,
            status = status,
            nowMillis = System.currentTimeMillis()
        )
        // 这次服药已经有结果了，未触发的延后闹钟（稍后提醒/临时改时间）都作废。
        // 用 clearDoseOutcome 而不是在这里各写一遍：之前就是因为这段清理在
        // ReminderReceiver 和这里各写一份，这份漏了撤延后闹钟，才出现
        // "先点稍后提醒、再在 App 里打卡，闹钟到点照样响"的 bug。
        Reminders.clearDoseOutcome(
            getApplication(), item.medication.id, item.date.toString(), item.time
        )

        if (status == DoseStatus.TAKEN) {
            val updated = repo.data.value.medications.firstOrNull { it.id == item.medication.id }
            updated?.let(::notifyIfLowStock)
        }
    }

    /** 录入、调整或自动扣减库存后统一检查，避免不同入口漏掉补药预警。 */
    private fun notifyIfLowStock(medication: Medication) {
        val remaining = medication.stockRemaining ?: return
        if (remaining <= medication.stockThreshold) {
            Reminders.showStockAlert(getApplication(), medication)
            SmsAlerts.sendLowStock(getApplication(), medication)
        }
    }

    fun rescheduleAllAlarms() = Reminders.rescheduleAll(getApplication())

    fun setSnoozeMinutes(minutes: Int) = repo.setSnoozeMinutes(minutes)

    fun setFamilyContact(contact: FamilyContact) = repo.setFamilyContact(contact)

    fun setElderLargeText(enabled: Boolean) = repo.setElderLargeText(enabled)

    /** 在今日清单内延后一次提醒；复用通知按钮相同的持久化闹钟机制。 */
    fun snoozeDose(item: DoseItem) {
        Reminders.snooze(
            getApplication(), item.medication, item.time, item.date.toString(),
            repo.data.value.snoozeMinutes
        )
        Reminders.dismissDoseNotification(getApplication(), item.medication.id, item.time)
    }

    fun setOngoingNotification(on: Boolean) = repo.setOngoingNotification(on)

    fun markSetupGuideShown() = repo.markSetupGuideShown()

    fun dismissHealthBanner() = repo.setHealthBannerDismissed(true)

    // ---- 暂停用药 ----

    /**
     * 暂停到 [until]（含当天）。传 null 立即恢复。
     *
     * 设置的同时要撤掉已排的闹钟和已弹出的通知——否则暂停期内旧闹钟照样会响，
     * 通知栏上那条也还挂着。这和 [toggleArchived] 是同一套处理。
     */
    fun setPaused(med: Medication, until: LocalDate?) {
        repo.setPausedUntil(med.id, until?.toString())
        // 用更新后的数据重排：scheduleFor 开头会先 cancelFor 清掉旧闹钟，
        // 然后按 nextOccurrence 排下一次——它会自动跳过暂停期，
        // 所以暂停和恢复都只需要这一次调用。
        val updated = repo.data.value.medications.firstOrNull { it.id == med.id } ?: return
        if (until != null) med.times.forEach { Reminders.cancelEscalationChecks(getApplication(), med.id, it) }
        Reminders.scheduleFor(getApplication(), updated)
        // 通知栏上可能还挂着这条药的提醒，暂停了就该收掉
        if (until != null) Reminders.dismissAllFor(getApplication(), updated)
        // 排/撤恢复提醒：暂停时排到恢复日早上，取消暂停时撤掉
        Reminders.scheduleResume(getApplication(), updated, until?.toString())
    }

    fun isPaused(med: Medication): Boolean = ScheduleEngine.isPausedNow(med)

    // ---- 临时改这次的时间 ----

    /** 把某一次服药挪到今天的另一个时刻。 */
    fun rescheduleDose(item: DoseItem, newTime: TimeOfDay) {
        Reminders.rescheduleOneDose(
            getApplication(), item.medication, item.time, item.date.toString(), newTime
        )
    }

    /** 撤销挪动，回到原定时刻。 */
    fun clearDoseReschedule(item: DoseItem) {
        Reminders.clearOneDoseReschedule(
            getApplication(), item.medication, item.time, item.date.toString()
        )
    }

    // ---- 示例数据 ----
    //
    // 刻意不在首次启动时自动塞入：示例药和真实用药混在一起会让人误判
    // （"我什么时候加过降压药？"），对一个吃药提醒应用来说这是危险的。
    // 改为在空状态里提供入口，由用户主动选择查看，且示例带 isSample 标记：
    // 界面上有「示例」角标、不会排闹钟（见 Reminders.scheduleFor）、可一键清除。

    val hasSampleData: StateFlow<Boolean> =
        repo.data.map { d -> d.medications.any { it.isSample } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** 放入示例药品，让用户看到界面长什么样。 */
    fun addSampleData() {
        if (repo.data.value.medications.any { it.isSample }) return
        val today = LocalDate.now().toString()
        listOf(
            Medication(
                id = UUID.randomUUID().toString(),
                name = "维生素 D",
                dosage = 1.0,
                unit = "粒",
                note = "随早餐服用吸收更好",
                colorIndex = 5,
                iconIndex = 1,
                times = listOf(TimeOfDay(8, 0)),
                mealRelation = com.jian.pillreminder.data.MealRelation.WITH_MEAL,
                startDate = today,
                stockRemaining = 30.0,
                stockThreshold = 5.0,
                isSample = true
            ),
            Medication(
                id = UUID.randomUUID().toString(),
                name = "降压药",
                dosage = 1.0,
                unit = "片",
                note = "",
                colorIndex = 0,
                iconIndex = 0,
                times = listOf(TimeOfDay(8, 30), TimeOfDay(20, 30)),
                mealRelation = com.jian.pillreminder.data.MealRelation.AFTER_MEAL,
                startDate = today,
                stockRemaining = 14.0,
                stockThreshold = 6.0,
                isSample = true
            )
        ).forEach { repo.upsertMedication(it) }
    }

    /** 清除全部示例药品及其记录。 */
    fun clearSampleData() {
        repo.data.value.medications.filter { it.isSample }.forEach { med ->
            Reminders.cancelFor(getApplication(), med)
            Reminders.dismissAllFor(getApplication(), med)
            repo.deleteMedication(med.id)
        }
    }
}
