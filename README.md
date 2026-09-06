# Chess Overlay

Android overlay app for drawing and arranging six families of transparent geometric squares above other apps.

## Current v0.1 scope

- Small dark floating control button.
- Single tap opens/closes the control menu.
- Double tap on the floating button toggles Edit Mode.
- Enable/disable all shapes at once.
- First activation creates two copies of each of the six types (12 total) unless a startup layout was saved previously.
- Add any one of the six shape types independently.
- Transparent square interior with fully opaque colored outline/branches.
- Type 1: four straight orthogonal branches from side midpoints.
- Type 2: four diagonal X branches from corners.
- Type 3: four 90-degree bent branches inspired by a knight path.
- Type 4: combines Type 1 and Type 2 (8 branches).
- Type 5: two branches from the lower corners.
- Type 6: initial king-style cross/crown branch geometry; intended to be refined with the next visual specification.
- Per-instance length editing with a 1–100 slider; adjusted value also becomes the default for new instances of that type.
- In Edit Mode: tap selects a square, hold for 0.3 seconds then drag to move, double tap deletes.
- Save Startup Layout stores shape count, type, center position, and branch length in SharedPreferences.
- Outside Edit Mode the geometry canvas is `FLAG_NOT_TOUCHABLE`, so user touches pass through to the app underneath.

## Android build

- Application ID: `com.chess.overlay`
- minSdk: 26
- targetSdk / compileSdk: 36 (Android 16)
- Java 17
- Android Gradle Plugin 9.3.1
- Gradle 9.5.0

The app requests `SYSTEM_ALERT_WINDOW`. On Android 14+ the persistent overlay service is declared as a `specialUse` foreground service and displays a low-importance notification.

## First run

1. Build and install the app.
2. Open **Chess Overlay**.
3. Grant **Display over other apps** permission.
4. Tap the floating chess button.
5. Enable all shapes or add specific types.
6. Enter Edit Mode to move/delete/select shapes and change lengths.
7. Press **حفظ وضع البدء** to persist the current arrangement.
