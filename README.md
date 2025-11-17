# Pocket Garden – Plant Identification Application


# Overview and Purpose
Pocket Garden is an Android Application that’s designed to streamline plant care through identification, tracking and personalised reminders. The app caters to users that both are beginner and experienced gardeners by providing a central hub for managing their virtual garden. 

# Prerequisites
•	Android Studio (Narwhal version or later)

•	An Android Device or Emulator running API level 24(Nougat) or higher

•	Internet Connection (in order to make use of the Plant Identification features)

•	Camera Access Permissions 


# Features
Currently Implemented Features:

•	Splash Screen:

A Welcome Screen once the Application is launched

•	User Authentication

User Registration and Login Pages using email

Biometric Authentication (Fingerprint authentication)


•	Home Page

The main navigation hub of the application

Provides needs of plants that are currently saved and kept track of

Provides Blogs that are helpful in Plant Care


•	Plant Identification

Uses the device camera to capture a plant for identification

Once plant is identified, the plant’s information and care guide is provided


•	My Garden Page

Users can view their saved plants on this page which provides a personal virtual garden


•	Settings Page

Allows users to manage their application preferences


• Backend Integration

Connects to REST API allowing for data synchronisation

Offline Mode enabled for specific features such as Plant Notes

• Real-time Notifications

Push notifications that alert user for any water or fertiliser reminders

Allows users to have customisable notification schedules


• Multi-language Support

Options of switching between English and 2 additional official South African languages


• Plant Diagnosis 

Makes use of camera to capture plant images

Displays whether the captured plant is sick or healthy

Provides detailed information about plant health issues


• App Icon

A custom application icon


# Installation

•	To run the project locally, we need to clone the repository using the following link:
https://github.com/ST10261605/PocketGarden

•	Once you have the link, open Android Studio, click “Clone Repository” and enter the repository link.

•	Allow Android Studio to sync the Gradle files and download the dependencies.

•	Connect your Android Device or begin the Emulator.

•	Click the “Run” button to build and install the app.


# Additional Setup Requirements

•	Ensure that biometric sensors are available for authentication testing

•	Ensure that notification permissions are configured for the device

•	Ensure that camera permissions are granted in order for plant identification features



# Tech Stack

•	Development Environment: Android Studio

•	Programming Language: Kotlin

•	Architecture: Model-View-ViewModel (MVM)

•	Backend: REST API with database connectivity

•	Authentication: Biometric, Google Sign-in and via Email


# Permissions Required

•	Camera: for plant identification and diagnosis

•	Internet Access: for API calls and synchronisation

•	Notifications: for plant reminders


# GitHub Link

https://github.com/ST10261605/PocketGarden

# Demonstration Video

https://youtu.be/dYUjv0y6c4A

# Acknowledgements

•	Fixing Error in Home Screen UI: https://chat.deepseek.com/a/chat/s/d52c1b31-6dce-410f-b426-df5c28672738

•	Fixing UI Button Error: https://chat.deepseek.com/a/chat/s/c44d6e19-ce45-42cb-9886-1be0be68e96b

•	Connecting with plant ID REST API through HTTP requests, converting API responses to Java or Kotlin objects, and enabling authentication and custom headers: https://www.geeksforgeeks.org/android/introduction-retofit-2-android-set-1/

•	Integration of Google Sign-In: https://developers.google.com/identity/sign-in/web/sign-in
