/*******************************************************************************
 *   Copyright (c) 2022  Aditya Bavadekar
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 ******************************************************************************/

package com.adityaamolbavadekar.pinlog.database

data class ApplicationLogModel(
    val id: Int,
    var LOG: String,
    var LOG_LEVEL: Int,
    var TAG: String = "",
    var created: Long = 0
) {

    @Deprecated(message = "new property TAG has been added")
    constructor(id: Int, LOG: String, LOG_LEVEL: Int) : this(id, LOG, LOG_LEVEL, "", 0)

}