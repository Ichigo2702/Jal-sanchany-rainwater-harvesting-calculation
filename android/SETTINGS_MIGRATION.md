# Settings Migration Strategy

Room owns settings schema changes through `JalSanchayDatabase` migrations.

- Keep `user_setup.id = 1` as the single settings row.
- Add new settings fields as nullable columns or non-null columns with defaults.
- Bump the database version for every schema change.
- Add a named `Migration(old, new)` that preserves existing settings and rainfall entries.
- Recalculate rainfall entries after settings changes through `TrackerRepository.recalculateAllEntries()`.
- Remove `fallbackToDestructiveMigration()` before production release.
