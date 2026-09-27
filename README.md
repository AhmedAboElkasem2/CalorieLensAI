# Calorie Lens AI

Android app for practical AI-assisted food calorie and macro estimation.

## Features
- Select/capture a meal image
- Optional second angle
- Optional plate diameter for scale
- Oil/sauce/cooking notes
- Ingredient + gram estimates
- Calories, protein, carbs and fat
- Estimated calorie range and confidence
- Edit grams and instantly recalculate totals
- Arabic-first dark UI and Egyptian/Arabic food awareness
- API key stored with Android encrypted preferences
- GitHub Actions workflow builds `app-debug.apk`

## Build on GitHub
Push the project to the `main` branch. Open **Actions → Build Android APK**.
After the job succeeds, download the artifact named `CalorieLensAI-debug-apk`.

## OpenAI API key
The app asks for your own OpenAI API key on-device. Never commit a key to GitHub.

> Image-based nutrition is an estimate. For highest accuracy, weigh ingredients when possible.
