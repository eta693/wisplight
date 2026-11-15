#!/usr/bin/env bash
# Attend le rapport CI du commit courant (branche ci-report) puis affiche le statut.
set -u
SHA=$(git -C "$(dirname "$0")/.." rev-parse HEAD)
cd "$(dirname "$0")/.."
for i in $(seq 1 60); do
  git fetch -q --depth 1 origin +ci-report:refs/remotes/origin/ci-report 2>/dev/null
  if git show origin/ci-report:status.txt 2>/dev/null | grep -q "sha=$SHA"; then
    git show origin/ci-report:status.txt
    exit 0
  fi
  sleep 20
done
echo "timeout"
exit 1
