#!/bin/bash

mkdir -p .git-hooks
for hook in .git-hooks/*; do
    [ -f "$hook" ] && ln -sf "../../${hook}" ".git/hooks/$(basename "$hook")"
done