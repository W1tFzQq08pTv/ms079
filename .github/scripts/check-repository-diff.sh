#!/usr/bin/env bash

set -euo pipefail

base_sha="${1:?base SHA is required}"
head_sha="${2:?head SHA is required}"
max_file_bytes=$((20 * 1024 * 1024))

git cat-file -e "${base_sha}^{commit}"
git cat-file -e "${head_sha}^{commit}"
git diff --check "${base_sha}...${head_sha}"

failures=()
while IFS= read -r -d '' path; do
  case "${path}" in
    target/*|out/*|build/*|logs/*|*.log|*.orig|*.zip|*.tar.gz)
      failures+=("Forbidden generated or archive file: ${path}")
      ;;
  esac

  size="$(git cat-file -s "${head_sha}:${path}")"
  if (( size > max_file_bytes )); then
    failures+=("New file exceeds 20 MiB (${size} bytes): ${path}")
  fi
done < <(git diff --diff-filter=A --name-only -z "${base_sha}...${head_sha}")

if (( ${#failures[@]} > 0 )); then
  printf '%s\n' "${failures[@]}" >&2
  exit 1
fi

echo "Repository diff hygiene checks passed."
