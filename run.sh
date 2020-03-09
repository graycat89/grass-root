#! /bin/bash

filename=$1

source ./.env-local

./gradlew clean build run --args="$filename"