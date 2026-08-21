#!/usr/bin/env bash
# Computes the next semantic version from Conventional Commits since the last
# v* tag. Prints "none" (exit 0) when there is nothing to release.
#
# Bump rules (checked in order):
#   feat! / BREAKING CHANGE    -> major (X.0.0)
#   feat(scope)?:              -> minor (X.Y.0)
#   fix/refactor/perf/chore/... -> patch (X.Y.Z)
#
# Requires a git checkout with full history and tags (fetch-depth: 0).
#
# Usage: bash scripts/next-version.sh
set -euo pipefail

latest_tag=$(git describe --tags --abbrev=0 --match 'v*' 2>/dev/null || true)

if [[ -z "$latest_tag" ]]; then
    current="0.1.0"   # no tags yet: first release
    range="HEAD"
else
    current="${latest_tag#v}"
    range="${latest_tag}..HEAD"
fi

messages=$(git log --format=%B "$range" 2>/dev/null || true)
if [[ -z "$messages" ]]; then
    echo "none"
    exit 0
fi

IFS='.' read -r major minor patch <<< "$current"
major=${major:-0}; minor=${minor:-0}; patch=${patch:-0}

if grep -qE '^feat(\([^)]*\))?!|BREAKING[ -]CHANGE' <<< "$messages"; then
    echo "$((major + 1)).0.0"
elif grep -qE '^feat(\([^)]*\))?:' <<< "$messages"; then
    echo "$major.$((minor + 1)).0"
elif grep -qE '^(fix|refactor|perf|chore|docs|style|test|build|ci|revert)(\([^)]*\))?:' <<< "$messages"; then
    echo "$major.$minor.$((patch + 1))"
else
    echo "none"
fi
