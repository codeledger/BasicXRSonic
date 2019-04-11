# BasicXRSonic
Sample using BoseWear with Resonance Audio

Simple POC for using [Resonance Audio](https://resonance-audio.github.io/resonance-audio/) with [Bose AR SDK](https://developer.bose.com/bose-ar)

You should hear a chirping bird in front of you. Rotate yourself 360 degrees to experience effect.

## Instructions
* Check out Android project from GitHub
* Download and unzip Bose AR SDK in separate directory
* Find the AAR directory in Bose AR SDK
* Open up Android project in Android Studio
* To avoid including unreleased AAR files, you'll need to add them as modules
* In Android Studio, 'File'->'New'->'New Module'->'Import .JAR/.AAR Package' (scroll down) and navigate to AAR Directory and add AAR file
* Repeat for all three (3) AAR files
* Uncomment the `project dependencies` in app/build.gradle
```
//    implementation project(':blecore-release')
//    implementation project(':bosewearable-release')
//    implementation project(':bosewearableui-release')
```
* Build/Run Project


