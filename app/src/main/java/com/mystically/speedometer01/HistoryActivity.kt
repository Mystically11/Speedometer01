package com.mystically.speedometer01

import android.os.Bundle
import android.view.MenuItem
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import com.mystically.speedometer01.data.AppDatabase
import com.mystically.speedometer01.data.SessionAdapter
import com.mystically.speedometer01.data.SessionData
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryActivity : BaseActivity() {

    private lateinit var adapter: SessionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        val toolbar: Toolbar = findViewById(R.id.bluds_toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)
        supportActionBar?.title = "Session History"

        val historyListView = findViewById<ListView>(R.id.historyListView)

        val items = mutableListOf<Any>()

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val sessionHistory: List<SessionData> = db.sessionDataDao().getAllSessions()

            var currentDate = ""
            for (session in sessionHistory) {
                val formattedDate = formatDateString(session.startTime.substring(0, 10))
                if (formattedDate != currentDate) {
                    items.add(formattedDate)
                    currentDate = formattedDate
                }
                items.add(session)
            }

            adapter = SessionAdapter(this@HistoryActivity, items) { session ->
                deleteSession(session)
            }
            historyListView.adapter = adapter
            historyListView.setEmptyView(findViewById(R.id.historyIsEmpty))
        }
    }

    private fun deleteSession(session: SessionData) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            db.sessionDataDao().deleteSession(session)

            refreshSessionList(session)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun refreshSessionList(dSession: SessionData) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val sessionHistory: List<SessionData> = db.sessionDataDao().getAllSessions()

            val items = mutableListOf<Any>()
            var currentDate = ""

            for (session in sessionHistory) {
                val formattedDate = formatDateString(session.startTime.substring(0, 10))

                if (formattedDate != currentDate) {
                    val sessionsForDate = sessionHistory.filter {
                        it.startTime.startsWith(session.startTime.substring(0, 10))
                    }

                    if (sessionsForDate.isNotEmpty()) {
                        items.add(formattedDate)
                        currentDate = formattedDate
                    }
                }
                items.add(session)
            }
            adapter.updateItems(items)
        }
    }

    private fun formatDateString(dateString: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("d MMMM yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        return outputFormat.format(date!!)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}


