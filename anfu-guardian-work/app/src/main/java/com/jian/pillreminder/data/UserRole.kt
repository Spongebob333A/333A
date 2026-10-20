package com.jian.pillreminder.data

/** App 内演示的使用角色。账号与跨设备绑定由后续后端版本提供。 */
enum class UserRole(val label: String) {
    ELDER("老人端"),
    FAMILY("家属端")
}
