# Implementation Plan - Fix Plural Resources for Internationalization

Convert hardcoded plural strings to Android Plural Resources (`<plurals>`) to correctly handle singular and plural forms in English and other languages.

## Proposed Changes

### [Resource Changes]

#### [MODIFY] [strings.xml](file:///G:/Project/AppLog/app/src/main/res/values/strings.xml)
- Convert identified strings to `<plurals>` resources.
- Provide `one` and `other` quantities for English.

#### [MODIFY] [strings.xml](file:///G:/Project/AppLog/app/src/main/res/values-zh-rCN/strings.xml)
- Convert identified strings to `<plurals>` resources.
- Provide `other` quantity for Chinese (since Chinese usually doesn't distinguish singular/plural in this context, but `plurals` are still preferred for consistency).

### [Code Changes]

#### [MODIFY] [AppsScreen.kt](file:///G:/Project/AppLog/app/src/main/java/top/hzchu/applog/ui/screens/AppsScreen.kt)
- Update usage of `apps_count` to use a helper or `LocalContext` to get quantity string.

#### [MODIFY] [CommitDetailScreen.kt](file:///G:/Project/AppLog/app/src/main/java/top/hzchu/applog/ui/screens/CommitDetailScreen.kt)
- Update usages of `apps_count`, `diff_summary_xxx`, and `diff_xxx` strings.

#### [MODIFY] [HistoryScreen.kt](file:///G:/Project/AppLog/app/src/main/java/top/hzchu/applog/ui/screens/HistoryScreen.kt)
- Update usage of `unpushed_count`.

#### [MODIFY] [PackageChangeReceiver.kt](file:///G:/Project/AppLog/app/src/main/java/top/hzchu/applog/receiver/PackageChangeReceiver.kt)
- Update usage of `notif_text_changes_detected`.

### [Helper Creation]

#### [NEW] [PluralStringResource.kt](file:///G:/Project/AppLog/app/src/main/java/top/hzchu/applog/ui/utils/PluralStringResource.kt)
- Create a `pluralStringResource` Composable helper to simplify usage in Compose.

## Verification Plan

### Manual Verification
- Verify the app builds successfully.
- Check the UI for various counts (0, 1, 2+) to ensure the correct string is displayed.
- Switch language to Chinese and verify it still works as expected.
