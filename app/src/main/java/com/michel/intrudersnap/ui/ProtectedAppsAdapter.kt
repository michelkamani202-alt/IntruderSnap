package com.michel.intrudersnap.ui

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.michel.intrudersnap.R

data class InstalledAppItem(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    var protected: Boolean
)

class ProtectedAppsAdapter(
    private val items: MutableList<InstalledAppItem>,
    private val onToggle: (InstalledAppItem, Boolean) -> Unit
) : RecyclerView.Adapter<ProtectedAppsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.imageAppIcon)
        val name: TextView = view.findViewById(R.id.textAppName)
        val toggle: SwitchMaterial = view.findViewById(R.id.switchProtected)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_protected_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.label
        holder.icon.setImageDrawable(item.icon)
        holder.toggle.setOnCheckedChangeListener(null)
        holder.toggle.isChecked = item.protected
        holder.toggle.setOnCheckedChangeListener { _, isChecked ->
            item.protected = isChecked
            onToggle(item, isChecked)
        }
    }

    override fun getItemCount(): Int = items.size
}
