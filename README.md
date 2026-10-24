# AnDraw

A cozy Android tracing studio made for anime fanart. AnDraw layers a reference image over the live rear-camera view so you can line it up with paper under a phone stand and trace without needing transparent paper.

## MVP features

- Material 3 interface with a warm, soft anime-art-studio palette.
- Import an image from Android's document picker.
- Live rear-camera preview with a translucent, movable reference layer.
- Pinch, drag, and rotate the reference before locking alignment.
- Ink-only mode that removes bright pixels from light backgrounds, making line art easier to trace over the camera view.
- Opacity and screen-brightness controls, optional alignment guides, grayscale, flashlight, recenter, and alignment lock.
- Remember the last imported reference using a persisted document URI.

## Build

Open this folder in Android Studio with Android SDK Platform 35 and JDK 17 installed. The project uses Gradle 8.10.2 and Android Gradle Plugin 8.8.2. You can also build from a shell with Gradle 8.10.2 installed:

```sh
gradle assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Notes

- Camera permission is required for the live tracing preview. If permission is denied, reopen the tracing screen and grant it in Android settings.
- For best alignment, secure the phone in your existing stand, point the rear camera straight down, put the paper inside the camera view, and adjust the reference until it matches the paper. Lock alignment before tracing.
- The current version locks the reference image's transform; it does not yet automatically compensate for the phone or paper moving after calibration.
- Ink-only mode is intended for dark line art on a light background. It deliberately removes near-white pixels and is not a general-purpose background remover.
- Images are processed locally on the device. No account or network access is required.
