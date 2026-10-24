package com.michel.intrudersnap.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.michel.intrudersnap.R
import com.michel.intrudersnap.data.AttemptLogStore

/**
 * Journal consultable par l'utilisateur de toutes les tentatives
 * (réussies, échouées, captures envoyées) — aucune action d'IntruderSnap
 * n'est cachée à la personne qui possède l'appareil.
 */
class LogActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        val entries = AttemptLogStore(this).readAll()
        val recycler = findViewById<RecyclerView>(R.id.recyclerLog)
        val emptyView = findViewById<TextView>(R.id.textLogEmpty)

        if (entries.isEmpty()) {
            emptyView.visibility = TextView.VISIBLE
            recycler.visibility = RecyclerView.GONE
        } else {
            recycler.layoutManager = LinearLayoutManager(this)
            recycler.adapter = LogAdapter(entries)
        }
    }
}
