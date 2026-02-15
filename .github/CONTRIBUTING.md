# Contributing

See [Build from source](../README.md#build) for setup.

KtLint enforces `.editorconfig` rules on every build. Fix issues with:

```bash
./gradlew ktlintFormat
```

## Commits

Follow conventional commits: `type(scope): description`

Types: `feat`, `fix`, `refactor`, `docs`, `perf`, `test`, `build`, `chore`

Scopes: `ui`, `hook`, `prefs`, `view`, `ci`

Examples:

```
feat(ui): add vertical text toggle for landscape
fix(hook): guard notification listener on API 28
```
