#!/usr/bin/env bash
set -euo pipefail

APK=${1:?usage: check-release-dates.sh <apk>}
if diff <(unzip -p "$APK" assets/release-dates.txt | sort) <("$(dirname "$0")/release-dates.sh" | sort); then
	echo "OK release dates match published GitHub releases"
else
	echo "FAIL release dates differ from published GitHub releases"
	exit 1
fi
