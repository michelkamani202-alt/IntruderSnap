package com.michel.intrudersnap.ui

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.michel.intrudersnap.R
import com.michel.intrudersnap.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Liste des applications installées (hors IntruderSnap et apps système sans
 * icône de lancement) avec un interrupteur pour choisir lesquelles sont
 * protégées par le code PIN.
 */
class ProtectedAppsActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_protected_apps)
        prefs = Prefs(this)

        val recycler = findViewById<RecyclerView>(R.id.recyclerApps)
        recycler.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) { loadInstalledApps() }
            val adapter = ProtectedAppsAdapter(items.toMutableList()) { item, isChecked ->
                prefs.setAppProtected(item.packageName, isChecked)
            }
            recycler.adapter = adapter
        }
    }

    private fun loadInstalledApps(): List<InstalledAppItem> {
        val pm = packageManager
        val launchables = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val protectedSet = prefs.protectedPackages

        return launchables
            .filter { app ->
                app.packageName != packageName &&
                    pm.getLaunchIntentForPackage(app.packageName) != null
            }
            .map { app ->
                InstalledAppItem(
                    packageName = app.packageName,
                    label = pm.getApplicationLabel(app).toString(),
                    icon = try { pm.getApplicationIcon(app.packageName) } catch (e: Exception) { null },
                    protected = app.packageName in protectedSet
                )
            }
            .sortedBy { it.label.lowercase() }
    }
}
