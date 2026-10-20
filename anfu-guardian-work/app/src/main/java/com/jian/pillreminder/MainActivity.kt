package com.jian.pillreminder

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jian.pillreminder.notify.ReminderHealth
import com.jian.pillreminder.notify.Reminders
import com.jian.pillreminder.ui.MedViewModel
import androidx.core.content.ContextCompat
import com.jian.pillreminder.ui.screens.EditMedicationScreen
import com.jian.pillreminder.ui.screens.RecentRecordsScreen
import com.jian.pillreminder.ui.screens.MedicationsScreen
import com.jian.pillreminder.ui.screens.ReminderHealthBanner
import com.jian.pillreminder.ui.screens.ReminderSetupScreen
import com.jian.pillreminder.ui.screens.ScanLeafletScreen
import com.jian.pillreminder.ui.screens.TodayScreen
import com.jian.pillreminder.ui.screens.SettingsScreen
import com.jian.pillreminder.ui.screens.FamilyContactScreen
import com.jian.pillreminder.ui.components.suggestIconForUnit
import com.jian.pillreminder.ui.components.PillBrandMark
import com.jian.pillreminder.ui.theme.PillReminderTheme
import com.jian.pillreminder.data.UserRole
import com.jian.pillreminder.domain.MedicationRiskAnalyzer
import com.jian.pillreminder.ui.screens.RoleSelectionScreen
import com.jian.pillreminder.ui.screens.MedicationRiskReviewDialog

class MainActivity : ComponentActivity() {

    /** 每次从桌面图标/通知重新进入时自增，用来把导航栈重置回今日清单。 */
    private val relaunchSignal = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        // 不恢复上次的导航栈：吃药提醒每次打开都应先看到"今天要吃什么"，
        // 而不是停在上次离开时的编辑页。传 null 让 Compose Navigation 从起始页开始。
        super.onCreate(null)
        enableEdgeToEdge()
        Reminders.ensureChannels(this)
        setContent {
            var largeText by remember { mutableStateOf(false) }
            PillReminderTheme(largeText = largeText) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PillApp(relaunchSignal.intValue, onLargeTextChange = { largeText = it })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 用户重新打开 App（而不是在应用内部导航）时，回到今日清单，
        // 不要停在上次离开时的编辑页——吃药提醒打开就该先看到"今天要吃什么"。
        relaunchSignal.intValue++
    }
}

private sealed class Dest(val route: String, val label: String) {
    data object Today : Dest("today", "服药情况")
    data object Meds : Dest("meds", "用药方案")
    data object History : Dest("history", "记录")
    data object Edit : Dest("edit", "编辑")
    data object Scan : Dest("scan", "拍说明书")
    data object Setup : Dest("setup", "提醒设置")
    data object FamilyContact : Dest("family_contact", "家属短信联系人")

    // 注意 route 不能以已有的 route 为前缀：下面 isEditing 用的是 startsWith。
    // "settings" 不是 "setup" 的前缀，安全。
    data object Settings : Dest("settings", "设置")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PillApp(relaunchSignal: Int = 0, onLargeTextChange: (Boolean) -> Unit) {
    val vm: MedViewModel = viewModel()
    val appData by vm.data.collectAsState()
    var selectedRole by remember { mutableStateOf<UserRole?>(null) }
    // 用 remember 而非 rememberSaveable 语义的 rememberNavController：
    // 后者会从 SavedStateRegistry 恢复上次的 back stack，导致重开 App 停在编辑页。
    // 吃药提醒每次打开都应落在今日清单，所以这里每个进程都新建一个干净的控制器。
    val context0 = LocalContext.current
    val nav = remember(selectedRole) {
        androidx.navigation.NavHostController(context0).apply {
            navigatorProvider.addNavigator(androidx.navigation.compose.ComposeNavigator())
            navigatorProvider.addNavigator(androidx.navigation.compose.DialogNavigator())
        }
    }
    // OCR 识别结果的交接站：扫描页写入，编辑页读取后预填表单
    var scanResult by remember { mutableStateOf<com.jian.pillreminder.domain.LeafletParser.Result?>(null) }
    // 正在编辑的药品 id（null = 新建）。放在 state 里而不是路由参数里，
    // 避免带参路由被 Navigation 恢复导致重开 App 直接落在编辑页。
    var editingId by remember { mutableStateOf<String?>(null) }
    var riskCandidate by remember { mutableStateOf<com.jian.pillreminder.data.Medication?>(null) }
    var riskFindings by remember { mutableStateOf(emptyList<MedicationRiskAnalyzer.Finding>()) }
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route

