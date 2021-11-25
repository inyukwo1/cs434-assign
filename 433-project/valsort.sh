#!/bin/bash
echo Your container args are: "$@"
if [ "$#" -le 2 ]; then
  echo "$#"
  valsort $1
else
  expanded=()
  for var in "$@"
  do
    expanded+=("${var}".sum)
    valsort -o "$var".sum "$var"
  done
  cat "${expanded[@]}" > all.sum
  valsort -s all.sum
fi

