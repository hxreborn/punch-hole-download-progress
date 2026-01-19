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

echo "Updating CHANGELOG.md for $TAG..."
git cliff --config .github/cliff.toml --tag "$TAG" -o CHANGELOG.md

git add CHANGELOG.md
git commit -m "docs(changelog): update for $TAG"

git tag -a "$TAG" -m "Release $TAG"

git push origin main
git push origin "$TAG"

echo "Released $TAG"
