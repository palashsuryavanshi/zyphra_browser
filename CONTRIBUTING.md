# Contributing

## Zyphra Browser

Thank you for your interest in contributing to Zyphra Browser.

## Getting Started

### Prerequisites

- JDK 17 or newer (Android Studio's bundled JBR works)
- Android SDK with API 26+ platform
- Android Gradle Plugin 9.4.1
- Git

### Setup

1. Clone the repository:
   ```
   git clone https://github.com/palashsuryavanshi/zyphra_browser.git
   cd zyphra_browser
   ```

2. Open the project in Android Studio (File -> Open) and let Gradle sync finish.

3. Create `local.properties` with your SDK path if not auto-created by Studio.

### Building

```bash
./gradlew assembleDebug       # Debug build
./gradlew assembleRelease     # Release build (unsigned)
./gradlew testDebugUnitTest   # Unit tests
./gradlew lintDebug           # Android Lint
```

On Windows, use `gradlew.bat`.

### F-Droid Build

For F-Droid-aligned builds:
```bash
./gradlew assembleRelease -PfdroidBuild=true \
  -PfdroidAbi=arm64-v8a \
  -PcurrentVersionCode=102 \
  -PfdroidVersionName=1.0-fdroid
```

### Toolchain Details

See [README.md](README.md) for full build instructions, SDK 37 caveat, R8 notes,
and signing configuration.

## Development Guidelines

### Code Style
- Kotlin code follows `kotlin.code.style=official` (set in gradle.properties)
- No Kotlin Gradle plugin needed (AGP 9 has Kotlin support built in)
- Single module (`:app`), single package (`com.zyphra.browser`)

### Privacy Principles
- No telemetry, analytics, or crash reporting SDK may be added
- No advertising SDK may be added
- No automatic update mechanism
- HTTPS enforcement should remain in place
- No network requests outside of page loading and clearly user-initiated actions

### Adding a Feature
1. Open an issue to discuss the feature before implementing
2. Keep the declarative approach: settings rows are data in SettingsScreens.kt,
   not control flow
3. Add a setting only when a real engine API or shell code backs it
4. Update TESTING.md with what was verified

### Adding a Setting
- Add the string to strings.xml
- Add the row in SettingsScreens.kt as data
- Update the documentation in docs/SETTINGS-PLAN.md if adding a new capability

## Testing Requirements

- Run `./gradlew testDebugUnitTest` before submitting
- Run `./gradlew lintDebug` before submitting
- Add unit tests for new pure decision logic
- Test on a physical device for UI/behavior changes
- Update TESTING.md with verified items

## Pull Request Process

1. Fork the repository
2. Create a feature branch (`feature/my-changes`)
3. Make changes with clear, atomic commits
4. Add or update tests as appropriate
5. Update documentation as needed
6. Ensure all tests pass and lint is clean
7. Open a PR with a clear description of changes
8. Respond to review feedback

## Commit Messages

- Use imperative mood ("Add feature", not "Added feature")
- Keep first line under 72 characters
- Reference issue numbers when applicable

## What NOT to Do

- Do not add Google Play Services or Firebase dependencies
- Do not add analytics, tracking, or telemetry
- Do not add advertising SDKs
- Do not embed API keys, secrets, or signing credentials
- Do not change the application ID without discussion
- Do not bypass certificate validation or HTTPS enforcement
- Do not auto-execute downloaded files

## License

This project is licensed under MPL-2.0. By contributing, you agree that your
contributions will be licensed under the same terms.