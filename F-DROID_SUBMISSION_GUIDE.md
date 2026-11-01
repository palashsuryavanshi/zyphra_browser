# F-Droid Submission Guide
# Zyphra Browser

## Prerequisites

- Forked official fdroiddata repository: https://gitlab.com/fdroid/fdroiddata
- fdroidserver installed (for metadata validation)
- Git configured with your GitLab account

## Step 1: Prepare the Application Repository

1. Ensure all changes are committed to the `fdroid` branch:
   ```
   git checkout fdroid
   git status  # should be clean
   ```

2. Verify the latest commit hash for the metadata file:
   ```
   git rev-parse HEAD
   ```

3. Create a Git release tag for the F-Droid version:
   ```
   git tag v1.0-fdroid-<commit-short-hash>
   git push origin v1.0-fdroid-<commit-short-hash>
   ```
   (Use the actual release tag format preferred by F-Droid, e.g., v1.0)

## Step 2: Prepare the F-Droid Metadata File

The metadata file (`fdroid/com.zyphra.browser.yml`) needs updating with the actual
commit hash:

Replace `<tag v1.0 commit>` with the actual commit hash of the release tag.

For example, if the tag v1.0 points to commit `abcdef1`:
```yaml
Builds:
  - versionName: 1.0-fdroid
    versionCode: 101
    commit: abcdef1
    timeout: 5400
    gradle: yes
    gradleprops: fdroidBuild=true,fdroidAbi=armeabi-v7a,currentVersionCode=101,fdroidVersionName=1.0-fdroid
    output: app/build/outputs/apk/release/app-armeabi-v7a-release-unsigned.apk
```

## Step 3: Add Fastlane Metadata to Application Repository

Copy screenshots into the Fastlane metadata structure:

```
fastlane/metadata/android/en-US/images/
  icon.png           <- from logo/app-icon.png
  phoneScreenshots/
    1.png            <- from assets/screenshot-start-page.png
    2.png            <- from assets/screenshot-settings.png
    3.png            <- from assets/screenshot-privacy-settings.png
```

## Step 4: Validate with fdroidserver

Run from the fdroiddata repository directory:

```bash
# Read and validate metadata
fdroid readmeta

# Lint the metadata
fdroid lint com.zyphra.browser

# Check for updates
fdroid checkupdates com.zyphra.browser

# Build (requires available source)
fdroid build com.zyphra.browser
```

## Step 5: Fork fdroiddata and Create Merge Request

1. Fork the official fdroiddata repository:
   https://gitlab.com/fdroid/fdroiddata

2. Clone your fork:
   ```
   git clone https://gitlab.com/<your-username>/fdroiddata.git
   cd fdroiddata
   ```

3. Copy the metadata file:
   ```
   cp /path/to/zyphra_browser/fdroid/com.zyphra.browser.yml \
      metadata/com.zyphra.browser.yml
   ```

4. Commit and push:
   ```
   git add metadata/com.zyphra.browser.yml
   git commit -m "Add com.zyphra.browser: F-Droid build metadata"
   git push origin main
   ```

5. Open a Merge Request on GitLab at:
   https://gitlab.com/fdroid/fdroiddata/-/merge_requests/new

## Step 6: Prepare Merge Request Description

Include:
- Link to source repository: https://github.com/palashsuryavanshi/zyphra_browser
- Explanation of Maven repository concern (GeckoView not on Maven Central)
- Explanation of F-Droid build flavour (Google Play Services excluded)
- Note about trademark/branding independence
- Summary of compliance checks performed (build, lint, test all pass)
- Source of screenshots and icon

## Step 7: Respond to Maintainer Feedback

Common questions from F-Droid maintainers:
1. **"Mozilla Maven repository is not trusted"** - Explain that GeckoView is
   Mozilla's official FOSS release and cannot be found on Maven Central. Link to
   Mozilla's Maven repository policy.
2. **"No screenshots provided"** - Add screenshots to Fastlane metadata if not
   already done.
3. **"Application ID not domain-derived"** - Explain that it's an independent
   project, application ID follows current conventions. Offer to discuss if needed.
4. **"Trademark concern"** - Emphasize independent community project. README
   clearly states no Mozilla affiliation.

## Step 8: Track Review

1. Monitor the merge request for maintainer comments
2. Respond promptly to feedback
3. Make required changes and push updates to the MR branch
4. After merge, the app will be built and published on F-Droid

## Step 9: Post-Publishing

Once published on F-Droid:
- Update the app's README to mention F-Droid availability
- Update the fastlane metadata to include F-Droid metadata badge
- Monitor F-Droid build results for issues
- Ensure new releases include updated commit hashes in the metadata file

## Important Notes

- Do NOT submit without the repository owner's explicit approval
- Do NOT modify fdroiddata without a fork/merge request workflow
- Do NOT claim F-Droid availability before publication is confirmed
- Keep the `fdroid` branch updated with the latest release commits

## Files Prepared

| File | Location | Purpose |
|------|----------|---------|
| F-Droid metadata YAML | fdroid/com.zyphra.browser.yml | fdroiddata metadata |
| Fastlane title | fastlane/.../title.txt | App listing title |
| Fastlane short description | fastlane/.../short_description.txt | 80-char summary |
| Fastlane full description | fastlane/.../full_description.txt | Catalog description |
| Changelog | fastlane/.../changelogs/1.txt | Version changelog |
| F-Droid audit report | F-DROID_AUDIT_REPORT.md | Compliance audit |
| Validation report | F-DROID_VALIDATION_REPORT.md | Build/test results |
| Third-party licences | THIRD_PARTY_LICENSES.md | Licence inventory |
| Privacy policy | PRIVACY_POLICY.md | Privacy terms |
| Security policy | SECURITY.md | Security disclosure |
| Contributing guide | CONTRIBUTING.md | Development guide |