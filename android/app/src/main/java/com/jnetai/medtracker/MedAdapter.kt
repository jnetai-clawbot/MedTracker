package com.jnetai.medtracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jnetai.medtracker.data.Medication

class MedAdapter(
    private val meds: List<Medication>,
    private val activity: MainActivity
) : RecyclerView.Adapter<MedAdapter.MedViewHolder>() {

    private val gson = com.google.gson.Gson()

    class MedViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.med_name)
        val dosageText: TextView = view.findViewById(R.id.med_dosage)
        val pillsText: TextView = view.findViewById(R.id.med_pills)
        val timesContainer: LinearLayout = view.findViewById(R.id.med_times)
        val editBtn: Button = view.findViewById(R.id.btn_edit)
        val refillBtn: Button = view.findViewById(R.id.btn_refill)
        val pillWarning: TextView = view.findViewById(R.id.pill_warning)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medication, parent, false)
        return MedViewHolder(view)
    }

    override fun onBindViewHolder(holder: MedViewHolder, position: Int) {
        val med = meds[position]
        holder.nameText.text = med.name
        holder.dosageText.text = "${med.dosage} • ${med.frequencyPerDay}x/day"
        holder.pillsText.text = "Remaining: ${med.remainingPills} pills"

        // Pill warning
        if (med.remainingPills in 1..med.refillThreshold) {
            holder.pillWarning.visibility = View.VISIBLE
            holder.pillWarning.text = "⚠️ Low supply! ${med.remainingPills} pills remaining"
        } else if (med.remainingPills <= 0) {
            holder.pillWarning.visibility = View.VISIBLE
            holder.pillWarning.text = "🔴 Out of pills! Refill needed!"
        } else {
            holder.pillWarning.visibility = View.GONE
        }

        // Show times with taken/skipped buttons
        holder.timesContainer.removeAllViews()
        val times: List<String> = try {
            val type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
            gson.fromJson(med.reminderTimes, type)
        } catch (_: Exception) { listOf("08:00") }

        for (time in times) {
            val row = LinearLayout(holder.itemView.context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 4, 0, 4)
            }
            val timeLabel = TextView(holder.itemView.context).apply {
                text = "🕐 $time"
                setPadding(0, 0, 16, 0)
                textSize = 14f
            }
            val btnTaken = Button(holder.itemView.context).apply {
                text = "✅ Taken"
                setOnClickListener { activity.markDose(med, time, com.jnetai.medtracker.data.DoseStatus.TAKEN) }
            }
            val btnSkipped = Button(holder.itemView.context).apply {
                text = "⏭️ Skip"
                setOnClickListener { activity.markDose(med, time, com.jnetai.medtracker.data.DoseStatus.SKIPPED) }
            }
            row.addView(timeLabel)
            row.addView(btnTaken)
            row.addView(btnSkipped)
            holder.timesContainer.addView(row)
        }

        holder.editBtn.setOnClickListener { activity.showEditMedDialog(med) }
        holder.refillBtn.setOnClickListener { activity.showRefillDialog(med) }
    }

    override fun getItemCount() = meds.size
}