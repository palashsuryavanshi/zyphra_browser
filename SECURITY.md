# Security Policy

## Zyphra Browser

For the complete technical basis of this policy, see [docs/PRIVACY.md](docs/PRIVACY.md).

## Reporting a Vulnerability

**Please do not report security vulnerabilities in public GitHub issues.**

To report a security vulnerability, use GitHub's Private Security Advisory feature:
https://github.com/palashsuryavanshi/zyphra_browser/security/advisories/new

Or create a confidential draft advisory through the repository's Security tab.

## What to Include

- Affected component(s) and file paths
- Steps to reproduce the vulnerability
- Potential impact and severity
- Suggested fix (if available)
- Whether the issue affects the F-Droid build, GitHub release, or both

## Supported Versions

| Version | Supported |
|---------|-----------|
| 1.0 (current) | Yes |
| Pre-1.0 | No |

## Security Architecture

### Data Protection
- `allowBackup="false"` - no Android backup of app data
- `android:dataExtractionRules` and `android:fullBackupContent` restrict all backup
- Password vault encrypted with AES-256-GCM using Android Keystore (non-exportable key)
- Content on disk is plaintext for non-vault data (standard for Firefox-like browsers)

### Network Security
- `network_security_config.xml` blocks cleartext traffic as defense in depth
- HTTPS-only mode enforced by app logic where configured
- Certificate validation never bypassed (no "click through" or exceptions)
- No custom certificate trust stores

### Web-to-Native Security
- `intent:` URLs stripped of dangerous components before launch (component, package,
  Selector) and all FLAG_GRANT_* bits dropped
- `content://`, `file://`, `android_app://` schemes refused from intent URLs
- Download filenames sanitized against path traversal (DownloadSafety.sanitize)
- Executables and double-extension downloads require explicit user confirmation
- Downloaded files are never auto-executed

### Permissions
- Camera, microphone, location denied by default
- Per-site permission decisions stored separately
- Android runtime permissions separate from browser-level decisions
- Exported components minimized (MainActivity and ProfileInstallReceiver only)
- ProfileInstallReceiver guarded by `android.permission.DUMP`

### Build Security
- No signing credentials in repository (.jks, keystore.properties gitignored)
- Release builds use v2+v3 APK signature (v1 disabled - closes Janus vulnerability)
- No developer-specific absolute paths in build
- Build works without any local secrets
- CI produces unsigned artifacts (no key exposure)

## Scope

### In Scope
- Android app code (Kotlin)
- Build configuration (Gradle)
- Manifest and permissions
- Network security configuration
- Data storage and encryption
- Web-to-native boundary

### Out of Scope
- GeckoView engine internals (Mozilla maintains - report to Mozilla separately)
- Add-ons installed from AMO (third-party code)
- Issues in dependencies (report to respective projects)
- Physical device attacks requiring root access against encrypted vault

## Supported Android Versions

Minimum SDK: Android 8.0 (API 26)
targetSdk: Android 14 (API 34)

## Privacy Commitments

This project makes these verifiable claims:
- No analytics, tracking, or crash reporting
- No accounts or sync
- No telemetry or usage pings
- No advertising
- No automatic updater (updates via distribution channel only)
- All data stored locally on device only
- No Bluetooth permissions
- No advertising ID access
- No location access without runtime permission

All claims are traceable to source code. See [docs/PRIVACY.md](docs/PRIVACY.md) for the
technical basis of each claim.