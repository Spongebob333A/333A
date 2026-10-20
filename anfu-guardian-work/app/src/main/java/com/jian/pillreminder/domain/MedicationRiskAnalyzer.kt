package com.jian.pillreminder.domain

import com.jian.pillreminder.data.Medication
import java.text.Normalizer
import java.util.Locale

/**
 * 当前离线原型只做资料完整性预检，不判断临床相互作用。
 * 规则刻意保守：提醒重名药品，以及处方备注里明确出现的风险字样。
 */
object MedicationRiskAnalyzer {
    data class Finding(val title: String, val detail: String)

    fun inspect(candidate: Medication, existing: List<Medication>): List<Finding> = buildList {
        val normalizedName = normalize(candidate.name)
        if (normalizedName.isNotEmpty() && existing.any {
                it.id != candidate.id && normalize(it.name) == normalizedName
            }
        ) {
            add(Finding("发现同名药品", "药箱中已有同名药品，请核对是否重复录入或剂量不同。"))
        }

        val cautionPhrases = listOf("避免同服", "不可同服", "不要同服", "相互作用", "用药冲突")
        val notes = existing.filter { it.id != candidate.id } + candidate
        notes.filter { med -> cautionPhrases.any { phrase -> med.note.contains(phrase) } }
            .forEach { med ->
                add(Finding("备注中有待核实提示", "「${med.name}」的备注含有相互作用相关提示，请家属按说明书或咨询药师核对。"))
            }
    }

    private fun normalize(value: String): String = Normalizer
        .normalize(value, Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT)
        .filterNot { it.isWhitespace() }
}
