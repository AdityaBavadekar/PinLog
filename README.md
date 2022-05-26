![Version](https://img.shields.io/github/v/release/adityabavadekar/PinLog?label=PinLog%20&style=plastic374547970654465636f72653f6c6162656c3d546f617374547970654465636f7265253230267374796c653d706c6173746963)

# What is PinLog?
PinLog is an easy-to-use and powerful android Logging Library. It is made by Aditya Bavadekar.
PinLog supports storing logs for later retrieval, saving logs in a file, saving logs in a zip file and more.

# Latest version
For the latest version and a complete changelog, please see the Release page.

# Implementation 

### Using Gradle : 
> Add `maven{  }` in your build.gradle(project)
```gradle
allprojects {
    repositories {
      //Add this `maven` block
      maven { url 'https://jitpack.io' }
    }
}
```
> Add the dependency
 ![TAG](https://jitpack.io/v/AdityaBavadekar/PinLog.svg)
```gradle
dependencies {
     // Refer the above badge for latest `TAG`.
    implementation 'com.github.AdityaBavadekar:PinLog:TAG'
}
```
### Using Maven : 
> Add `repository`
```xml
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
```
> Add the dependency
![TAG](https://jitpack.io/v/AdityaBavadekar/PinLog.svg)
```xml
	<dependency>
	    <groupId>com.github.AdityaBavadekar</groupId>
	    <artifactId>PinLog</artifactId>
	    <version>Tag</version>
	</dependency>
```

# How do I use PinLog?

### Initialisation
 - PinLog should be initialised in the Application Class :
```kotlin
class App : Application() {

    override fun onCreate() {
        super.onCreate()

         PinLog.initialise(this)
         PinLog.setDevLogging(true)//Optional
         PinLog.setBuildConfigClass(BuildConfig::class.java)//Optional
    }

}
```
*OR*
```kotlin
 //For Debuggable Builds
 PinLog.initialiseDebug(this@App)

 //For Release Builds
 PinLog.initialiseRelease(this@App)
```

### Usage
```kotlin

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        logI("onCreate")
        logW("This is a warning")
        logE("This is a error")
        logD("This is a debug log")
        //OR
        PinLog.logI("onCreate")
        PinLog.logW("This is a warning")
        PinLog.logE("This is a error")
        PinLog.logD("This is a debug log")

        //Get stored logs
        PinLog.getAllPinLogsAsStringList()
        //Delete stored logs
        PinLogs.deleteAllPinLogs()

    }

}
```

# Author
[@Aditya Bavadekar](https://github.com/AdityaBavadekar) on GitHub 

# Licence

```

   Copyright 2022 Aditya Bavadekar

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

```
