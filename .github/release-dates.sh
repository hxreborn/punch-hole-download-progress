#!/usr/bin/env bash
set -euo pipefail

gh api repos/hxreborn/punch-hole-download-progress/releases --paginate \
	--jq '.[] | select(.draft or .prerelease | not) | "\(.tag_name) \(.published_at[:10])"'
