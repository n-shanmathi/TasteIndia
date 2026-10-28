# TasteIndia

TasteIndia is a native Android app for discovering Indian recipes. Users can search and filter recipes, view cooking instructions, use Cook Mode, and save favourites. The app uses TheMealDB for recipe data and Firebase for authentication and cloud favourites.

## Tech Stack

- Kotlin and Jetpack Compose
- Material 3 and Navigation Compose
- MVVM with Repository pattern
- Retrofit and OkHttp for API requests
- Room for local recipe caching
- Coil for recipe images
- Kotlin Coroutines and StateFlow
- Firebase Authentication and Cloud Firestore
- JUnit for unit tests

Check the project Gradle files and wrapper for the exact toolchain versions. The documented target is Android API 35, with a minimum API of 26.

## Setup

1. Open the project in Android Studio.
2. Use a compatible JDK and install the required Android SDK.
3. Configure Firebase as described below.
4. Sync Gradle and run the `app` configuration on an emulator or Android device.

Useful commands:

```bash
./gradlew app:assembleDebug
./gradlew app:testDebugUnitTest
./gradlew signingReport
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Firebase Setup

1. Register the Android app in Firebase using the application ID from the app-level Gradle file.
2. Add `google-services.json` to the `app/` directory.
3. Enable Email/Password and Google providers in Firebase Authentication.
4. Configure the required SHA-1 and SHA-256 fingerprints for Google Sign-In.
5. Create a Cloud Firestore database and publish the project's reviewed security rules.

Favourites should be stored per user at:

```text
users/{uid}/favourites/{mealId}
```

The `uid` must come from the signed-in Firebase user. Confirm the Firebase project ID matches the project open in the Firebase Console. After favouriting a recipe, verify that its document appears in Firestore. Never report a cloud save as successful if the write fails.

Do not place service-account private keys or OAuth client secrets in the Android app.

## Architecture

The app follows MVVM and the Repository pattern.

- **UI:** Compose screens display state and send user actions.
- **ViewModels:** Manage screen state with StateFlow.
- **Repositories:** Coordinate remote data, local caching, and favourites.
- **Remote data:** Retrofit calls TheMealDB.
- **Local data:** Room can cache recipe details for offline access.
- **Authentication:** Firebase manages user identity and sessions.
- **Favourites:** Firestore stores each user's favourite recipe IDs separately.

Adjust this overview to match the actual files in the repository.

## Navigation

Main destinations:

- **Sign In / Sign Up:** Firebase email/password and Google authentication.
- **Forgot Password:** Requests a password-reset email after the user presses the send button.
- **Discover:** Browse, search, filter, and sort recipes.
- **Recipe Details:** View ingredients, measures, instructions, and available source links.
- **Cook Mode:** Follow instructions one step at a time.
- **Favourites:** View saved recipes for the current account.

The app follows Android's system light/dark setting; there is no in-app theme switcher.

## API Endpoints

Base URL: `https://www.themealdb.com/api/json/v1/1/`

| Purpose | Endpoint |
|---|---|
| Indian recipes | `filter.php?a=Indian` |
| Recipe details | `lookup.php?i={mealId}` |
| Category list | `list.php?c=list` |
| Ingredient list | `list.php?i=list` |
| Category filter | `filter.php?c={category}` |
| Ingredient filter | `filter.php?i={ingredient}` |
| Search by name | `search.php?s={query}` |

Recipe data is provided by [TheMealDB](https://www.themealdb.com/).

## Search and Filtering

The Indian recipe list is the base set. Category and ingredient results are intersected with this set so filters do not introduce recipes outside the selected Indian collection. When multiple filters are active, their ID sets are intersected. Search and sorting are then applied to the matching results.

Search input should be debounced, and older requests should not overwrite newer results. Use the stable TheMealDB meal ID for recipe identity and navigation.

## Persistence and Cache Policy

- Firestore is the cloud source for account-specific favourites.
- Room may cache recipe details for offline viewing.
- Do not use one global local favourites list for all accounts.
- Clear the previous user's in-memory favourites when the account changes.
- Show loading and error states for Firestore operations.
- Cached recipes may be available offline; uncached recipes require a network connection.
- Verify the actual cache refresh and expiry behavior in the implementation.

## Assumptions and Trade-offs

- TheMealDB availability and completeness depend on the external service.
- Some recipe fields may be empty, so the UI should omit unavailable details.
- Firebase configuration and signing fingerprints vary by development environment.
- Firestore requires security rules that restrict each user to their own UID path.
- Local caching improves offline access but does not guarantee that every recipe is available offline.

## Testing

Run:

```bash
./gradlew app:testDebugUnitTest
BUILDBUILDBUILD SUCCESSFUL
```

Check these flows manually:

- Email/password sign-up and sign-in.
- Google sign-in and cancellation handling.
- Password reset only after the user requests it.
- Favourite add/remove and Firestore persistence.
- Two different accounts see separate favourites.
- Search, filters, sorting, recipe details, and Cook Mode.
- Light and dark themes follow Android system settings.
- Loading, network-error, and retry states.

Only report tests as passing after running them.

## Known Issues and Verification

Before submission, verify:

- Toolchain versions against Gradle configuration.
- Application ID and Firebase project ID.
- Google Sign-In configuration and signing fingerprints.
- Firestore rules and actual document writes.
- Account-specific favourite isolation.
- Build and test results.

Record any unresolved issues here rather than claiming unverified functionality works.

## Time Spent

- Implementation: 6 Hours
- Testing and debugging: 3 Hours
- Documentation: 1 Hour
- Total: 10 Hours

## AI and Tool Use

Android Studio Gemini AI
Firebase Project AI
