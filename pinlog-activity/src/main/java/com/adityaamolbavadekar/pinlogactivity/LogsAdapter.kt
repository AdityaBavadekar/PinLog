/*
 *     Copyright (c) 2022  Aditya Bavadekar
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package com.adityaamolbavadekar.pinlogactivity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.adityaamolbavadekar.pinlog.database.ApplicationLogModel

internal class LogsAdapter : RecyclerView.Adapter<LogsAdapter.LogHolder>() {

    private var itemsList = mutableListOf<ApplicationLogModel>()
    private var backupItemsList = mutableListOf<ApplicationLogModel>()

    class LogHolder private constructor(private val view: View) : RecyclerView.ViewHolder(view) {
        fun bind(logItem: ApplicationLogModel) {
            val textViewLogLevel = view.findViewById<TextView>(R.id.textView1)
            val textViewLog = view.findViewById<TextView>(R.id.textView2)
            textViewLogLevel.text = logItem.LOG_LEVEL.toText

            var isExpanded = false
            textViewLog.text = logItem.LOG

            view.setOnClickListener {

                if (textViewLog.lineCount > 20) {
                    when {
                        !isExpanded -> {
                            isExpanded = true
                            textViewLog.maxLines = Integer.MAX_VALUE
                        }
                        isExpanded -> {
                            isExpanded = false
                            textViewLog.maxLines = 30
                        }
                    }

                }
            }
            view.setOnLongClickListener { copyLog(logItem) }
        }

        private fun copyLog(logItem: ApplicationLogModel): Boolean {
            val c = view.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            c.setPrimaryClip(ClipData.newPlainText("log", logItem.LOG))
            Toast.makeText(view.context, view.context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
            return true
        }

        companion object {
            fun from(parent: ViewGroup): LogHolder {
                val context = parent.context
                val inflater = LayoutInflater.from(context)
                val layout = inflater.inflate(R.layout.log_item, parent, false)
                return LogHolder(layout)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogHolder =
        LogHolder.from(parent)

    override fun onBindViewHolder(holder: LogHolder, position: Int) =
        holder.bind(itemsList[position])

    override fun getItemCount(): Int = itemsList.size

    fun filterTag(tag: String, context: Context) {
        val toast = Toast.makeText(
            context,
            context.getString(R.string.no_results_found),
            Toast.LENGTH_SHORT
        )
        val results = mutableListOf<ApplicationLogModel>()
        backupItemsList.forEach {
            if (it.TAG.contains(tag, true)) {
                results.add(it)
            }
        }
        if (results.isEmpty()) {
            toast.show()
        } else {
            clearList()
            itemsList.addAll(results)
            notifyDataSetChanged()
        }
    }

    fun sortByLogLevel() {
        itemsList.sortBy { it.LOG_LEVEL }
        notifyDataSetChanged()
    }


    fun sortById() {
        clearList()
        itemsList.addAll(backupItemsList)
        itemsList.sortBy { it.id }
        notifyDataSetChanged()
    }

    fun search(s: String, context: Context) {
        val toast = Toast.makeText(
            context,
            context.getString(R.string.no_results_found),
            Toast.LENGTH_SHORT
        )
        val results = mutableListOf<ApplicationLogModel>()
        backupItemsList.forEach {
            if (it.LOG.contains(s, true) || it.LOG_LEVEL.toText.contains(s, true)) {
                results.add(it)
            }
        }
        if (results.isEmpty()) {
            toast.show()
        } else {
            clearList()
            itemsList.addAll(results)
            notifyDataSetChanged()
        }
    }

    fun clearSuggestions() {
        clearList()
        itemsList.addAll(backupItemsList)
        notifyDataSetChanged()
    }

    private fun clearList() {
        if (itemsList.size <= 0) return
        while (itemsList.size != 0) {
            itemsList.removeAt(itemsList.size - 1)
        }
        notifyDataSetChanged()
    }

    fun addAll(list: List<ApplicationLogModel>) {
        itemsList.addAll(list)
        backupItemsList.addAll(list)
    }

}