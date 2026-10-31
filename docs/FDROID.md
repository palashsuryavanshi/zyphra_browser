# F-Droid submission

Everything in this file was measured on this repository at commit
`5b4d2ccb8275cc15c7c675a53fd7e3208d4fccc2` (branch `beta`). Where something was
*not* verified, it says so.

## Why there is a separate build

F-Droid's inclusion policy states that Google Play Services "are strictly
forbidden in all applications", and that upstream developers must ship "a
build flavour that does not require these dependencies when such features
become necessary".

GeckoView 147 leaves no room for this. Its POM declares
`com.google.android.gms:play-services-fido:21.2.0` as a **non-optional runtime
dependency**, so every GeckoView app inherits it. Excluding it is the only
route to compliance.

Measured, not assumed:

| | Default build | F-Droid build |
|---|---|---|
| `com.google.android.gms` classes in the dex | **18** | **0** |
| GMS components in the APK's own manifest | present | none |
| arm64-v8a APK size | 84.72 MB | 84.48 MB |

## What it costs, stated plainly

GeckoView implements WebAuthn on top of the GMS FIDO2 privileged API. R8
reports the hard references from `WebAuthnTokenManager`, `WebAuthnUtils` and
`WebAuthnTokenManager.getAssertion`, so **passkey / WebAuthn sign-in does not
work in the F-Droid build.**

This is a real functional loss, not a theoretical one. What was verified:

- The F-Droid build **launches** and Gecko reaches `MOZGLUE_READY` with no
  `NoClassDefFoundError`, `ClassNotFoundException` or crash in logcat.
- It **loads and renders HTTPS pages** correctly (checked on a Pixel 9a,
  Android 17).
- The GMS classes are **absent from all five dex files** (`dexdump` class
  descriptor count = 0), so any passkey code path cannot resolve them.

What was **not** verified: no real passkey sign-in was driven end-to-end, so
whether that path fails gracefully or throws is unknown. Treat it as untested.

To avoid claiming a capability the build does not have, the Passkeys screen now
reads `BuildConfig.FDROID_BUILD` and states that passkeys are unavailable,
instead of implying they work.

Note this costs **no Anti-Feature**. `GooglePlayServices` is not one of
F-Droid's Anti-Features — the list is Ads, DisabledAlgorithm, KnownVuln,
NonFreeAdd, NonFreeAssets, NonFreeDep, NonFreeNet, NoSourceSince,
TetheredNet, Tracking. None of those apply here.

## The build flags

Nothing about the published release changed. The F-Droid build is opt-in:

```
./gradlew assembleRelease -PfdroidBuild=true
```

Per-ABI, which is how F-Droid builds it:

```
./gradlew assembleRelease -PfdroidBuild=true -PfdroidAbi=arm64-v8a \
                          -PcurrentVersionCode=101 \
                          "-PfdroidVersionName=1.0-fdroid"
```

| Flag | Meaning |
|---|---|
| `fdroidBuild` | Excludes `com.google.android.gms` and Firebase |
| `fdroidAbi` | Builds exactly one ABI and suppresses the universal APK |
| `currentVersionCode` | Version code F-Droid assigned to this ABI |
| `fdroidVersionName` | Version name, suffixed so F-Droid's build is distinguishable |

Quote the version name on Windows. `gradlew.bat` mis-splits an unquoted
dotted value: `-PfdroidVersionName=1.0-fdroid` becomes
`-PfdroidVersionName=1` plus a bogus `.0-fdroid` task.

## Verified on this checkout

| Check | Result |
|---|---|
| `assembleDebug` (default) | builds |
| `assembleDebug -PfdroidBuild=true` | builds |
| `assembleRelease` (default, all 4 APKs) | builds |
| `assembleRelease` per-ABI, GMS-free, all 3 ABIs | builds |
| `assembleRelease` with **no keystore present** | builds, emits `-unsigned.apk` |
| `lintDebug` | 0 errors |
| Gradle cache collision between the two variants | none — building default after F-Droid regenerates the GMS-bearing APK |

The no-keystore case matters: F-Droid's builder has no signing key, so the
release variant must build unsigned rather than fail. It does, and F-Droid
signs the result itself.

## Metadata to submit to fdroiddata

Draft, not yet filed. `fastlane/metadata/android/en-US/` is already in the repo
with the title, short description, full description and changelogs.