    val context = LocalContext.current
    var smsPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> smsPermissionGranted = granted }

    // 首次进入：把闹钟排上（示例数据改为用户在空状态里主动选择添加）
    LaunchedEffect(Unit) {
        vm.rescheduleAllAlarms()
    }

    // 从桌面图标/通知重新进入时（onNewIntent）回到今日清单
    LaunchedEffect(relaunchSignal) {
        if (relaunchSignal > 0) {
            scanResult = null
            if (selectedRole == null) return@LaunchedEffect
            nav.navigate(if (selectedRole == UserRole.FAMILY) Dest.Meds.route else Dest.Today.route) {
                popUpTo(nav.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    // 每次回到前台刷新"是否错过"的判定
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refreshNow()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 提醒可靠性体检：通知 / 精确闹钟 / 电池优化豁免
    var healthChecks by remember { mutableStateOf(ReminderHealth.checks(context)) }
    fun refreshHealth() { healthChecks = ReminderHealth.checks(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        refreshHealth()
        if (granted) vm.rescheduleAllAlarms()
    }

    fun fix(check: ReminderHealth.Check) {
        when (val a = check.action) {
            is ReminderHealth.Action.RequestNotificationPermission ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    ReminderHealth.launchFirstAvailable(context, listOf(ReminderHealth.appDetailsSettings(context)))
                }
            is ReminderHealth.Action.OpenSettings ->
                ReminderHealth.launchFirstAvailable(context, a.intents)
            ReminderHealth.Action.None -> { /* 纯展示项，无操作 */ }
        }
    }

    // 每次回到前台都重新体检（用户可能刚在系统设置里改过）
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshHealth()
                vm.rescheduleAllAlarms()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 首次启动且还有未就绪项时，直接进引导页
    var guideChecked by remember { mutableStateOf(false) }
    LaunchedEffect(appData.setupGuideShown, healthChecks, selectedRole) {
        if (guideChecked) return@LaunchedEffect
        if (selectedRole == null) return@LaunchedEffect
        guideChecked = true
        if (!appData.setupGuideShown && healthChecks.any { !it.granted }) nav.navigate(Dest.Setup.route)
    }

    LaunchedEffect(selectedRole, appData.elderLargeText) {
        onLargeTextChange(selectedRole == UserRole.ELDER && appData.elderLargeText)
    }

    if (selectedRole == null) {
        RoleSelectionScreen(
            largeText = appData.elderLargeText,
            onLargeTextChange = { vm.setElderLargeText(it); onLargeTextChange(it) },
            onChoose = { role ->
                selectedRole = role
                onLargeTextChange(role == UserRole.ELDER && appData.elderLargeText)
            }
        )
        return
    }
    val homeRoute = if (selectedRole == UserRole.FAMILY) Dest.Meds.route else Dest.Today.route

    // 这些是自带顶栏的全屏页面，要把外层顶栏/底栏/悬浮按钮藏掉，
    // 否则会出现两条标题栏
    val isEditing = route?.startsWith(Dest.Edit.route) == true ||
        route?.startsWith(Dest.Scan.route) == true ||
        route?.startsWith(Dest.Setup.route) == true ||
        route?.startsWith(Dest.Settings.route) == true ||
        route?.startsWith(Dest.FamilyContact.route) == true

    Scaffold(
        topBar = {
            if (!isEditing) {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PillBrandMark(size = 28.dp)
                            Text(
                                "药时安心",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { nav.navigate(Dest.Settings.route) }) {
                            Icon(Icons.Filled.Settings, contentDescription = "设置")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        bottomBar = {
            if (!isEditing) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.background) {
                    NavigationBarItem(
                        selected = route == Dest.Today.route,
                        onClick = {
                            nav.navigate(Dest.Today.route) {
                                // 三个一级 tab 互不堆叠：切 tab 前把当前栈弹回起始页并存档，
                                // 切过去时恢复该 tab 自己的状态——根 tab 永远在返回栈底。
                                // 这样系统返回手势在任一根 tab 上就是退出 App，
                                // 不会再层层往回翻之前来回切的那些页面。
                                popUpTo(homeRoute) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                if (route == Dest.Today.route) Icons.Filled.Today
                                else Icons.Outlined.Today,
                                contentDescription = null
                            )
                        },
                        label = { Text(Dest.Today.label) }
                    )
                    if (selectedRole == UserRole.FAMILY) NavigationBarItem(
                        selected = route == Dest.Meds.route,
                        onClick = {
                            nav.navigate(Dest.Meds.route) {
                                popUpTo(homeRoute) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                if (route == Dest.Meds.route) Icons.Filled.Medication
                                else Icons.Outlined.Medication,
                                contentDescription = null
                            )
                        },
                        label = { Text(Dest.Meds.label) }
                    )
                    NavigationBarItem(
                        selected = route == Dest.History.route,
                        onClick = {
                            nav.navigate(Dest.History.route) {
                                popUpTo(homeRoute) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                if (route == Dest.History.route) Icons.Filled.Insights
                                else Icons.Outlined.Insights,
                                contentDescription = null
                            )
                        },
                        label = { Text(Dest.History.label) }
                    )
                }
            }
        },
        floatingActionButton = {}
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = if (selectedRole == UserRole.FAMILY) Dest.Meds.route else Dest.Today.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Dest.Today.route) {
                TodayScreen(
                    vm = vm,
                    onAddMedication = { editingId = null; nav.navigate(Dest.Edit.route) },
                    onOpenMedication = { id -> editingId = id; nav.navigate(Dest.Edit.route) },
                    isElderMode = selectedRole == UserRole.ELDER,
                    canManageMedication = selectedRole == UserRole.FAMILY,
                    allowDoseFeedback = selectedRole == UserRole.ELDER,
                    permissionBanner = run {
                        val pending = healthChecks.filterNot { it.granted }
                        val showHealth = pending.isNotEmpty() && !appData.healthBannerDismissed
                        when {
                            selectedRole != UserRole.ELDER -> null
                            showHealth -> {
                                {
                                    ReminderHealthBanner(
                                        pending = pending,
                                        onOpenSetup = { nav.navigate(Dest.Setup.route) },
                                        onDismiss = { vm.dismissHealthBanner() }
                                    )
                                }
                            }
                            else -> null
                        }
                    }
                )
            }

            composable(Dest.Meds.route) {
                if (selectedRole == UserRole.FAMILY) {
                    MedicationsScreen(
                        vm = vm,
                        onOpenMedication = { id -> editingId = id; nav.navigate(Dest.Edit.route) },
                        onAddMedication = { editingId = null; nav.navigate(Dest.Edit.route) }
                    )
                } else Text("药品由家属端管理")
            }

            composable(Dest.History.route) {
                RecentRecordsScreen(vm = vm)
            }

            composable(Dest.Edit.route) {
                if (selectedRole != UserRole.FAMILY) {
                    LaunchedEffect(Unit) { nav.popBackStack() }
                    return@composable
                }
                val id = editingId
                val existing = vm.medicationById(id)
                val ocr = scanResult
                // 新建时只生成一次草稿；有 OCR 结果就把识别到的字段预填进去
                val draft = remember(id, ocr) {
                    val base = existing ?: vm.newMedicationDraft()
                    if (existing != null || ocr == null) base else base.copy(
                        name = ocr.name ?: base.name,
                        dosage = ocr.dosage ?: base.dosage,
                        unit = ocr.unit ?: base.unit,
                        // 图标按剂型分类，识别到的单位就是剂型线索（"粒"→胶囊）
                        iconIndex = ocr.unit?.let { suggestIconForUnit(it) } ?: base.iconIndex,
                        times = ocr.suggestedTimes.ifEmpty { base.times },
                        schedule = ocr.schedule ?: base.schedule,
                        mealRelation = ocr.mealRelation ?: base.mealRelation
                    )
                }
                val note = remember(ocr, existing) {
                    // 示例药不排闹钟，得说清楚，否则用户以为提醒坏了
                    if (existing?.isSample == true) {
                        "这是示例药品，不会真的提醒你。改动并保存后它就会变成正式药品，按你设的时间提醒。"
                    } else if (existing != null || ocr == null) null
                    else {
                        val filled = buildList {
                            if (ocr.name != null) add("药名")
                            if (ocr.dosage != null) add("剂量")
                            if (ocr.timesPerDay != null) add("服药时间")
                            if (ocr.schedule != null) add("用药频率")
                            if (ocr.mealRelation != null) add("进餐要求")
                        }
                        if (filled.isEmpty()) null
                        else "已根据说明书填好：${filled.joinToString("、")}。请核对无误后再保存。"
                    }
                }

                EditMedicationScreen(
                    initial = draft,
                    isNew = existing == null,
                    onSave = { med ->
                        riskCandidate = med
                        riskFindings = MedicationRiskAnalyzer.inspect(med, appData.medications)
                    },
                    onDelete = existing?.let {
                        {
                            vm.deleteMedication(it)
                            nav.popBackStack()
                        }
                    },
                    onBack = { scanResult = null; nav.popBackStack() },
                    onScanLeaflet = if (existing == null) {
                        { nav.navigate(Dest.Scan.route) }
                    } else null,
                    prefillNote = note
                )
            }

            composable(Dest.Setup.route) {
                ReminderSetupScreen(
                    checks = healthChecks,
                    vendorHint = remember { ReminderHealth.vendorHint() },
                    onFix = { fix(it) },
                    onDone = {
                        vm.markSetupGuideShown()
                        vm.rescheduleAllAlarms()
                        nav.popBackStack()
                    },
                    onSkip = {
                        vm.markSetupGuideShown()
                        vm.dismissHealthBanner()
                        nav.popBackStack()
                    }
                )
            }

            composable(Dest.Scan.route) {
                if (selectedRole != UserRole.FAMILY) {
                    LaunchedEffect(Unit) { nav.popBackStack() }
                    return@composable
                }
                ScanLeafletScreen(
                    onUseResult = { r ->
                        scanResult = r
                        // 回到编辑页（它会读取 scanResult 预填表单）
                        nav.popBackStack()
                    },
                    onBack = { nav.popBackStack() }
                )
            }

            composable(Dest.Settings.route) {
                SettingsScreen(
                    snoozeMinutes = appData.snoozeMinutes,
                    ongoingNotification = appData.ongoingNotification,
                    currentRole = selectedRole ?: UserRole.ELDER,
                    largeText = appData.elderLargeText,
                    onSnoozeMinutesChange = { vm.setSnoozeMinutes(it) },
                    onOngoingNotificationChange = { vm.setOngoingNotification(it) },
                    onLargeTextChange = { vm.setElderLargeText(it); onLargeTextChange(selectedRole == UserRole.ELDER && it) },
                    onOpenReminderSetup = { nav.navigate(Dest.Setup.route) },
                    onOpenFamilyContact = { nav.navigate(Dest.FamilyContact.route) },
                    onSwitchRole = { selectedRole = null; scanResult = null },
                    onBack = { nav.popBackStack() }
                )
            }

            composable(Dest.FamilyContact.route) {
                if (selectedRole != UserRole.FAMILY) {
                    LaunchedEffect(Unit) { nav.popBackStack() }
                    return@composable
                }
                FamilyContactScreen(
                    initial = appData.familyContact,
                    smsPermissionGranted = smsPermissionGranted,
                    onRequestSmsPermission = { smsPermissionLauncher.launch(Manifest.permission.SEND_SMS) },
                    onSave = { contact ->
                        vm.setFamilyContact(contact)
                        nav.popBackStack()
                    },
                    onBack = { nav.popBackStack() }
                )
            }

        }
    }

    riskCandidate?.let { med ->
        MedicationRiskReviewDialog(
            medicationName = med.name,
            findings = riskFindings,
            onConfirm = {
                vm.saveMedication(med)
                riskCandidate = null
                scanResult = null
                nav.popBackStack()
            },
            onCancel = { riskCandidate = null }
        )
    }
}

@Composable
private fun PermissionBanner(
    notifGranted: Boolean,
    exactGranted: Boolean,
    onRequestNotif: () -> Unit,
    onOpenNotifSettings: () -> Unit,
    onOpenExactSettings: () -> Unit
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.NotificationsActive, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("提醒还没完全打开", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                buildString {
                    if (!notifGranted) append("需要允许通知，才能在该吃药时提醒你。")
                    if (!exactGranted) {
                        if (!notifGranted) append("\n")
                        append("还需要允许「精确闹钟」，否则提醒时间可能延迟几分钟到几十分钟。")
                    }
                },
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!notifGranted) {
                    TextButton(onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) onRequestNotif()
                        else onOpenNotifSettings()
                    }) { Text("开启通知") }
                }
                if (!exactGranted) {
                    TextButton(onClick = onOpenExactSettings) { Text("开启精确闹钟") }
                }
            }
        }
    }
}
