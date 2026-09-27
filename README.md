# Calorie Lens AI — Free Offline Edition

Android app for estimating meal calories and macros without an OpenAI API key.

## What changed
- Removed OpenAI API integration and API-key field.
- Removed internet permission.
- Added Google's bundled ML Kit image-labeling model for on-device image analysis.
- Added a local nutrition database with Egyptian and common foods.
- AI suggestions are confirmed by the user before being added.
- Gram amounts can be edited and calories/macros recalculate instantly.
- Manual food search is always available when the vision model is unsure.

## Privacy / cost
Image labeling runs on-device using the bundled ML Kit model. No per-analysis API fee is required.

## Accuracy note
A normal photo cannot measure food weight precisely. The app uses image labels to suggest likely foods, then relies on a default portion that the user can edit. For best calorie accuracy, enter the real gram weight when known.
