# Build Glass List APK using only an Android phone

This project includes a GitHub Actions workflow. The APK is built on GitHub's cloud runner, so Android Studio/SDK/Gradle do not need to be installed on your phone.

## Steps
1. In Chrome, sign in to GitHub and create a new repository named `GlassList`. Public or private is fine. Do not initialize it with a README, .gitignore, or license.
2. Open the new repository, choose **Add file > Upload files**, and upload the contents of this `GlassList` folder (including the hidden `.github` folder). If the GitHub mobile website hides dot-folders, switch Chrome to **Desktop site**.
3. Commit the uploaded files to the `main` branch.
4. Open the repository's **Actions** tab. If asked, enable workflows.
5. Select **Build Glass List APK** from the left side, tap **Run workflow**, choose `main`, and tap the green **Run workflow** button.
6. Wait for the workflow to finish with a green check. Open that run and scroll to **Artifacts**.
7. Download `GlassList-debug-apk`, extract the ZIP on your phone, and install `app-debug.apk`. If Android blocks it, allow Chrome or your file manager to install unknown apps for this installation.

The generated file is a debug APK, suitable for installing/testing. It is not a Play Store release-signed APK. Keep your GitHub account private and do not upload passwords or signing keys.
