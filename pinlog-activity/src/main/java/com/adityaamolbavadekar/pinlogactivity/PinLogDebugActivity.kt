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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.webkit.MimeTypeMap
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adityaamolbavadekar.pinlog.PinLog
import com.adityaamolbavadekar.pinlog.database.ApplicationLogModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PinLogDebugActivity : AppCompatActivity(), SearchView.OnQueryTextListener {

    private lateinit var logsAdapter: LogsAdapter
    private lateinit var logsRecyclerView: RecyclerView
    private var tagsList: MutableList<String> = mutableListOf()
    private var appName: String = "Unknown"
    private var selectionIndex: Int = 0
    private val isLoading: MutableLiveData<Boolean> = MutableLiveData(true)
    private val isEmpty: MutableLiveData<Boolean> = MutableLiveData(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pinlog_debug_logs)
        tagsList.add(getString(R.string.none))//Default value for TAGs filter.
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

        isEmpty.observe(this) { empty ->
            findViewById<ScrollView>(R.id.emptyLayout).apply {
                if (empty) {
                    this.visibility = View.VISIBLE
                    logsRecyclerView.visibility = View.GONE
                } else {
                    this.visibility = View.GONE
                    logsRecyclerView.visibility = View.VISIBLE
                }
            }
        }
        appName = application.applicationInfo.loadLabel(application.packageManager).toString()
        supportActionBar?.title = getString(R.string.pinlog_title, appName)
        showCount()
        loadLogs()
    }

    private fun showCount() {
        PinLog.getPinLogsCount().also {
            if (it > 0) Toast.makeText(
                applicationContext,
                getString(R.string.logs_found_formatted, it),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun loadLogs() = CoroutineScope(Dispatchers.IO).launch {
        isLoading.postValue(true)
        PinLog.getAllPinLogs().also { list ->
            if (list.isEmpty()) isEmpty.postValue(true)
            else {
                isEmpty.postValue(false)
                retrieveTags(list)
            }
            logsAdapter.addAll(list)
        }
        CoroutineScope(Dispatchers.Main).launch {
            logsAdapter.notifyDataSetChanged()
        }
        delay(700)//For better experience
        isLoading.postValue(false)
    }

    /*Retrieves the tags that are present in the logs.*/
    private fun retrieveTags(list: List<ApplicationLogModel>) =
        CoroutineScope(Dispatchers.IO).launch {
            list.forEach {
                val tag = it.TAG
                if (!tagsList.contains(tag)) {
                    tagsList.add(tag)
                }
            }
        }

    private fun showTagChooserDialog() {
        if (tagsList.size <= 1) {
            //Don't show the dialog if tagsList is empty.
            Toast.makeText(this, getString(R.string.no_tags_found_to_filter), Toast.LENGTH_SHORT)
                .show()
            return
        }
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.choose_a_tag_value))
        builder.setCancelable(true)
        builder.setSingleChoiceItems(tagsList.toTypedArray(), selectionIndex)
        { _, which -> selectionIndex = which }
        builder.setNeutralButton(getString(R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
        }
        builder.setPositiveButton(getString(R.string.apply)) { dialog, _ ->
            if (selectionIndex == 0) {
                //Index 0 indicates that `None` option has been selected.
                logsAdapter.sortById()
            } else {
                logsAdapter.filterTag(tagsList[selectionIndex], this)
            }
            dialog.dismiss()
        }
        builder.setNegativeButton(getString(R.string.clear_filter)) { dialog, _ ->
            selectionIndex = 0
            logsAdapter.sortById()
            dialog.dismiss()
        }
        builder.create()
        builder.show()
    }

    private fun showInfoDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.pinlog_library, BuildConfig.VERSION_NAME))
        builder.setCancelable(true)
        builder.setIcon(R.drawable.ic_logo)
        builder.setMessage(getString(R.string.pinlog_info, appName))
        builder.setNeutralButton(getString(R.string.okay)) { dialog, _ ->
            dialog.dismiss()
        }
        builder.setPositiveButton(getString(R.string.view_source)) { dialog, _ ->
            Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(getString(R.string.repo_url_do_not_translate))
                startActivity(this)
            }
            dialog.dismiss()
        }
        builder.create()
        builder.show()
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
                    item.title = getString(R.string.sort_by_log_level)
                    logsAdapter.sortById()
                    isSorted = false
                } else {
                    item.setIcon(R.drawable.ic_cancel)
                    item.title = getString(R.string.cancel_sorting)
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
            R.id.applyTagFilter -> {
                showTagChooserDialog()
                true
            }
            R.id.info -> {
                showInfoDialog()
                true
            }
            R.id.export -> {
                val f = PinLog.getAllPinLogsInFile() ?: return false
                PinLog.getUriForFile(f)?.let {
                    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(f.extension)
                    val i = getShareIntent(it, mimeType)
                    grantPermission(i, it)
                    startActivity(i)
                    true
                }
                false
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getShareIntent(it: Uri, mimeType: String?): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.application_logs))
            putExtra(Intent.EXTRA_TEXT, getString(R.string.hey_i_am_sharing_logs_with_you))
            putExtra(Intent.EXTRA_STREAM, it)
            type = mimeType
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        }
    }

    private fun grantPermission(i: Intent, attachmentUri: Uri) {
        try {
            for (resolveInfo in packageManager.queryIntentActivities(
                i, 0
            )) {
                grantUriPermission(
                    resolveInfo.resolvePackageName,
                    attachmentUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        } catch (e: Exception) {
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

    override fun onBackPressed() {
        super.onBackPressed()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask()
        } else finish()
    }

}

/*
// STOPSHIP: 7/5/2022
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

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.webkit.MimeTypeMap
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.adityaamolbavadekar.pinlog.PinLog
import com.adityaamolbavadekar.pinlog.database.ApplicationLogModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PinLogDebugActivity : AppCompatActivity(), SearchView.OnQueryTextListener {

    private lateinit var logsAdapter: LogsAdapter
    private lateinit var logsRecyclerView: RecyclerView
    private var tagsList: MutableList<String> = mutableListOf()
    private var appName: String = "Unknown"
    private val isLoading: MutableLiveData<Boolean> = MutableLiveData(true)
    private val isEmpty: MutableLiveData<Boolean> = MutableLiveData(false)

    private lateinit var viewModel : DebugActivityViewModel

    class DebugActivityViewModel:ViewModel(){

        private val _isLoading: MutableLiveData<Boolean> = MutableLiveData(true)
        private val _isEmpty: MutableLiveData<Boolean> = MutableLiveData(false)
        private val _list: MutableLiveData<List<ApplicationLogModel>> = MutableLiveData(listOf())
        private val _tagsList: MutableLiveData<List<String>> = MutableLiveData(listOf())

        val isLoading : LiveData<Boolean> = _isLoading
        val isEmpty : LiveData<Boolean> = _isEmpty
        val list : LiveData<List<ApplicationLogModel>> = _list
        val tags : LiveData<List<String>> = _tagsList

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pinlog_debug_logs)
        viewModel = defaultViewModelProviderFactory.create(DebugActivityViewModel::class.java)
        tagsList.add(getString(R.string.none))//Default value for TAGs filter.
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

        isEmpty.observe(this) { empty ->
            findViewById<ScrollView>(R.id.emptyLayout).apply {
                if (empty) {
                    this.visibility = View.VISIBLE
                    logsRecyclerView.visibility = View.GONE
                } else {
                    this.visibility = View.GONE
                    logsRecyclerView.visibility = View.VISIBLE
                }
            }
        }
        appName = application.applicationInfo.loadLabel(application.packageManager).toString()
        supportActionBar?.title = getString(R.string.pinlog_title, appName)
        showCount()
        loadLogs()
    }

    private fun showCount() {
        PinLog.getPinLogsCount().also {
            if (it > 0) Toast.makeText(
                applicationContext,
                getString(R.string.logs_found_formatted, it),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun loadLogs() = CoroutineScope(Dispatchers.IO).launch {
        isLoading.postValue(true)
        PinLog.getAllPinLogs().also { list ->
            if (list.isEmpty()) isEmpty.postValue(true)
            else {
                isEmpty.postValue(false)
                retrieveTags(list)
            }
            logsAdapter.addAll(list)
        }
        CoroutineScope(Dispatchers.Main).launch {
            logsAdapter.notifyDataSetChanged()
        }
        delay(700)//For better experience
        isLoading.postValue(false)
    }

    /*Retrieves the tags that are present in the logs.*/
    private fun retrieveTags(list: List<ApplicationLogModel>) =
        CoroutineScope(Dispatchers.IO).launch {
            list.forEach {
                val tag = it.TAG
                if (!tagsList.contains(tag)) {
                    tagsList.add(tag)
                }
            }
        }

    private fun showTagChooserDialog() {
        var selection = 0
        if (tagsList.size <= 1) {
            //Don't show the dialog if tagsList is empty.
            Toast.makeText(this, getString(R.string.no_tags_found_to_filter), Toast.LENGTH_SHORT)
                .show()
            return
        }
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.choose_a_tag_value))
        builder.setCancelable(true)
        builder.setSingleChoiceItems(tagsList.toTypedArray(), selection)
        { _, which -> selection = which }
        builder.setNeutralButton(getString(R.string.cancel)) { dialog, _ ->
            dialog.dismiss()
        }
        builder.setPositiveButton(getString(R.string.apply)) { dialog, _ ->
            if (selection == 0) {
                //Index 0 indicates that `None` option has been selected.
                logsAdapter.sortById()
            } else {
                logsAdapter.filterTag(tagsList[selection], this)
            }
            dialog.dismiss()
        }
        builder.setNegativeButton(getString(R.string.clear_filter)) { dialog, _ ->
            logsAdapter.sortById()
            dialog.dismiss()
        }
        builder.create()
        builder.show()
    }

    private fun showInfoDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.pinlog_library, BuildConfig.VERSION_NAME))
        builder.setCancelable(true)
        builder.setIcon(R.drawable.ic_logo)
        builder.setMessage(getString(R.string.pinlog_info, appName))
        builder.setNeutralButton(getString(R.string.okay)) { dialog, _ ->
            dialog.dismiss()
        }
        builder.setPositiveButton(getString(R.string.view_source)) { dialog, _ ->
            startActivity(Intent().setData(Uri.parse(getString(R.string.repo_url_do_not_translate))))
            dialog.dismiss()
        }
        builder.create()
        builder.show()
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
                    item.title = getString(R.string.sort_by_log_level)
                    logsAdapter.sortById()
                    isSorted = false
                } else {
                    item.setIcon(R.drawable.ic_cancel)
                    item.title = getString(R.string.cancel_sorting)
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
            R.id.applyTagFilter -> {
                showTagChooserDialog()
                true
            }
            R.id.info -> {
                showInfoDialog()
                true
            }
            R.id.export -> {
                val f = PinLog.getAllPinLogsInFile() ?: return false
                PinLog.getUriForFile(f)?.let {
                    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(f.extension)
                    val i = getShareIntent(it, mimeType)
                    grantPermission(i, it)
                    startActivity(i)
                    true
                }
                false
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getShareIntent(it: Uri, mimeType: String?): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.application_logs))
            putExtra(Intent.EXTRA_TEXT, getString(R.string.hey_i_am_sharing_logs_with_you))
            putExtra(Intent.EXTRA_STREAM, it)
            type = mimeType
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        }
    }

    private fun grantPermission(i: Intent, attachmentUri: Uri) {
        try {
            for (resolveInfo in packageManager.queryIntentActivities(
                i, 0
            )) {
                grantUriPermission(
                    resolveInfo.resolvePackageName,
                    attachmentUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        } catch (e: Exception) {
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

    override fun onBackPressed() {
        super.onBackPressed()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            finishAndRemoveTask()
        } else finish()
    }

}


*/