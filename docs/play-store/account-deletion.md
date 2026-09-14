# Delete a Forgetty account and data

_Last updated: September 14, 2026_

## Delete from the Android app

1. Sign in to the Forgetty account you want to delete.
2. Open **Profile**.
3. Open **Manage data**.
4. Tap **Delete account**.
5. Confirm the warning and complete Google reauthentication.

Forgetty then deletes the account's task documents and user document from Firebase, deletes the Firebase Authentication account, and clears local Forgetty data on the device. This includes task titles, notes, dates, reminders, recurrence, priorities, lists, tags, subtasks, attachment metadata, assignee labels, and user-entered location data associated with that account.

## Request deletion without the app

Open a request in the [Forgetty support tracker](https://github.com/si13n/forgetty-android/issues/new?title=Account%20deletion%20request).

Include only the Google email address used for Forgetty and ask for a private verification channel. Do not include passwords, sign-in tokens, task contents, or other sensitive information in a public issue. The maintainer must verify that the requester controls the account before deleting data.

## Retention

Active signed-in data is retained until it is deleted. Some data may remain temporarily in encrypted service-provider backups or be retained when required by law; it is not restored to the active account after deletion.

For more detail, read the [Privacy Policy](privacy-policy.md).
