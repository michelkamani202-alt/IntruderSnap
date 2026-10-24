package com.michel.intrudersnap.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.michel.intrudersnap.R
import com.michel.intrudersnap.data.LogEntry
import com.michel.intrudersnap.data.LogResult

class LogAdapter(private val items: List<LogEntry>) : RecyclerView.Adapter<LogAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val app: TextView = view.findViewById(R.id.textLogApp)
        val status: TextView = view.findViewById(R.id.textLogStatus)
        val time: TextView = view.findViewById(R.id.textLogTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log_entry, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = items[position]
        holder.app.text = entry.appLabel
        holder.time.text = entry.formattedTime()
        holder.status.text = when (entry.result) {
            LogResult.SUCCESS -> holder.itemView.context.getString(R.string.log_success)
            LogResult.FAILURE -> holder.itemView.context.getString(R.string.log_failure)
            LogResult.CAPTURE_SENT -> holder.itemView.context.getString(R.string.log_capture_sent)
            LogResult.CAPTURE_FAILED -> holder.itemView.context.getString(R.string.log_capture_failed)
        }
    }

    override fun getItemCount(): Int = items.size
}
