#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

mkdir -p $DIR/img/tmp

cp $DIR/../job_runner/target/scala-2.11/job_runner-assembly-0.1.jar         $DIR/img/tmp/job_runner-assembly-0.1.jar
cp $DIR/../export_job/target/scala-2.11/export_job-assembly-0.1.jar         $DIR/img/tmp/export_job-assembly-0.1.jar
cp $DIR/../similarity_job/target/scala-2.11/similarity_job-assembly-0.1.jar $DIR/img/tmp/similarity_job-assembly-0.1.jar
cp $DIR/../import_job/target/scala-2.11/import_job-assembly-0.1.jar         $DIR/img/tmp/import_job-assembly-0.1.jar
cp $DIR/../event_manager/target/scala-2.11/event_manager-assembly-0.1.jar   $DIR/img/tmp/event_manager-assembly-0.1.jar
cp $DIR/../recommender/target/scala-2.11/recommender-assembly-0.1.jar       $DIR/img/tmp/recommender-assembly-0.1.jar

# TODO remove --build and build images before with included jars
docker-compose -f "$DIR/docker-compose-dev.yaml" up -d --build $@
docker-compose -f "$DIR/docker-compose-dev.yaml" scale spark-slave=2
docker-compose -f "$DIR/docker-compose-dev.yaml" logs -f