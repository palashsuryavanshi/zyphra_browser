# Privacy Policy

## Zyphra Browser

**Last Updated:** November 1, 2026  
**Version:** 1.0

---

## Overview

Zyphra Browser is a privacy-focused web browser for Android. This privacy policy
explains what data the application handles and how.

## Summary

- **No data collection.** The app collects no personal information, device
  identifiers, or usage data.
- **No third-party services.** No analytics, crash reporting, advertising, or
  telemetry services are integrated.
- **No accounts.** There is no account system and no sync mechanism.
- **No automatic updates.** Updates are managed by the distribution channel
  (e.g., F-Droid, GitHub).
- **Local-only storage.** All data is stored on the device.

## Data Stored on Device

The following data is stored locally in the app's private data directory:

| Data | Storage | Encrypted | Backed Up |
|------|---------|-----------|-----------|
| Browsing history (URL, title, time) | shared_prefs/zyphra_history.xml | No | No |
| Bookmarks | shared_prefs/zyphra_bookmarks.xml | No | No |
| Site permissions | shared_prefs/site_permissions.xml | No | No |
| Saved sessions | shared_prefs/browser_sessions.xml | No | No |
| Download list | shared_prefs/zyphra_downloads.xml | No | No |
| Settings | shared_prefs/zyphra_privacy.xml | No | No |
| Password vault | shared_prefs/zyphra_vault.xml | Yes (AES-256-GCM, Android Keystore) | No |
| Cookies, cache, localStorage | GeckoView profile directory | No | No |
| Camera captures | cache/uploads/ | No | No |
| Crash minidumps | Gecko profile minidumps/ | No | No |

**Backup disabled.** Android backup and device-to-device transfer are switched off
for all data domains.

## Network Activity

The app makes network requests only for:
- **Page loading** - loading URLs you navigate to
- **DNS-over-HTTPS** (opt-in only) - sends DNS queries to your chosen resolver
- **Add-on installation** (manual only) - downloads from addons.mozilla.org
- **Safe Browsing** (optional) - Google Safe Browsing via GeckoView (user-disableable)

No telemetry, no analytics, no config fetches, no crash uploads, and no usage pings
are sent.

## Internet Communication

The app uses the following encrypted DNS (DoH) providers (all opt-in):
- Quad9 (dns10.quad9.net, dns.quad9.net)
- Wikimedia (wikimedia-dns.org)
- DNS4All (doh.dns4all.eu)
- Mullvad (dns.mullvad.net)

## Permissions

| Permission | Purpose | Required |
|-----------|---------|----------|
| INTERNET | Loading web pages | Essential |
| ACCESS_NETWORK_STATE | Detecting connection status | Essential |
| CAMERA | Sites requesting camera (user-initiated) | Runtime |
| RECORD_AUDIO | Sites requesting microphone (user-initiated) | Runtime |
| ACCESS_COARSE/FINE_LOCATION | Sites requesting location (user-initiated) | Runtime |
| POST_NOTIFICATIONS | Download and media notifications | Runtime |
| FOREGROUND_SERVICE* | Background downloads/media | Essential |

No Bluetooth permissions. No advertising ID access. No location access at launch.

## Third-Party Services

The following are **integrated by GeckoView** (the browser engine) and are outside
this app's direct control:

- **Google Safe Browsing** - Phishing/malware protection (user-disableable, no
  account required)
- **Google Play Services** - Included only in non-F-Droid builds for WebAuthn/passkey
support. The F-Droid build excludes Play Services.

This app itself does NOT integrate any proprietary analytics, advertising, or
crash reporting SDK.

## Private Browsing

Private tabs use GeckoView's real private mode:
- No history, cookies, cache, or session data persisted
- Private storage held in memory only
- `FLAG_SECURE` prevents screenshots and screen recording
- Web notifications refused in private mode
- Media metadata withheld from system
- Files downloaded in private mode still persist to storage (user is warned)

## Data Deletion

All browsing data can be cleared from Settings:
- Cookies and site sessions
- Cached images and files
- Website storage and permissions
- Browsing history (with date-range options)
- Bookmarks
- Saved passwords
- Downloads

Password vault uses Android Keystore-protected AES-256-GCM encryption. The key is
non-exportable and is not backed up.

## Children's Privacy

The app does not knowingly collect any data from anyone, including children under
13. The app has no account system and collects no personal information.

## Open Source

This application is open source, licensed under MPL-2.0. The complete source code
is available at https://github.com/palashsuryavanshi/zyphra_browser.

## Contact

For privacy-related questions, open an issue on the GitHub repository:
https://github.com/palashsuryavanshi/zyphra_browser/issues

---

*This privacy policy reflects the actual data handling practices of the app.
No marketing claims are made about capabilities beyond what has been verified.*