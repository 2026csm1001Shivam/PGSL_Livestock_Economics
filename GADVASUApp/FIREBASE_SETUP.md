# Firebase setup (do this once, about 15 minutes)

The app uses **Firebase Authentication** (login with mobile OTP or email + password) and
**Cloud Firestore** (cloud backup of every record). Android Studio will not build the app until
step 3 is done, because it needs your project's `google-services.json`.

## 1. Create the Firebase project
1. Open https://console.firebase.google.com and click **Create a project**.
2. Name it, for example `GADVASU Livestock`. Google Analytics is optional (you can turn it off).

## 2. Register the Android app
1. In the project, click the **Android** icon (**Add app**).
2. **Android package name:** `com.gadvasu.livestockapp` (must match exactly).
3. **Debug signing certificate SHA-1:** needed for mobile OTP login. Get it on Windows with:
   ```
   keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
   ```
   (`keytool` comes with Android Studio: `C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe`.)
   Copy the **SHA1** and **SHA-256** lines. Add both: *Project settings → Your apps → Add fingerprint*.
4. Click **Register app**.

## 3. Add `google-services.json`
1. Download **google-services.json** from the same screen (or *Project settings → Your apps*).
2. Put it in the **`GADVASUApp/app/`** folder (next to `app/build.gradle`).
3. Skip the console's "add Firebase SDK" step: the Gradle files already contain it.

## 4. Turn on login methods
*Build → Authentication → Get started → Sign-in method*:
1. Enable **Email/Password**.
2. Enable **Phone**. Under **Phone numbers for testing**, add a test number, for example
   `+91 9999999999` with code `123456`. Test numbers log in without a real SMS.
   Sending real SMS to farmers may require upgrading the project to the Blaze (pay-as-you-go) plan;
   Firebase shows a prompt if it does.

## 5. Create the database and its security rules
1. *Build → Firestore Database → Create database*. Location: **asia-south1 (Mumbai)**. Start in **production mode**.
2. Open the **Rules** tab, replace everything with the contents of **`firebase/firestore.rules`**, click **Publish**.
   Without this step every save is refused (`PERMISSION_DENIED` in Logcat).
3. Indexes (`firebase/firestore.indexes.json`) are **not needed** by this version: the app downloads a
   farmer's records once and filters them on the phone. Keep the file for later server-side reports.

## 6. Run
Open the `GADVASUApp` folder in Android Studio → wait for Gradle sync → Run ▶.

### Check that it works
- Log in with the test number `9999999999`, OTP `123456`.
- Save a Livestock record. In the Firebase console → Firestore you should see
  `users / <uid> / dailyRecords / livestock_<date>`.
- Uninstall the app, install again, log in with the same number: the record comes back.
- Turn on airplane mode, save a record, turn it off: it uploads by itself.

## For the team
- Commit `app/google-services.json` to the repository so all four of you can build. It identifies the
  project but is not a password: data is protected by the Firestore rules and login.
- Mobile OTP works on a teammate's computer only after **their** debug SHA-1 is added (step 2.3).
  Email login works without it.

## Common problems
| Message | Fix |
|---|---|
| `File google-services.json is missing` | Step 3 |
| OTP fails with "app not authorized" / "missing a valid app identifier" | Add the SHA-1 and SHA-256 (step 2.3), download `google-services.json` again |
| `PERMISSION_DENIED` in Logcat, nothing appears in Firestore | Publish the rules (step 5.2) |
| Email login: "Wrong email or password" for a new farmer | Tap **Create account** the first time |
