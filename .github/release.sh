#!/usr/bin/env bash
set -euo pipefail

VERSION_INPUT=${1:-}

if [[ -z "$VERSION_INPUT" ]]; then
	echo "Usage: ./scripts/release.sh <version>"
	echo "Example: ./scripts/release.sh 1.0.0-alpha5"
	echo "Example: ./scripts/release.sh v1.0.0-alpha5"
	exit 1
fi

if [[ "$VERSION_INPUT" == vv* ]]; then
	echo "Error: invalid version '$VERSION_INPUT'"
	echo "Example: ./scripts/release.sh v1.2.3"
	exit 1
fi

VERSION="${VERSION_INPUT#v}"

SEMVER_RE='^(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)(-[0-9A-Za-z-]+(\.[0-9A-Za-z-]+)*)?(\+[0-9A-Za-z-]+(\.[0-9A-Za-z-]+)*)?$'
if [[ ! "$VERSION" =~ $SEMVER_RE ]]; then
	echo "Error: invalid version '$VERSION_INPUT'"
	echo "Example: ./scripts/release.sh 1.2.3"
	exit 1
fi

TAG="v$VERSION"

if [[ -n "$(git status --porcelain)" ]]; then
	echo "Error: working tree not clean"
	exit 1
fi

BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [[ "$BRANCH" != "main" ]]; then
	echo "Error: not on main branch (currently on $BRANCH)"
	exit 1
fi

git pull --ff-only

if ! command -v git-cliff &>/dev/null; then
	echo "Error: git-cliff not installed"
	exit 1
fi

if [[ ! -f ".github/cliff.toml" ]]; then
	echo "Error: .github/cliff.toml not found"
	exit 1
fi

PREV_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "none")
COMMIT_COUNT=$(git rev-list "${PREV_TAG}..HEAD" --count 2>/dev/null || git rev-list HEAD --count)

echo "Releasing ${PREV_TAG} → ${TAG} (${COMMIT_COUNT} commits)"
echo ""
git cliff --config .github/cliff.toml --tag "$TAG" --unreleased
echo ""

# Check fastlane changelog exists
IFS='.' read -r MAJOR MINOR PATCH _ <<< "$VERSION"
VERSION_CODE=$((MAJOR * 10000 + MINOR * 1000 + PATCH))
CHANGELOG_FILE="fastlane/metadata/android/en-US/changelogs/${VERSION_CODE}.txt"

if [[ ! -f "$CHANGELOG_FILE" ]]; then
	echo "Warning: missing fastlane changelog: $CHANGELOG_FILE"
	read -rp "Continue anyway? [y/N] " skip_changelog
	if [[ ! "$skip_changelog" =~ ^[Yy]$ ]]; then
		echo "Aborted - create changelog first"
		exit 1
	fi
fi

read -rp "Push and release? [y/N] " confirm
if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
	echo "Aborted"
	exit 1
fi

# Update version properties for F-Droid
sed -i "s/^version\.code=.*/version.code=${VERSION_CODE}/" gradle.properties
sed -i "s/^version\.name=.*/version.name=${VERSION}/" gradle.properties

git add gradle.properties
git commit -m "chore(release): bump version to ${VERSION}"

git tag -a "$TAG" -m "Release $TAG"
git push origin main "$TAG"

echo ""
echo "Released $TAG"
