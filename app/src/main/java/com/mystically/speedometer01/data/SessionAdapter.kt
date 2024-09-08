package com.mystically.speedometer01.data

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import com.mystically.speedometer01.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class SessionAdapter(
    private val context: Context,
    private var items: List<Any>,
    private val onDelete: (SessionData) -> Unit
) : BaseAdapter() {

    companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_SESSION = 1
    }

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Any = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getViewTypeCount(): Int = 2  // to do: Change this to something less stupid probably

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is String) {
            VIEW_TYPE_HEADER
        } else {
            VIEW_TYPE_SESSION
        }
    }

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val viewType = getItemViewType(position)

        return if (viewType == VIEW_TYPE_HEADER) {
            val headerView = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.list_item_header, parent, false)
            val headerDateTextView: TextView = headerView.findViewById(R.id.headerDateText)
            headerDateTextView.text = items[position] as String
            headerView
        } else {
            val sessionView = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.list_item_session, parent, false)
            val session = items[position] as SessionData

            val startTimeText: TextView = sessionView.findViewById(R.id.startTimeText)
            startTimeText.text = "Start: ${formatDateAsTime(session.startTime)}"

            val stopTimeText: TextView = sessionView.findViewById(R.id.stopTimeText)
            stopTimeText.text = "End: ${formatDateAsTime(session.stopTime)}"

            val tripText: TextView = sessionView.findViewById(R.id.tripText)
            tripText.text = "Distance: ${String.format(Locale.getDefault(), "%.2f", session.trip)} km"

            val maxSpeedText: TextView = sessionView.findViewById(R.id.maxSpeedText)
            maxSpeedText.text = "Max: ${String.format(Locale.getDefault(), "%.0f", session.maxSpeed)} km/h | Avg : ${String.format(Locale.getDefault(), "%.0f", session.avgSpeed)} km/h"

            val totalTimeText: TextView = sessionView.findViewById(R.id.totalTimeText)
            totalTimeText.text = "Duration: ${session.totalTime}"

            val overflowMenu: ImageView = sessionView.findViewById(R.id.overflowMenu)
            overflowMenu.setOnClickListener { view ->
                showOverflow(view, session)
            }

            sessionView
        }
    }

    @SuppressLint("DiscouragedPrivateApi")
    private fun showOverflow(view: View, session: SessionData) {
        val popup = PopupMenu(context, view)
        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.session_overflow_menu, popup.menu)

        try {
            val field = PopupMenu::class.java.getDeclaredField("mPopup")
            field.isAccessible = true
            val menuPopupHelper = field.get(popup)
            val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
            val setForceIcons = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.java)
            setForceIcons.invoke(menuPopupHelper, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_delete -> {
                    onDelete(session)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    fun updateItems(updatedItems: List<Any>) {
        this.items = updatedItems
        notifyDataSetChanged()
    }

    private fun formatDateAsTime(dateString: String): String {
        val dateTime = LocalDateTime.parse(dateString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        return dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    }
}



