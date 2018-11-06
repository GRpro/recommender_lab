#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

docker cp $DIR/../batch_jobs/target/scala-2.11/batch_jobs-assembly-0.1.jar dev_spark-master_1:/usr/local

#docker exec -it dev_spark-master_1 bash -c "/usr/local/spark/bin/spark-submit \
#--class lab.reco.batch.ImportEventsJob \
#--master spark://spark-master:7071 \
#--executor-memory 1G \
#--total-executor-cores 2 \
#batch_jobs-assembly-0.1.jar \
#-eit event/indicator \
#-ep 9200 \
#-eu elasticsearch \
#-eun elastic \
#-eup elastic \
#-o hdfs://hdfs:9000/events
#"