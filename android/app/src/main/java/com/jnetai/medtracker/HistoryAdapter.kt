package com.jnetai.medtracker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jnetai.medtracker.data.DoseHistory
import com.jnetai.medtracker.data.DoseStatus

class HistoryAdapter(
    private val doses: List<DoseHistory>
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val medName: TextView = view.findViewById(R.id.hist_med_name)
        val timeDate: TextView = view.findViewById(R.id.hist_time_date)
        val statusText: TextView = view.findViewById(R.id.hist_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val dose = doses[position]
        holder.medName.text = dose.medicationName
        holder.timeDate.text = "${dose.scheduledDate} at ${dose.scheduledTime}"
        holder.statusText.text = when (dose.status) {
            DoseStatus.TAKEN -> "✅ Taken${dose.takenAt?.let { " at ${formatTime(it)}" } ?: ""}"
            DoseStatus.SKIPPED -> "⏭️ Skipped"
            DoseStatus.MISSED -> "❌ Missed"
        }
    }

    private fun formatTime(timestamp: Long): String {
        return java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).format(java.util.Date(timestamp))
    }

    override fun getItemCount() = doses.size
}