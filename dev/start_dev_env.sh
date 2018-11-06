#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

mkdir -p $DIR/img/tmp
cp $DIR/../batch_jobs/target/scala-2.11/batch_jobs-assembly-0.1.jar $DIR/img/tmp/batch_jobs-assembly-0.1.jar
cp $DIR/../job_runner/target/scala-2.11/job_runner-assembly-0.1.jar $DIR/img/tmp/job_runner-assembly-0.1.jar

# TODO remove --build and build images before with included jars
docker-compose -f "$DIR/docker-compose-dev.yaml" up --build $@
