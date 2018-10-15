#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [ ! -z $1 ] && [ $1 = "clean" ]; then
    # clean volume data from containers
    docker-compose -f "$DIR/docker-compose-dev.yaml" down --volumes
else
    docker-compose -f "$DIR/docker-compose-dev.yaml" down
fi
