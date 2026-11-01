# Third-Party Licences

## Zyphra Browser

This application is licensed under the Mozilla Public License 2.0.
See [LICENSE](LICENSE) for the full text.

## Engine

### Mozilla GeckoView (org.mozilla.geckoview:geckoview)
- **Version:** 147.0.20260212191108
- **Licence:** Mozilla Public License 2.0 (MPL-2.0)
- **Source:** https://github.com/mozilla/gecko-dev
- **Repository:** https://maven.mozilla.org/maven2/
- **Usage:** Core browser engine (rendering, JS/WASM, WebGL, cookies, storage, DRM)
- **Copyright:** Mozilla Foundation

### Android Jetpack / AndroidX

| Dependency | Version | Licence | Copyright |
|-----------|---------|---------|-----------|
| androidx.core:core-ktx | 1.13.1 | Apache License 2.0 | Google LLC / The Android Open Source Project |
| androidx.appcompat:appcompat | 1.7.0 | Apache License 2.0 | Google LLC / The Android Open Source Project |
| androidx.recyclerview:recyclerview | 1.3.2 | Apache License 2.0 | Google LLC / The Android Open Source Project |
| androidx.swiperefreshlayout:swiperefreshlayout | 1.1.0 | Apache License 2.0 | Google LLC / The Android Open Source Project |
| androidx.fragment:fragment | transitive | Apache License 2.0 | Google LLC / The Android Open Source Project |
| androidx.lifecycle:lifecycle-* | transitive | Apache License 2.0 | Google LLC / The Android Open Source Project |
| androidx.collection:collection | transitive | Apache License 2.0 | Google LLC / The Android Open Source Project |
| androidx.annotation:annotation | transitive | Apache License 2.0 | Google LLC / The Android Open Source Project |
| androidx.profileinstaller:profileinstaller | transitive | Apache License 2.0 | Google LLC / The Android Open Source Project |

### Material Components

| Dependency | Version | Licence | Copyright |
|-----------|---------|---------|-----------|
| com.google.android.material:material | 1.12.0 | Apache License 2.0 | Google LLC / The Android Open Source Project |

### Kotlin Standard Library

| Dependency | Version | Licence | Copyright |
|-----------|---------|---------|-----------|
| org.jetbrains.kotlin:kotlin-stdlib | 2.2.20 | Apache License 2.0 | JetBrains s.r.o. |

### Other Dependencies

| Dependency | Version | Licence | Copyright |
|-----------|---------|---------|-----------|
| org.yaml:snakeyaml | transitive | Apache License 2.0 | Andrey Somov |
| org.json:json | 20231013 (test only) | Custom JSON License (2-clause BSD-like) | JSON.org |
| junit:junit | 4.13.2 (test only) | Eclipse Public License 2.0 | JUnit team |

### Firefox Icons

The engine's bundled icons and UI resources are part of GeckoView and are licensed under MPL-2.0 as part of the Gecko source code.

## Excluded (F-Droid Build)

| Dependency | Reason |
|-----------|--------|
| com.google.android.gms:play-services-fido (21.2.0) | Proprietary Google library, excluded from F-Droid build to comply with inclusion policy. Included in default/GitHub release builds for WebAuthn support. |
| com.google.firebase:* (various) | Proprietary Google libraries, excluded from F-Droid build. |

## Add-ons Available for User Installation (Mozilla AMO)

The following add-ons are offered in-app but are installed by the user from Mozilla AMO.
They are NOT bundled with this application.

All AMO add-ons are subject to their own respective licences
(typically MPL-2.0, GPL v3 or BSD as specified by their authors).

| Add-on | Slug | Licence |
|--------|------|--------|
| uBlock Origin | ublock-origin | GPL-3.0+ |
| ClearURLs | clearurls | LGPL-3.0 |
| Ghostery | ghostery | MPL-2.0 |
| Cookie AutoDelete | cookie-autodelete | MIT |
| LeechBlock NG | leechblock-ng | GPL-3.0 |
| Dark Reader | darkreader | MIT |
| NoScript Security Suite | noscript | GPL-3.0+ |
| SponsorBlock | sponsorblock | GPL-3.0 |
| Video Background Play Fix | video-background-play-fix | MIT |
| SingleFile | single-file | AGPL-3.0 |
| Search by Image | search-by-image | MIT |
| Google Search Fixer | google-search-fixer | MIT |
| TWP - Translate For Mobile | twp-translate-for-mobile | GPL-3.0 |
| Tampermonkey | tampermonkey | Proprietary (free for personal use) |

## Notes for F-Droid

1. The only non-FOSS dependency integrated into the application is Google Play
   Services (via GeckoView), which is **excluded in the F-Droid build**.
2. The application does not bundle any third-party libraries beyond AndroidX,
   Material Components, Kotlin stdlib, and Mozilla GeckoView.
3. All Apache-2.0 and MPL-2.0 licensed dependencies are fully compliant with
   F-Droid's inclusion policy.
4. Tampermonkey is a user-installed add-on; it is not bundled and its licence
   status is the responsibility of the user who chooses to install it.