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

import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adityaamolbavadekar.pinlog.PinLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PinLogDebugActivity : AppCompatActivity(), SearchView.OnQueryTextListener {

    private lateinit var logsAdapter: LogsAdapter
    private lateinit var logsRecyclerView: RecyclerView
    private val isLoading: MutableLiveData<Boolean> = MutableLiveData(true)
    private val isEmpty: MutableLiveData<Boolean> = MutableLiveData(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pinlog_debug_logs)
        logsAdapter = LogsAdapter()
        logsRecyclerView = findViewById(R.id.logsRecyclerView)
        logsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@PinLogDebugActivity)
            adapter = logsAdapter
        }
        isLoading.observe(this) { loading ->
            findViewById<RelativeLayout>(R.id.loadingLayout).apply {
                if (loading) {
                    this.visibility = View.VISIBLE
                    logsRecyclerView.visibility = View.GONE
                } else {
                    logsRecyclerView.visibility = View.VISIBLE
                    this.visibility = View.GONE
                }
            }
        }
        isEmpty.observe(this) {empty->
            findViewById<LinearLayout>(R.id.emptyLayout).apply {
                if (empty) {
                    this.visibility = View.VISIBLE
                    logsRecyclerView.visibility = View.GONE
                } else {
                    this.visibility = View.GONE
                    logsRecyclerView.visibility = View.VISIBLE
                }
            }
        }
        supportActionBar?.title =
            "PinLog : " + application.applicationInfo.loadLabel(application.packageManager)
        Toast.makeText(
            applicationContext,
            "${PinLog.getPinLogsCount()} logs found.",
            Toast.LENGTH_SHORT
        ).show()
        loadLogs()
    }

    private fun loadLogs() = CoroutineScope(Dispatchers.IO).launch {
        isLoading.postValue(true)
        PinLog.getAllPinLogs().also { list ->
            if (list.isEmpty()) isEmpty.postValue(true)
            else isEmpty.postValue(false)
            logsAdapter.addAll(list)
        }
        CoroutineScope(Dispatchers.Main).launch {
            logsAdapter.notifyDataSetChanged()
        }
        delay(700)//For better experience
        isLoading.postValue(false)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_logs_activity, menu)
        return true
    }

    private var isSorted = false

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.sortLogs -> {

                if (isSorted) {
                    item.setIcon(R.drawable.ic_filter)
                    item.title = "Sort by Log Level"
                    logsAdapter.sortById()
                    isSorted = false
                } else {
                    item.setIcon(R.drawable.ic_cancel)
                    item.title = "Cancel sorting"
                    logsAdapter.sortByLogLevel()
                    isSorted = true
                }

                true
            }
            R.id.searchLogs -> {
                val s = (item.actionView as SearchView)
                s.setOnQueryTextListener(this)
                s.setOnCloseListener {
                    logsAdapter.clearSuggestions()
                    true
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        newText?.let {
            if (it.trim().isNotEmpty()) {
                logsAdapter.search(it, this)
            } else logsAdapter.clearSuggestions()
        }
        return false
    }

}