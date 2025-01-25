[![](https://jitpack.io/v/khushpanchal/Ketch.svg)](https://jitpack.io/#khushpanchal/Ketch)
[![](https://androidweekly.net/issues/issue-622/badge)](https://androidweekly.net/issues/issue-622)

# Ketch

## An Android File downloader library based on WorkManager with pause and resume support

<p align="center">
  <img width="950" src="https://raw.githubusercontent.com/khushpanchal/Ketch/master/assets/Ketch_logo.png" >
</p>

# About Ketch

Ketch is a simple, powerful, customisable file downloader library for Android built entirely in Kotlin. It simplifies the process of downloading files in Android applications by leveraging the power of WorkManager. Ketch guarantees the download irrespective of application state.

<p align="center">
  <img height="500" alt = "High level design" src=https://raw.githubusercontent.com/khushpanchal/Ketch/master/assets/Sample_app.png >
</p>

# Why use Ketch

- Ketch can download any type of file. (jpg, png, gif, mp4, mp3, pdf, apk and many more)
- Ketch guarantees file download unless canceled explicitly or download is failed.
- Ketch provides all download info including speed, file size, progress.
- Ketch provides option to pause, resume, cancel, retry and delete the download file.
- Ketch provides option to observe download items (or single download item) as Flow.
- Ketch can download multiple files in parallel.
- Ketch supports large file downloads.
- Ketch provides various customisation including custom timeout, custom okhttp client and custom notification.
- Ketch is simple and very easy to use.
- Ketch provide notification for each download providing download info (speed, time left, total size, progress).
- Ketch includes option to pause, resume, retry and cancel download from notification.

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
  implementation 'com.github.khushpanchal:Ketch:2.0.2' // Use latest available version
}
```

## Usage

- Simplest way to use Ketch:
  
  - Create the instance of Ketch in application onCreate. (Ketch is a singleton class and instance will create automatically on first use)
    
    ```Kotlin
      private lateinit var ketch: Ketch
      override fun onCreate() {
        super.onCreate()
        ketch = Ketch.builder().build(this)
      }
    ```
    
  - Call the download() function, pass the url, path, fileName and observe the download status
    
    ```Kotlin
      val id = ketch.download(url, path, fileName)
      lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
          ketch.observeDownloadById(id)
            .flowOn(Dispatchers.IO)
            .collect { downloadModel -> 
              // use downloadModel
            }
        }
      }
    ```
    
  #### Important Note 1: Add the appropriate storage permission based on API level or onFailure(error) callback will be triggered. Check out sample app for reference.
  #### Important Note 2: Add FOREGROUND_SERVICE_DATA_SYNC, WAKE_LOCK, INTERNET permission. Check out sample app for reference.
  
- To cancel the download
  
  ```Kotlin
      ketch.cancel(downloadModel.id) // other options: cancel(tag), cancelAll()
  ```

- To pause the download
  
  ```Kotlin
      ketch.pause(downloadModel.id) // other options: pause(tag), pauseAll()
  ```

- To resume the download
  
  ```Kotlin
      ketch.resume(downloadModel.id) // other options: resume(tag), resumeAll()
  ```

- To retry the download
  
  ```Kotlin
      ketch.retry(downloadModel.id) // other options: retry(tag), retryAll()
  ```

- To delete the download

  ```Kotlin
      ketch.clearDb(downloadModel.id) // other options: clearDb(tag), clearAllDb(), clearDb(timeInMillis)
      ketch.clearDb(downloadModel.id, false) // Pass "false" to skip the actual file deletion (only clear entry from DB)
  ```

- Observing: Provides state flow of download items (Each item carries download info like url, fileName, path, tag, id, timeQueued, status, progress, length, speed, lastModified, metaData, failureReason, eTag)

  ```Kotlin
    //To observe from Fragment
    viewLifecycleOwner.lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        ketch.observeDownloads()
          .flowOn(Dispatchers.IO)
          .collect { 
             //set items to adapter
          }
      }
    }
  ```
  
- To enable the notification:
  
  - Add the notification permission in manifest file.
    
     ```
      <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
     ```
     
   - Request permission from user (required from Android 13 (API level 33)). Check out sample app for reference.
   - Pass the notification config while initialization
     
     ```Kotlin
     ketch = Ketch.builder().setNotificationConfig(
              config = NotificationConfig(
                enabled = true,
                smallIcon = R.drawable.ic_launcher_foreground // It is required to pass the smallIcon for notification.
              )
            ).build(this)
     ```
     
## Customisation
  
- Provide headers with network request.
  
  ```Kotlin
  ketch.download(url, path, fileName,
   headers = headers, //Default: Empty hashmap
  )
  ```
  
- Tag: Group various downloads by providing additional Tag. (This tag can be use to cancel, pause, resume, delete the download as well)

  ```Kotlin
  ketch.download(url, path, fileName,
   tag = tag, //Default: null
  )
  ```
  
- Download config: Provides custom connect and read timeout

  ```Kotlin
    ketch = Ketch.builder().setDownloadConfig(
      config = DownloadConfig(
        connectTimeOutInMs = 20000L, //Default: 10000L
        readTimeOutInMs = 15000L //Default: 10000L
      )
    ).build(this)
  ```

- Custom OKHttp: Provides custom okhttp client

  ```Kotlin
    ketch = Ketch.builder().setOkHttpClient(
      okHttpClient = OkHttpClient
        .Builder()
        .connectTimeout(10000L)
        .readTimeout(10000L)
        .build()
    ).build(this)
  ```
  
- Notification config: Provide custom notification config

  ```Kotlin
    ketch = Ketch.builder().setNotificationConfig(
      config = NotificationConfig(
        enabled = true, //Default: false
        channelName = channelName, //Default: "File Download"
        channelDescription = channelDescription, //Default: "Notify file download status"
        importance = importance, //Default: NotificationManager.IMPORTANCE_HIGH
        smallIcon = smallIcon, //It is required
        showSpeed = true, //Default: true
        showSize = true, //Default: true
        showTime = true //Default: true
      )
    ).build(this)
  ```

# Blog

Check out the blog to understand working of Ketch (High Level Design): [https://medium.com/@khush.panchal123/ketch-android-file-downloader-library-7369f7b93bd1](https://medium.com/@khush.panchal123/ketch-android-file-downloader-library-7369f7b93bd1)

### High level Design

<p align="center">
  <img width="950" src="https://raw.githubusercontent.com/khushpanchal/Ketch/master/assets/Ketch_hld.png" >
</p>

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
