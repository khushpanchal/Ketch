[![](https://jitpack.io/v/khushpanchal/Ketch.svg)](https://jitpack.io/#khushpanchal/Ketch)
[![](https://androidweekly.net/issues/issue-622/badge)](https://androidweekly.net/issues/issue-622)

# Ketch

## An Android File downloader library based on WorkManager

<p align="center">
  <img width="950" src="https://raw.githubusercontent.com/khushpanchal/Ketch/master/assets/Ketch_logo.png" >
</p>

# About Ketch

Ketch is simple, powerful, customisable file downloader library for Android built entirely in Kotlin. It simplifies the process of downloading files in Android applications by leveraging the power of WorkManager. Ketch guarantees the download irrespective of application state.

<p align="center">
  <img height="500" alt = "High level design" src=https://raw.githubusercontent.com/khushpanchal/Ketch/master/assets/Sample_app.png >
</p>

# Why use Ketch

- Ketch can download any type of file. (jpg, png, gif, mp4, mp3, pdf, apk and many more)
- Ketch guarantees file download unless cancelled explicitly or download is failed.
- Ketch provide all download info including speed, file size, progress.
- Ketch provide various callbacks like onQueue, onStart, onProgress, onSuccess, onFailure, onCancel.
- Ketch provide observable flow of download items.
- Ketch can download multiple files in parallel.
- Ketch support cancellation of downloads.
- Ketch support large file downloads.
- Ketch provide various customisation including custom timeout and custom notification.
- Ketch is simple and very easy to use.
- Ketch provide notification for each download providing download info (speed, time left, total size, progress) with cancel action.

<p align="center">
  <img height="200" alt = "High level design" src=https://raw.githubusercontent.com/khushpanchal/Ketch/master/assets/Sample_notification.png >
</p>

# How to use Ketch

## Installation

To integrate Ketch library into your Android project, follow these simple steps:

- Update your settings.gradle file with the following dependency.
   
```Groovy
dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
    maven { url 'https://jitpack.io' } // this one
  }
}
```

- Update your module level build.gradle file with the following dependency.
   
```Groovy
dependencies {
  implementation 'com.github.khushpanchal:Ketch:1.0.0'
}
```

## Usage

- Simplest way to use Ketch:
  
  - Inside Activity or Fragment, create the instance of Ketch
    
    ```Kotlin
      private lateinit var ketch: Ketch
      override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ketch = Ketch.init(this)
      }
    ```
    
  - Call the download() function, pass the url, and get callback
    
    ```Kotlin
      val request = ketch.download(url,
        onQueue = {},
        onStart = { length -> },
        onProgress = { progress, speedInBytePerMs, length -> },
        onSuccess = { },
        onFailure = { error -> },
        onCancel = { }
      )
    ```
    
  ### Important Note: Add the appropriate storage permission based on API level or onFailure(error) callback will be triggered. Check out sample app for reference.
  
- To cancel the download
  
  ```Kotlin
      ketch.cancel(request.id)
  ```
  
- To enable the notification:
  
  - Add the notification permission in manifest file.
    
     ```
      <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
     ```
     
   - Request permission from user (required from Android 13 (API level 33)). Check out sample app for reference.
   - Pass the notification config while initialization
     
     ```Kotlin
       ketch = Ketch.init(this,
                notificationConfig = NotificationConfig(
                    enabled = true,
                    smallIcon = R.drawable.ic_launcher_foreground // It is required to pass the smallIcon for notification.
                )
              )
     ```
     
## Customisation

- Provide file name and path for download. (It is recommended to pass the fileName or library will assign random name)

  ```Kotlin
  ketch.download(url,
   path, //Default path: Download directory
   fileName //Default fileName: Random UUID.
  )
  ```
  
- Provide headers with network request.
  
  ```Kotlin
  ketch.download(url,
   headers = headers, //Default: Empty hashmap
  )
  ```
  
- Tag: Group various downloads by providing additional Tag. (This tag can be use to cancel the download as well)

  ```Kotlin
  ketch.download(url,
   tag = tag, //Default: null
  )
  ```
  
- Cancellation: In addition to cancel by request id, provides cancellation by tag and cancel all downloads.

  ```Kotlin
    ketch.cancel(tag)
  ```
  
  ```Kotlin
    ketch.cancelAll()
  ```
  
- Observing: Provides state flow of download items (Each item carries download info like url, fileName, path, tag, status, progress, length, timeQueued, speed, id)

  ```Kotlin
    //To observe from Fragment
    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        ketch.observeDownloads()
          .collect { 
             //set items to adapter
          }
      }
    }
    //observe from viewModel to survive configuration change
  ```

  ```Kotlin
    // To stop observing
    ketch.stopObserving()
  ```
  
- Download config: Provides custom connect and read timeout

  ```Kotlin
    ketch = Ketch.init(this,
                downloadConfig = DownloadConfig(
                    connectTimeOutInMs = 20000L, //Default: 10000L
                    readTimeOutInMs = 15000L //Default: 10000L
                )
              )
  ```
  
- Notification config: Provide custom notification config

  ```Kotlin
    ketch = Ketch.init(this,
                notificationConfig = NotificationConfig(
                   enabled = true, //Default: false
                   channelName = channelName, //Default: "File Download"
                   channelDescription = channelDescription, //Default: "Notify file download status"
                   importance = importance, //Default: NotificationManager.IMPORTANCE_HIGH
                   smallIcon = smallIcon, //It is required
                )
              )
  ```

# Blog

Check out the blog to understand working of Ketch (High Level Design): [https://medium.com/@khush.panchal123/ketch-android-file-downloader-library-7369f7b93bd1](https://medium.com/@khush.panchal123/ketch-android-file-downloader-library-7369f7b93bd1)

## Contact Me

- [LinkedIn](https://www.linkedin.com/in/khush-panchal-241098170/)
- [Twitter](https://twitter.com/KhushPanchal15)
- [Gmail](mailto:khush.panchal123@gmail.com)

## Check out my blogs: [https://medium.com/@khush.panchal123](https://medium.com/@khush.panchal123)

## If this project helps you, show love ❤️ by putting a ⭐ on [this](https://github.com/khushpanchal/Ketch) project ✌️

## Contribute to the project

Feel free to provide feedback, report an issue, or contribute to Ketch. Head over to [GitHub repository](https://github.com/khushpanchal/Ketch), create an issue or find the pending issue. All pull requests are welcome 😄

## License

```
   Copyright (C) 2024 Khush Panchal

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
