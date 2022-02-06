# LetsChat
LetsChat is a Sample Messaging Android application built to demonstrate the use of Modern Android development tools - (Kotlin, Coroutines, Dagger-Hilt, Architecture Components, MVVM, Room, Coil) and Firebase

- Create a firebase project and replace the google-services.json file which you get from your firebase project console
- Following firebase services need to be enabled in the firebase console
  - Phone Auth
  - Cloud Firestore
  - Realtime Database
  - Storage
  - Composite indexes should be created for contact query(link for enabling indexes could be found from logcat while using the app)

***You can Install and test latest LetsChat app from below 👇***

[![LetsChat App](https://img.shields.io/badge/LetsChat-APK-blue.svg?style=for-the-badge&logo=android)](https://github.com/a914-gowtham/LetsChat/blob/master/app/app-debug.apk)

<p float="center">
  <img src="demo_video.gif" />
</p>
 
## Features ✨
- One on one chat
- Group Chat
- Typing status for one on one and group chat
- Unread messages count
- Message status for failed,sent,delivered and seen
- Supported message types
  - Text
  - Voice
  - Sticker and Gif
- Attachments 
  - Image
  - Video - InProgress
- Notification actions for reply and mark as read
- Search users by username 

## Built With 🛠
- [Kotlin](https://kotlinlang.org/) - First class and official programming language for Android development.
- [Coroutines & Flow](https://kotlinlang.org/docs/reference/coroutines-overview.html) - For asynchronous and more..
- [Android Architecture Components](https://developer.android.com/topic/libraries/architecture) - Collection of libraries that help you design quality, robust, testable, and maintainable apps.
  - [Navigation Component](https://developer.android.com/guide/navigation/navigation-getting-started) - Handle everything needed for in-app navigation with a single Activity.
  - [LiveData](https://developer.android.com/topic/libraries/architecture/livedata) - Data objects that notify views when the underlying database changes.
  - [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel) - Stores UI-related data that isn't destroyed on UI changes. 
  - [DataBinding](https://github.com/android/databinding-samples) - Generates a binding class for each XML layout file present in that module and allows you to more easily write code that interacts with views.Declaratively bind observable data to UI elements.
   - [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) - WorkManager is an API that makes it easy to schedule deferrable, asynchronous tasks that are expected to run even if the app exits or the device restarts.
  - [Room](https://developer.android.com/topic/libraries/architecture/room) - SQLite object mapping library.
- [Dependency Injection](https://developer.android.com/training/dependency-injection) - 
  - [Dagger-Hilt](https://dagger.dev/hilt/) - Standard library to incorporate Dagger dependency injection into an Android application.
  - [Hilt-ViewModel](https://developer.android.com/training/dependency-injection/hilt-jetpack) - DI for injecting `ViewModel`.
- [Firebase](https://firebase.google.com/) - 
  - [Cloud Messaging](https://firebase.google.com/products/cloud-messaging) - For Sending Notification to client app.
  - [Cloud Firestore](https://firebase.google.com/docs/firestore) - Flexible, scalable NoSQL cloud database to store and sync data.
  - [Cloud Storage](https://firebase.google.com/docs/storage) - For Store and serve user-generated content.
  - [Authentication](https://firebase.google.com/docs/auth) - For Creating account with mobile number.
- [Kotlin Serializer](https://github.com/Kotlin/kotlinx.serialization) - Convert Specific Classes to and from JSON.Runtime library with core serialization API and support libraries with various serialization formats.
- [Coil-kt](https://coil-kt.github.io/coil/) - An image loading library for Android backed by Kotlin Coroutines.



