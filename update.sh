#!/usr/bin/env bash

current=$(git branch | awk  '$1 == "*"{print $2}')

git pull && git submodule init && git submodule update && git submodule foreach git checkout "$current" && git submodule foreach git pull origin "$current"