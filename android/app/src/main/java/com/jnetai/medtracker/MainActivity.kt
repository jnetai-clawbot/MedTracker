package com.jnetai.medtracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.jnetai.medtracker.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val db by lazy { MedDatabase.getInstance(this) }
    private val gson = Gson()
    private var currentTab = R.id.tab_meds
    private var selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            currentTab = item.itemId
            updateView()
            true
        }

        val fab = findViewById<FloatingActionButton>(R.id.fab_add)
        fab.setOnClickListener { showAddMedDialog() }

        updateView()
    }

    private fun updateView() {
        val content = findViewById<FrameLayout>(R.id.content_frame)
        content.removeAllViews()

        when (currentTab) {
            R.id.tab_meds -> showMedsView(content)
            R.id.tab_calendar -> showCalendarView(content)
            R.id.tab_history -> showHistoryView(content)
            R.id.tab_about -> showAboutView(content)
        }

        val fab = findViewById<FloatingActionButton>(R.id.fab_add)
        fab.visibility = if (currentTab == R.id.tab_meds || currentTab == R.id.tab_calendar) View.VISIBLE else View.GONE
    }

    // ===================== MEDS TAB =====================
    private fun showMedsView(content: FrameLayout) {
        val view = layoutInflater.inflate(R.layout.fragment_meds, content, false)
        content.addView(view)

        val recyclerView = view.findViewById<RecyclerView>(R.id.meds_recycler)
        val emptyView = view.findViewById<TextView>(R.id.empty_meds)
        recyclerView.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            db.medDao().getAllMedications().collectLatest { meds ->
                recyclerView.adapter = MedAdapter(meds, this@MainActivity)
                emptyView.visibility = if (meds.isEmpty()) View.VISIBLE else View.GONE
                recyclerView.visibility = if (meds.isEmpty()) View.GONE else View.VISIBLE

                // Check interactions
                if (meds.size >= 2) {
                    val interactions = DrugInteractions.checkInteractions(meds.map { it.name })
                    if (interactions.isNotEmpty()) {
                        val warnView = view.findViewById<LinearLayout>(R.id.interaction_warnings)
                        warnView.removeAllViews()
                        warnView.visibility = View.VISIBLE
                        for (interaction in interactions) {
                            val tv = TextView(this@MainActivity).apply {
                                text = "${DrugInteractions.getSeverityEmoji(interaction.severity)} ${interaction.drug1} + ${interaction.drug2}: ${interaction.description}"
                                setPadding(0, 8, 0, 8)
                                textSize = 13f
                            }
                            warnView.addView(tv)
                        }
                    }
                }

                // Check refill alerts
                for (med in meds) {
                    if (med.remainingPills > 0 && med.remainingPills <= med.refillThreshold) {
                        Toast.makeText(this@MainActivity, "⚠️ ${med.name}: Only ${med.remainingPills} pills left!", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun showAddMedDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_med, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Add Medication")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ -> }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.input_med_name)
        val dosageInput = dialogView.findViewById<TextInputEditText>(R.id.input_dosage)
        val freqInput = dialogView.findViewById<Spinner>(R.id.spinner_frequency)
        val timesContainer = dialogView.findViewById<LinearLayout>(R.id.times_container)
        val pillsInput = dialogView.findViewById<TextInputEditText>(R.id.input_pill_count)
        val refillInput = dialogView.findViewById<TextInputEditText>(R.id.input_refill_threshold)

        val timePickers = mutableListOf<TextInputEditText>()

        freqInput.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                val count = position + 1
                timesContainer.removeAllViews()
                timePickers.clear()
                for (i in 0 until count) {
                    val timeInput = TextInputEditText(this@MainActivity).apply {
                        hint = "Time ${i + 1} (HH:mm)"
                        setText(if (i == 0) "08:00" else if (i == 1) "20:00" else "12:00")
                        setPadding(0, 8, 0, 8)
                    }
                    timesContainer.addView(timeInput)
                    timePickers.add(timeInput)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        // Trigger initial
        freqInput.setSelection(0)

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = nameInput.text.toString().trim()
            val dosage = dosageInput.text.toString().trim()
            if (name.isBlank() || dosage.isBlank()) {
                Toast.makeText(this, "Name and dosage are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val freq = freqInput.selectedItemPosition + 1
            val times = timePickers.map { it.text.toString().trim() }
                .filter { it.matches(Regex("\\d{1,2}:\\d{2}")) }
            if (times.size < freq) {
                Toast.makeText(this, "Please fill in all times (HH:mm format)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val pills = pillsInput.text.toString().toIntOrNull() ?: 30
            val refill = refillInput.text.toString().toIntOrNull() ?: 5
            val timesJson = gson.toJson(times)

            lifecycleScope.launch(Dispatchers.IO) {
                val med = Medication(
                    name = name,
                    dosage = dosage,
                    frequencyPerDay = freq,
                    reminderTimes = timesJson,
                    remainingPills = pills,
                    refillThreshold = refill
                )
                val id = db.medDao().insertMedication(med)
                val savedMed = med.copy(id = id)
                ReminderScheduler.scheduleReminders(this@MainActivity, savedMed)

                withContext(Dispatchers.Main) {
                    dialog.dismiss()
                }
            }
        }
    }

    fun showEditMedDialog(med: Medication) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_med, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Edit Medication")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ -> }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Delete") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    ReminderScheduler.cancelReminders(this@MainActivity, med)
                    db.medDao().deleteDoseHistoryForMedication(med.id)
                    db.medDao().deleteMedication(med)
                }
            }
            .create()

        dialog.show()

        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.input_med_name)
        val dosageInput = dialogView.findViewById<TextInputEditText>(R.id.input_dosage)
        val freqInput = dialogView.findViewById<Spinner>(R.id.spinner_frequency)
        val timesContainer = dialogView.findViewById<LinearLayout>(R.id.times_container)
        val pillsInput = dialogView.findViewById<TextInputEditText>(R.id.input_pill_count)
        val refillInput = dialogView.findViewById<TextInputEditText>(R.id.input_refill_threshold)

        val times: List<String> = try {
            val type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
            gson.fromJson(med.reminderTimes, type)
        } catch (_: Exception) { listOf("08:00") }

        nameInput.setText(med.name)
        dosageInput.setText(med.dosage)
        pillsInput.setText(med.remainingPills.toString())
        refillInput.setText(med.refillThreshold.toString())

        val timePickers = mutableListOf<TextInputEditText>()

        freqInput.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                val count = position + 1
                timesContainer.removeAllViews()
                timePickers.clear()
                for (i in 0 until count) {
                    val timeInput = TextInputEditText(this@MainActivity).apply {
                        hint = "Time ${i + 1} (HH:mm)"
                        setText(times.getOrElse(i) { if (i == 0) "08:00" else "20:00" })
                        setPadding(0, 8, 0, 8)
                    }
                    timesContainer.addView(timeInput)
                    timePickers.add(timeInput)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        freqInput.setSelection(med.frequencyPerDay - 1)

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val name = nameInput.text.toString().trim()
            val dosage = dosageInput.text.toString().trim()
            if (name.isBlank() || dosage.isBlank()) {
                Toast.makeText(this, "Name and dosage are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val freq = freqInput.selectedItemPosition + 1
            val updatedTimes = timePickers.map { it.text.toString().trim() }
                .filter { it.matches(Regex("\\d{1,2}:\\d{2}")) }
            if (updatedTimes.size < freq) {
                Toast.makeText(this, "Please fill in all times", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val pills = pillsInput.text.toString().toIntOrNull() ?: med.remainingPills
            val refill = refillInput.text.toString().toIntOrNull() ?: med.refillThreshold
            val timesJson = gson.toJson(updatedTimes)

            lifecycleScope.launch(Dispatchers.IO) {
                val updated = med.copy(
                    name = name,
                    dosage = dosage,
                    frequencyPerDay = freq,
                    reminderTimes = timesJson,
                    remainingPills = pills,
                    refillThreshold = refill
                )
                ReminderScheduler.cancelReminders(this@MainActivity, med)
                db.medDao().updateMedication(updated)
                ReminderScheduler.scheduleReminders(this@MainActivity, updated)

                withContext(Dispatchers.Main) {
                    dialog.dismiss()
                }
            }
        }
    }

    fun showRefillDialog(med: Medication) {
        val input = TextInputEditText(this).apply {
            hint = "Number of pills to add"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Refill: ${med.name}")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val qty = input.text.toString().toIntOrNull() ?: 0
                if (qty > 0) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        db.medDao().updatePillCount(med.id, med.remainingPills + qty)
                        db.medDao().insertRefill(RefillHistory(
                            medicationId = med.id,
                            medicationName = med.name,
                            quantityAdded = qty
                        ))
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun markDose(med: Medication, time: String, status: DoseStatus) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        lifecycleScope.launch(Dispatchers.IO) {
            val existing = db.medDao().getDose(med.id, today, time)
            if (existing != null) {
                db.medDao().updateDoseHistory(existing.copy(
                    status = status,
                    takenAt = if (status == DoseStatus.TAKEN) System.currentTimeMillis() else null,
                    updatedAt = System.currentTimeMillis()
                ))
            } else {
                db.medDao().insertDoseHistory(DoseHistory(
                    medicationId = med.id,
                    medicationName = med.name,
                    scheduledTime = time,
                    scheduledDate = today,
                    takenAt = if (status == DoseStatus.TAKEN) System.currentTimeMillis() else null,
                    status = status
                ))
            }
            if (status == DoseStatus.TAKEN && med.remainingPills > 0) {
                db.medDao().updatePillCount(med.id, med.remainingPills - 1)
            }
        }
    }

    // ===================== CALENDAR TAB =====================
    private fun showCalendarView(content: FrameLayout) {
        val view = layoutInflater.inflate(R.layout.fragment_calendar, content, false)
        content.addView(view)

        val calendarView = view.findViewById<CalendarView>(R.id.calendar_view)
        val scheduleList = view.findViewById<LinearLayout>(R.id.schedule_list)

        loadScheduleForDate(selectedDate, scheduleList)

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
            selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
            loadScheduleForDate(selectedDate, scheduleList)
        }
    }

    private fun loadScheduleForDate(date: String, container: LinearLayout) {
        container.removeAllViews()
        lifecycleScope.launch {
            db.medDao().getDosesForDate(date).collectLatest { doses ->
                container.removeAllViews()
                if (doses.isEmpty()) {
                    container.addView(TextView(this@MainActivity).apply {
                        text = "No doses scheduled for $date"
                        setPadding(16, 16, 16, 16)
                    })
                } else {
                    for (dose in doses) {
                        val statusIcon = when (dose.status) {
                            DoseStatus.TAKEN -> "✅"
                            DoseStatus.SKIPPED -> "⏭️"
                            DoseStatus.MISSED -> "❌"
                        }
                        container.addView(TextView(this@MainActivity).apply {
                            text = "$statusIcon ${dose.scheduledTime} — ${dose.medicationName} (${dose.status.name})"
                            setPadding(8, 8, 8, 8)
                            textSize = 14f
                        })
                    }
                }
            }
        }
    }

    // ===================== HISTORY TAB =====================
    private fun showHistoryView(content: FrameLayout) {
        val view = layoutInflater.inflate(R.layout.fragment_history, content, false)
        content.addView(view)

        val recyclerView = view.findViewById<RecyclerView>(R.id.history_recycler)
        val emptyView = view.findViewById<TextView>(R.id.empty_history)
        val exportBtn = view.findViewById<Button>(R.id.btn_export)
        recyclerView.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            db.medDao().getAllDoseHistory().collectLatest { doses ->
                recyclerView.adapter = HistoryAdapter(doses)
                emptyView.visibility = if (doses.isEmpty()) View.VISIBLE else View.GONE
                recyclerView.visibility = if (doses.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        exportBtn.setOnClickListener { exportHistory() }
    }

    private fun exportHistory() {
        lifecycleScope.launch {
            val doseList = db.medDao().getAllDoseHistory().first()

            val json = gson.toJson(doseList)
            val file = File(getExternalFilesDir(null), "medtracker_history_${System.currentTimeMillis()}.json")
            file.writeText(json)

            val uri = FileProvider.getUriForFile(
                this@MainActivity,
                "${packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(shareIntent, "Export History"))
        }
    }

    // ===================== ABOUT TAB =====================
    @Suppress("DEPRECATION")
    private fun showAboutView(content: FrameLayout) {
        val view = layoutInflater.inflate(R.layout.fragment_about, content, false)
        content.addView(view)

        val versionText = view.findViewById<TextView>(R.id.version_text)
        val updateBtn = view.findViewById<Button>(R.id.btn_check_update)
        val shareBtn = view.findViewById<Button>(R.id.btn_share)
        val updateResult = view.findViewById<TextView>(R.id.update_result)

        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            versionText.text = "MedTracker v${pInfo.versionName}"
        } catch (_: Exception) {
            versionText.text = "MedTracker v1.0.0"
        }

        updateBtn.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val url = java.net.URL("https://api.github.com/repos/jnetai-clawbot/MedTracker/releases/latest")
                    val conn = url.openConnection() as java.net.HttpURLConnection
                    conn.requestMethod = "GET"
                    conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
                    val response = conn.inputStream.bufferedReader().readText()
                    conn.disconnect()
                    val json = com.google.gson.JsonParser.parseString(response).asJsonObject
                    val tagName = json.get("tag_name")?.asString ?: "unknown"
                    val htmlUrl = json.get("html_url")?.asString ?: ""
                    withContext(Dispatchers.Main) {
                        updateResult.text = "Latest release: $tagName\n$htmlUrl"
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        updateResult.text = "Failed to check for updates: ${e.message}"
                    }
                }
            }
        }

        shareBtn.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "Check out MedTracker - Medication Reminder App!\nhttps://github.com/jnetai-clawbot/MedTracker")
            }
            startActivity(Intent.createChooser(shareIntent, "Share MedTracker"))
        }
    }
}