# n8n Monitor

A small, read-only Android client for one self-hosted n8n instance.

## What works

- Stores the n8n URL and API key with Android encrypted preferences.
- Tests the connection before you rely on it.
- Lists workflows and their recent executions through n8n's public API.
- Checks for new executions with `status=error` every 15, 30, or 60 minutes.
- Opens the failed workflow's executions when you tap a notification.

The first background check records a baseline and does not notify about old failures.

## Requirements

- Android 8.0 (API 26) or newer.
- An n8n instance available over HTTPS.
- An n8n API key with read access to workflows and executions.

Create an API key in n8n under **Settings → n8n API**, then enter the instance root URL such as `https://n8n.example.com`. The app calls `/api/v1/workflows` and `/api/v1/executions`; it never modifies workflows.

## Build

Install JDK 17 and Android SDK 36, then run:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

The test APK is written to `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions runs the same checks and uploads that APK as a workflow artifact.

For a Google Play bundle, run `./gradlew lintRelease bundleRelease`, then sign
`app/build/outputs/bundle/release/app-release.aab` with the existing private upload
key using `jarsigner`. Keep signing passwords and Play credentials outside this
repository. Version 1.1.0 uses version code 11, continuing the existing Play app's
version history. Existing testers may need to re-enter their connection settings.

## Current limits

- One n8n instance per app installation.
- The workflow list is capped at 250 items.
- Execution lists and the background baseline are capped at the newest 100 items.
- Android schedules periodic work opportunistically, so a check can run later than its selected interval.
- The GitHub release contains a debug-signed test APK. Publishing to Google Play requires a private release signing key and Play Console credentials, which are deliberately not stored in this repository.

## Security

Only HTTPS endpoints are accepted. Redirects are rejected so the API key cannot be forwarded to another endpoint. Android's normal certificate validation remains enabled, backups are disabled, and the API key is not logged or committed to source control.
