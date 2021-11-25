#!/bin/bash#
docker run -v  $(pwd)/../data:/data valsort "$1"
