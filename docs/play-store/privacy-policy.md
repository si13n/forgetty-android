# Forgetty Privacy Policy

_Last updated: September 14, 2026_

Forgetty is a task-management app for Android. This policy explains what data the app handles and why.

## Guest mode

You can use Forgetty without signing in. Tasks, lists, tags, subtasks, reminder settings, and related app preferences created in guest mode are stored on your device. The guest task database is excluded from Android cloud backup and device-to-device transfer.

## Signed-in mode

If you choose Google sign-in, Forgetty uses Firebase Authentication to identify your account. The app may receive your Google account name, email address, profile photo URL, and a Firebase user identifier.

For signed-in users, task data is stored in Google Firebase Cloud Firestore under the authenticated user's account. This can include task titles, notes, completion state, dates, reminder settings, recurrence, priority, lists, tags, subtasks, attachment metadata, assignee labels, and any location label or coordinates that you choose to enter.

Forgetty uses this information only to provide sign-in, synchronization, task management, reminders, and user-requested export or sharing.

## Device features

- Notifications are scheduled and displayed on the device for reminders, tasks due today, and the Today digest. Notification permission is requested only when needed.
- Speech input is started only when you choose it and is handled by the speech-recognition service available on your Android device.
- Attachments are selected by you. Forgetty stores the attachment reference and metadata needed to display or export it; it does not upload the attachment file to Forgetty's own server.
- Export and sharing happen only after an action you initiate.

## Sharing and service providers

Forgetty does not sell personal data, show ads, or include an advertising SDK. Data is processed by Google services used to operate the app, including Firebase Authentication and Cloud Firestore. Google acts as a service provider under its applicable terms and privacy documentation.

Your device or operating-system services may also process data when you explicitly use Google sign-in, speech recognition, notifications, file selection, export, or Android backup.

## Security

Network traffic is sent over encrypted connections, and cleartext HTTP is disabled by the app. No method of storage or transmission is completely secure, so absolute security cannot be guaranteed.

## Retention and deletion

Guest data remains on the device until you delete it, clear app storage, or uninstall the app.

Signed-in task data remains in Firebase until you delete individual items or delete your account. In the app, open **Profile → Manage data → Delete account**. After Google reauthentication, Forgetty deletes the user's task documents, user document, Firebase Authentication account, and local app data.

You can also follow the instructions on the [Account deletion page](account-deletion.md). Deletion requests may require identity verification. Data may be retained where legally required or in limited provider backups until those backups expire.

## Children

Forgetty is a general productivity app and is not directed to children under 13. Do not use the app to submit another person's sensitive information without authorization.

## Changes

We may update this policy when the app or legal requirements change. The updated date above will show the latest revision.

## Contact

For privacy questions, open a support request in the [Forgetty issue tracker](https://github.com/si13n/forgetty-android/issues/new). Do not post passwords, tokens, task contents, or other sensitive data in a public issue. The maintainer will arrange a private verification channel when needed.
