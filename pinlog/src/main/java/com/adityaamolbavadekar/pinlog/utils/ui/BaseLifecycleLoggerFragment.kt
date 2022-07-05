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

@file:Suppress("MemberVisibilityCanBePrivate")

package com.adityaamolbavadekar.pinlog.utils.ui

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.adityaamolbavadekar.pinlog.PinLog
import com.adityaamolbavadekar.pinlog.utils.ui.LifecycleConstants.ON_ATTACH
import com.adityaamolbavadekar.pinlog.utils.ui.LifecycleConstants.ON_CREATE
import com.adityaamolbavadekar.pinlog.utils.ui.LifecycleConstants.ON_DESTROY
import com.adityaamolbavadekar.pinlog.utils.ui.LifecycleConstants.ON_PAUSE
import com.adityaamolbavadekar.pinlog.utils.ui.LifecycleConstants.ON_RESUME
import com.adityaamolbavadekar.pinlog.utils.ui.LifecycleConstants.ON_START
import com.adityaamolbavadekar.pinlog.utils.ui.LifecycleConstants.ON_STOP

/**
 * This is a util [Fragment] class that logs the lifecycle
 * of the fragment. Eg: onStart() onStop() etc.
 * You can access the value of [mTag] and also use the [logMethodInvoked]
 *  and [LifecycleConstants] class.
 * You can inherit your fragment from this class and the [mTag] get
 * the name of your fragment file.
 *
 * @see LifecycleConstants
 * @see BaseLifecycleLoggerActivity
 *
 * */
open class BaseLifecycleLoggerFragment : Fragment() {

    val mTag: String = javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logMethodInvoked(ON_CREATE)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        logMethodInvoked(ON_ATTACH)
    }

    override fun onStart() {
        super.onStart()
        logMethodInvoked(ON_START)
    }

    override fun onResume() {
        super.onResume()
        logMethodInvoked(ON_RESUME)
    }

    override fun onPause() {
        super.onPause()
        logMethodInvoked(ON_PAUSE)
    }

    override fun onStop() {
        super.onStop()
        logMethodInvoked(ON_STOP)
    }

    override fun onDestroy() {
        super.onDestroy()
        logMethodInvoked(ON_DESTROY)
    }

    /**
     * Logs the [methodName] using [PinLog.logD].
     * */
    fun logMethodInvoked(methodName: String) {
        PinLog.logD(mTag, methodName)
    }

}