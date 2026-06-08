# PingMon

PingMon is a native Android network and server uptime monitor. It lets an operator save hosts, domains, or URLs, run manual checks from a console-style dashboard, and receive local notifications when a monitored target is detected as DOWN by the background worker.

The app is built with Kotlin, XML layouts, MVVM, Room, OkHttp, WorkManager, Coroutines, and Flow.

## Table of Contents

- [Overview](#overview)
- [Current Features](#current-features)
- [Screens and User Flow](#screens-and-user-flow)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Architecture](#architecture)
- [Monitoring Logic](#monitoring-logic)
- [Background Checks and Notifications](#background-checks-and-notifications)
- [Local Database](#local-database)
- [Session and Dummy Login](#session-and-dummy-login)
- [Setup](#setup)
- [Build and Run](#build-and-run)
- [Debugging](#debugging)
- [Testing Targets](#testing-targets)
- [Known Limitations](#known-limitations)
- [Development Notes](#development-notes)

## Overview

PingMon monitors a list of targets and stores their latest uptime state locally.

Each target has:

- A display name.
- A host address, IP address, domain, or URL.
- An internal protocol value currently stored as `AUTO`.
- Latest status, latency, and checked timestamp.

The dashboard has a terminal-inspired visual style and shows:

- Logged-in operator name.
- Server list.
- UP, DOWN, or UNKNOWN status.
- Last latency.
- Last check timestamp.
- A short in-app debug console.
- Manual `Check Now` action.
- Add server dialog.
- Logout action.

## Current Features

- Dummy email and password login.
- Simple local session persistence using `SharedPreferences`.
- Server CRUD with Room.
- Reactive server list using `Flow`.
- MVVM separation for UI state and business logic.
- XML-based UI with RecyclerView, CardView, FAB, and custom drawables.
- Safe-area handling through `WindowInsetsCompat`.
- Smart protocol detection without exposing a protocol radio option in the UI.
- Lowercase normalization for host address input.
- Manual checks from the dashboard.
- Background checks every 15 minutes minimum through WorkManager.
- Local DOWN notifications.
- Notification dedupe per DOWN incident.
- Public DNS fallback for emulator or device DNS issues.
- Logcat diagnostics under the `PingMonDebug` tag.

## Screens and User Flow

1. User opens the app.
2. `LoginActivity` checks local session.
3. If a session exists, the app opens `MainActivity`.
4. If no session exists, user logs in with dummy credentials.
5. Dashboard loads Room data and schedules periodic background monitoring.
6. User adds a server through the add target dialog.
7. Address input is normalized to lowercase.
8. The app immediately starts a manual check after saving the server.
9. The target card updates with UP or DOWN status.
10. WorkManager continues periodic checks while the app is in the background.
11. If a target is DOWN, a local notification is sent once for that DOWN incident.
12. If the target later becomes UP, the DOWN alert state is cleared.

## Tech Stack

| Area | Library or Tool |
| --- | --- |
| Language | Kotlin |
| UI | XML layouts |
| Architecture | MVVM |
| Local database | Room |
| Networking | OkHttp, Java sockets, Android ping process |
| Background work | WorkManager |
| Async | Kotlin Coroutines, Flow, StateFlow, SharedFlow |
| List UI | RecyclerView |
| Components | Material FAB, CardView |
| Build | Gradle wrapper |

Important versions from `gradle/libs.versions.toml`:

| Dependency | Version |
| --- | --- |
| Android Gradle Plugin | 9.1.0 |
| Kotlin | 2.3.20 |
| Gradle Wrapper | 9.3.1 |
| Room | 2.8.4 |
| WorkManager | 2.11.2 |
| OkHttp | 5.3.2 |
| RecyclerView | 1.4.0 |
| CardView | 1.0.0 |
| Material Components | 1.14.0 |

Note: Compose dependencies are still present from the project template, but the app screens in this project use XML layouts.

## Project Structure

```text
PingMon/
  app/
    src/main/
      AndroidManifest.xml
      java/cloud/meis/
        LoginActivity.kt
        MainActivity.kt
        data/
          local/
            dao/
              ServerDao.kt
            database/
              AppDatabase.kt
            entity/
              ServerEntity.kt
            preference/
              SessionManager.kt
          model/
            PingResult.kt
            ServerProtocol.kt
            UserProfile.kt
          repository/
            ServerRepository.kt
        network/
          AutoPinger.kt
          IcmpPinger.kt
          NetworkPinger.kt
        ui/
          login/
            LoginEvent.kt
            LoginViewModel.kt
          main/
            MainUiState.kt
            MainViewModel.kt
            ServerAdapter.kt
          theme/
        worker/
          PingWorker.kt
      res/
        drawable/
        layout/
          activity_login.xml
          activity_main.xml
          dialog_add_server.xml
          item_server.xml
        values/
          colors.xml
          strings.xml
          themes.xml
        xml/
          network_security_config.xml
  gradle/
  build.gradle.kts
  settings.gradle.kts
```

## Architecture

PingMon follows a pragmatic MVVM architecture.

### UI Layer

Files:

- `LoginActivity.kt`
- `MainActivity.kt`
- `ServerAdapter.kt`
- XML layouts in `app/src/main/res/layout`

Responsibilities:

- Inflate XML layouts.
- Handle user interactions.
- Apply safe-area insets.
- Bind RecyclerView items.
- Observe ViewModel state.
- Show Toasts and dialogs.
- Request notification runtime permission.

### ViewModel Layer

Files:

- `LoginViewModel.kt`
- `MainViewModel.kt`
- `MainUiState.kt`

Responsibilities:

- Validate login credentials.
- Expose UI state with `StateFlow`.
- Emit one-shot UI events with `SharedFlow`.
- Add and delete servers.
- Trigger manual checks.
- Coordinate repository calls.
- Keep check state from getting stuck after deletion or cancellation.

### Repository Layer

File:

- `ServerRepository.kt`

Responsibilities:

- Provide a clean API over `ServerDao`.
- Keep Room details out of ViewModels and workers.
- Expose reactive server lists.
- Update latest status, latency, and check time.

### Data Layer

Files:

- `ServerEntity.kt`
- `ServerDao.kt`
- `AppDatabase.kt`
- `SessionManager.kt`

Responsibilities:

- Store server targets in Room.
- Store simple login session data in `SharedPreferences`.
- Provide database singleton access.

### Network Layer

Files:

- `AutoPinger.kt`
- `NetworkPinger.kt`
- `IcmpPinger.kt`

Responsibilities:

- Decide the best check strategy automatically.
- Resolve domains.
- Probe TCP connectivity.
- Optionally read HTTP response status.
- Fallback to ICMP when network checks fail.
- Return a normalized `PingResult`.

### Worker Layer

File:

- `PingWorker.kt`

Responsibilities:

- Run periodic checks in the background.
- Read all saved servers from Room.
- Update latest status and latency.
- Send local notifications for DOWN incidents.
- Avoid repeated DOWN notification spam until a target recovers.

## Monitoring Logic

PingMon currently uses automatic target checking. The UI does not expose a protocol selector.

### Address Normalization

When a server is added:

- Server name is trimmed.
- Host address is trimmed.
- Host address is converted to lowercase.
- Internal protocol is saved as `AUTO`.

### Candidate URL Selection

`NetworkPinger` builds check candidates from the saved address:

| Input | Candidate order |
| --- | --- |
| `http://example.com` | `http://example.com` |
| `https://example.com` | `https://example.com` |
| `139.59.242.94` | `http://139.59.242.94`, then `https://139.59.242.94` |
| `example.com` | `https://example.com`, then `http://example.com` |

### DNS Resolution

For domains, PingMon resolves addresses in this order:

1. Public DNS A record lookup through UDP against `1.1.1.1`.
2. Public DNS A record lookup through UDP against `8.8.8.8`.
3. Android system DNS lookup.

Resolved addresses are sorted with IPv4 first. This was added because some Android emulator networks can leave system DNS empty or prefer addresses that time out.

### TCP Probe

For every candidate URL, PingMon first performs a bounded TCP connect:

- HTTPS uses port `443` unless the URL has an explicit port.
- HTTP uses port `80` unless the URL has an explicit port.
- Connect timeout is short and bounded.
- The probe is executed through a dedicated executor so a slow socket path does not block the UI.

If TCP succeeds, the target is considered reachable.

### HTTP Status Probe

After TCP succeeds, PingMon tries an OkHttp request with a short timeout.

Current reachability rule:

- HTTP status `100..499` is treated as reachable.
- `5xx`, timeout, DNS failure, and socket failures are treated as not reachable for HTTP status purposes.

If HTTP does not return quickly but TCP already succeeded, PingMon uses the TCP result and marks the source with `http-no-result`.

Example source:

```text
tcp:capster.in:443:64.29.17.65:http-no-result
```

This means the host accepted a TCP connection, but HTTP response parsing did not complete before the short app timeout.

### ICMP Fallback

If the network check is DOWN, `AutoPinger` falls back to `IcmpPinger`.

`IcmpPinger` uses Android's system ping command:

```text
ping -c 1 -W 2 <host>
```

The process is bounded with a 3-second wait. Parsed latency is read from the ping output when available.

## Background Checks and Notifications

`MainActivity` schedules a unique periodic WorkManager job:

```kotlin
PeriodicWorkRequestBuilder<PingWorker>(15, TimeUnit.MINUTES)
```

Work name:

```text
pingmon_periodic_ping
```

Policy:

```text
ExistingPeriodicWorkPolicy.UPDATE
```

Constraint:

```text
NetworkType.CONNECTED
```

### Important WorkManager Behavior

Android enforces 15 minutes as the minimum periodic WorkManager interval. This is not an exact timer. The system can delay work depending on battery, app standby bucket, Doze, network, and scheduler policy.

For this app:

- Manual checks are immediate through `Check Now`.
- Background checks are periodic and system-managed.
- The worker should run after the app has been opened at least once, because scheduling happens in `MainActivity`.

### Notification Behavior

The worker sends a DOWN notification when:

- A target check result is DOWN.
- Notifications are allowed for the app.
- The `PingMon Alerts` notification channel is enabled.
- The target has not already triggered a DOWN notification in the current DOWN incident.

The app stores DOWN alert state in `SharedPreferences`:

```text
pingmon_down_alerts
```

This prevents repeated notifications every 15 minutes while a server remains DOWN.

When a target becomes UP again, its stored DOWN alert flag is cleared. If the same target goes DOWN later, a new notification can be sent.

### Notification Requirements

Manifest permissions:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

On Android 13 and newer, the user must allow notification permission at runtime.

Notification channel:

```text
PingMon Alerts
```

## Local Database

Room database:

```text
pingmon.db
```

Room version:

```text
1
```

Schema export:

```text
false
```

### ServerEntity Schema

Table:

```text
servers
```

| Kotlin property | Column name | Type | Notes |
| --- | --- | --- | --- |
| `id` | `id` | `Int` | Primary key, auto-generate |
| `serverName` | `server_name` | `String` | Display name |
| `hostAddress` | `host_address` | `String` | IP, domain, or URL |
| `protocol` | `protocol` | `String` | Currently saved as `AUTO` |
| `lastStatus` | `last_status` | `Boolean` | `true` is UP, `false` is DOWN |
| `lastLatency` | `last_latency` | `Int` | Latest latency in milliseconds |
| `lastChecked` | `last_checked` | `Long` | Epoch milliseconds |

### DAO Operations

`ServerDao` provides:

- `getAllServers(): Flow<List<ServerEntity>>`
- `getAllServersOnce(): List<ServerEntity>`
- `getServerById(id)`
- `insertServer(server)`
- `updateServer(server)`
- `updateServerStatus(id, isUp, latencyMs, checkedAt)`
- `deleteServer(server)`
- `deleteServerById(id)`

## Session and Dummy Login

The login system is local and intentionally simple for development.

Session storage:

```text
SharedPreferences: pingmon_session
```

Stored keys:

```text
logged_in
user_name
```

### Dummy Users

| Email | Password | Display name |
| --- | --- | --- |
| `rifky@pingmon.test` | `123456` | `Rifky Abdul Hanan` |
| `jesslyn@pingmon.test` | `654321` | `Jesslyn Eklesia` |

Email input is normalized to lowercase before matching.

## Setup

### Requirements

- Android Studio with Android SDK installed.
- Android SDK for compile SDK 36.
- JDK compatible with the Android Gradle Plugin used by this project. Android Studio's bundled JDK is recommended.
- A physical Android device or emulator.
- Internet access for Gradle dependency resolution.

### Clone and Open

```powershell
git clone <repo-url>
cd PingMon
```

Open the root folder in Android Studio, then let Gradle sync.

### Android Package

Application ID:

```text
cloud.meis
```

Launcher activity:

```text
cloud.meis.LoginActivity
```

## Build and Run

### Build Debug APK

```powershell
.\gradlew.bat :app:assembleDebug
```

Debug APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Install on Connected Device or Emulator

```powershell
.\gradlew.bat :app:installDebug
```

### Run Unit Tests

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

### Run Instrumented Tests

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
```

### Clear App Data

Use this when you want to reset login session, Room database, notification dedupe state, and WorkManager state:

```powershell
adb shell pm clear cloud.meis
```

After clearing data, open the app again so `MainActivity` can schedule the periodic worker.

## Debugging

### Logcat

PingMon writes diagnostics with this tag:

```text
PingMonDebug
```

Use:

```powershell
adb logcat -s PingMonDebug
```

Useful log lines:

```text
check start count=3
start capster.in
dns public start host=capster.in server=1.1.1.1
dns result host=capster.in provider=public addresses=64.29.17.65,216.198.79.65
tcp connect host=capster.in address=64.29.17.65 port=443
tcp result target=https://capster.in up=true latency=102 source=tcp:capster.in:443:64.29.17.65
http no-result target=https://capster.in fallback=tcp:capster.in:443:64.29.17.65
done capster.in UP 102ms tcp:capster.in:443:64.29.17.65:http-no-result
worker start count=3
worker result a.test up=false latency=0 source=dns-timeout:a.test
notify sent serverId=3
```

### Check Notification Permission

```powershell
adb shell appops get cloud.meis POST_NOTIFICATION
```

Expected healthy output includes:

```text
Default mode: allow
```

### Inspect Notification State

```powershell
adb shell dumpsys notification --noredact
```

Search for:

```text
cloud.meis
PingMon Alerts
Server DOWN
```

### Inspect WorkManager JobScheduler State

```powershell
adb shell dumpsys jobscheduler cloud.meis
```

Look for:

```text
androidx.work.systemjobscheduler
cloud.meis/androidx.work.impl.background.systemjob.SystemJobService
```

If forcing a WorkManager-backed JobScheduler job, Android 15+ may require the namespace:

```powershell
adb shell cmd jobscheduler run -f -n androidx.work.systemjobscheduler cloud.meis <job_id>
```

Note: for periodic WorkManager, forcing a job before its scheduled time can still be ignored by WorkManager with a message similar to:

```text
Delaying execution because it is being executed before schedule.
```

Manual app checks should be tested with the `Check Now` button.

### Common Debug Sources

| Source prefix | Meaning |
| --- | --- |
| `http:<code>` | HTTP response was received |
| `tcp:<host>:<port>:<ip>` | TCP connection succeeded |
| `http-no-result` | TCP succeeded but HTTP response did not finish before timeout |
| `dns-timeout:<host>` | DNS resolution did not return in time |
| `dns-empty:<host>` | DNS returned no address |
| `tcp-timeout:<host>:<port>:<ip>` | TCP connection timed out |
| `icmp:<host>` | ICMP ping succeeded |
| `icmp-timeout:<host>` | ICMP ping did not finish in time |
| `target-timeout` | Manual check exceeded the ViewModel timeout |
| `worker-timeout` | Worker check exceeded the worker timeout |

## Testing Targets

Useful UP targets:

```text
google.com
capster.in
http://139.59.242.94
139.59.242.94
```

Useful DOWN targets:

```text
a.test
test.test
invalid.invalid
```

Notes:

- Domains under `.test` should not resolve on public DNS.
- Some networks block ICMP, so ICMP failure does not always mean the server is down.
- TCP success means the selected port is reachable.
- HTTP status success means the app received a bounded HTTP response.

## Known Limitations

- Authentication is dummy-only and local.
- Session storage uses plain `SharedPreferences`.
- Room has no migration strategy yet because the database is still at version 1.
- WorkManager periodic jobs are not exact alarms.
- Background checks can be delayed by Android system policy.
- Android emulator DNS can be unreliable, which is why public DNS fallback exists.
- Public DNS fallback currently queries A records over UDP and prefers IPv4.
- DNS-over-HTTPS is not implemented yet.
- ICMP depends on the Android system `ping` command and device policy.
- TCP reachability is not the same as full application health.
- HTTP `100..499` is treated as reachable by design for uptime reachability, not content correctness.
- The UI is XML-based, but Compose template dependencies still exist in Gradle.
- Notification delivery depends on runtime permission, channel settings, and OS notification policy.

## Development Notes

### Add a New Field to `ServerEntity`

If the Room schema changes:

1. Add the field to `ServerEntity`.
2. Increment the database version in `AppDatabase`.
3. Add a Room migration.
4. Update DAO queries if needed.
5. Update UI binding in `ServerAdapter`.
6. Run unit and instrumented tests.

### Add a New Check Strategy

Recommended path:

1. Add a dedicated class in `network/`.
2. Return `PingResult` with a clear `source`.
3. Call it from `AutoPinger`.
4. Keep timeouts bounded.
5. Add `PingMonDebug` logs for start, success, failure, and timeout.
6. Keep database updates inside repository or worker/ViewModel orchestration.

### Notification Changes

When changing notifications:

- Keep Android 13+ runtime permission in mind.
- Keep notification channel behavior in mind.
- Avoid repeated notifications for the same incident.
- Log every skip reason, such as missing permission or disabled channel.

### Recommended Validation

Before handing off changes:

```powershell
.\gradlew.bat :app:assembleDebug --rerun-tasks
.\gradlew.bat :app:testDebugUnitTest --rerun-tasks
```

For device-level validation:

```powershell
.\gradlew.bat :app:installDebug
adb logcat -s PingMonDebug
```

## License

No license file is currently included. Add a license before publishing or distributing this project outside local development.
