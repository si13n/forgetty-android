# Google Play Data safety draft

This file is a source-of-truth draft for the Play Console form. Re-check it against the exact release bundle and every enabled Firebase or Google service before submission.

## High-level answers

| Play Console question | Proposed answer |
|---|---|
| Does the app collect or share required user data types? | Yes, when the user chooses Google sign-in and cloud sync |
| Is all collected user data encrypted in transit? | Yes |
| Can users request deletion? | Yes: in-app deletion and the public account-deletion page |
| Is data collection optional? | Yes; the core app works in guest mode |
| Does the app show ads or use an advertising SDK? | No |
| Is data sold? | No |

In Play terminology, transfers to Firebase acting as a service provider may qualify for the service-provider exception rather than “sharing.” Confirm this against the current Play definition while completing the form.

## Data types to declare

| Category | Data | Collected | Shared | Purpose | Optional |
|---|---|---:|---:|---|---:|
| Personal info | Name | Yes, after Google sign-in | No, subject to service-provider exception | Account management, app functionality | Yes |
| Personal info | Email address | Yes, after Google sign-in | No, subject to service-provider exception | Account management, app functionality | Yes |
| Personal info | User IDs | Yes, after Google sign-in | No, subject to service-provider exception | Account management, sync | Yes |
| App activity | Other user-generated content | Yes, for signed-in task content | No, subject to service-provider exception | App functionality, sync | Yes |
| Location | Approximate or precise location | Only if the user manually adds coordinates to a task | No, subject to service-provider exception | App functionality | Yes |
| Files and docs | Attachment metadata/reference | On device; verify whether any release path uploads files before declaring collection | No | App functionality, export | Yes |

Task content can include titles, notes, completion state, dates, reminders, recurrence, priorities, lists, tags, subtasks, attachment metadata, assignee labels, and user-entered location labels or coordinates.

## Data handling notes

- Guest task data stays in the app's local Room database.
- Signed-in task documents are stored under the authenticated Firebase user.
- Google profile fields are used to show and manage the signed-in account.
- Notifications are scheduled locally.
- Speech recognition runs only after user action and is handled by the device's configured recognition service.
- No analytics, crash reporting, advertising, or monetization SDK is intentionally included in the current dependency set.
- Account deletion removes task documents in paged batches, the user document, the Firebase Authentication account, and local app data.

## Before submitting

- Inspect the final dependency graph and release bundle for newly added SDKs.
- Confirm production Firestore rules prevent cross-user access.
- Confirm whether manually entered coordinates should be declared as precise location in the selected Play Console taxonomy.
- Confirm Firebase service-provider treatment under the current Play Data safety definitions.
- Keep the Play Console answers synchronized with the Privacy Policy.
