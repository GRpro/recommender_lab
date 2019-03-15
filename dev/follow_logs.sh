#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# add service names separated by whitespace to follow logs of a particular service
docker-compose -f "$DIR/docker-compose-dev.yaml" logs -f $@