```yaml
Categories:
  - Browser
  - Security

License: MPL-2.0
SourceCode: https://github.com/palashsuryavanshi/ZyphraAndroid
Changelog: https://github.com/palashsuryavanshi/ZyphraAndroid/releases
IssueTracker: https://github.com/palashsuryavanshi/ZyphraAndroid/issues

RepoType: git
Repo: https://github.com/palashsuryavanshi/ZyphraAndroid.git

# No AntiFeatures. Play Services are excluded, so none of the ten apply.
# No AllowedAPKSigningKeys: F-Droid signs with its own key.

Builds:
  - versionName: 1.0-fdroid
    versionCode: 101
    commit: '<tag v1.0 commit>'
    gradle: yes
    gradleprops: fdroidBuild=true,fdroidAbi=armeabi-v7a,currentVersionCode=101,fdroidVersionName=1.0-fdroid
    output: app/build/outputs/apk/release/*-unsigned.apk
  - versionName: 1.0-fdroid
    versionCode: 102
    commit: '<tag v1.0 commit>'
    gradle: yes
    gradleprops: fdroidBuild=true,fdroidAbi=arm64-v8a,currentVersionCode=102,fdroidVersionName=1.0-fdroid
    output: app/build/outputs/apk/release/*-unsigned.apk
  - versionName: 1.0-fdroid
    versionCode: 103
    commit: '<tag v1.0 commit>'
    gradle: yes
    gradleprops: fdroidBuild=true,fdroidAbi=x86_64,currentVersionCode=103,fdroidVersionName=1.0-fdroid
    output: app/build/outputs/apk/release/*-unsigned.apk
```

Three explicit build blocks rather than a `VercodeOperation` list. F-Droid
generates extra blocks from a `VercodeOperation` by *copying* the block it is
given, so every generated block would inherit one ABI — which cannot work when
each architecture needs different `gradleprops`. The cost is that a new release
needs three blocks written by hand.

`changelogs/` carries the release changelog under the base version code; the
per-ABI codes (101–103) are assigned at F-Droid build time and F-Droid falls
back sensibly when no exact per-ABI changelog exists.

## Other changes made for F-Droid

- **`buildToolsVersion` pin removed.** It was hard-pinned to `36.1.0`, which
  fails outright on any machine without that exact revision — the normal state
  of F-Droid's build server. Unpinned, AGP picks the newest installed version
  meeting its own minimum.
- Release builds unsigned when no `keystore.properties` exists. Already true;
  now commented as load-bearing for F-Droid.
- Store metadata added under `fastlane/metadata/android/en-US/`.

## Dependency audit

Every runtime dependency, and its licence:

| Dependency | Licence |
|---|---|
| `org.mozilla.geckoview:geckoview` + `geckoview-exoplayer2` | MPL-2.0 |
| `androidx.*` (core, appcompat, lifecycle, recyclerview, collection, annotation) | Apache-2.0 |
| `com.google.android.material:material` | Apache-2.0 |
| `org.jetbrains.kotlin:kotlin-stdlib` | Apache-2.0 |
| `org.yaml:snakeyaml` | Apache-2.0 |
| `com.google.android.gms:play-services-fido` | **proprietary — excluded in the F-Droid build** |

The only non-FOSS dependency is the one that is removed.

## Open items that need a human decision

These are not code problems and were not resolved here.

1. **Trademark and branding.**
   The inclusion policy says apps "must not infringe third party rights,
   including … trademarks". The app ships under its own name and icon (Zyphra
   Browser) and states that it is not affiliated with Mozilla.

2. **GeckoView comes from `maven.mozilla.org`, which is not a trusted
   repository.** The policy trusts Maven Central, Google Maven, OSS Sonatype,
   OSS JFrog, JitPack and Clojars. `org.mozilla.geckoview:geckoview` is **not**
   on Maven Central — checked directly — so the repository in `settings.gradle`
   cannot simply be deleted. Expect a maintainer question; the defence is that
   GeckoView is Mozilla's official FOSS release artifact under MPL-2.0, which
   is a stronger provenance claim than "it was on a trusted host".

3. **Application ID is not derived from a domain.** The policy advises that
   "the Application ID … stems from a domain name owned by the developer".
   `com.zyphra.browser` does not. Advisory rather than blocking, and
   changing it now would orphan existing installs.

4. **No screenshots in `fastlane/metadata/`.** F-Droid treats them as
   optional. There are suitable images in `assets/` and `logo/` to use if
   wanted.

5. **Version name.** The app's own label is `Zyphra Browser`, matching the
   F-Droid title. Settled by the Zyphra rebrand.

## Reproducing the checks

```bash
# builds, and the APK declares no GMS component
./gradlew assembleRelease -PfdroidBuild=true -PfdroidAbi=arm64-v8a

# the class count that matters
unzip -o app/build/outputs/apk/release/app-arm64-v8a-release.apk classes*.dex
dexdump -f classes.dex | grep -c 'Class descriptor.*Lcom/google/android/gms'
# -> 0
```