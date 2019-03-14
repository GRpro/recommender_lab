#!/usr/bin/env bash

/usr/local/spark/bin/spark-submit \
--class lab.reco.batch.ImportEventsJob \
--master spark://spark-master:7071 \
--executor-memory 512M \
--executor-cores 1 \
--num-executors 4 \
/usr/local/import_job-assembly-0.1.jar \
-eit event/indicator \
-ep 9200 \
-eu elasticsearch \
-eun elastic \
-eup elastic \
-o hdfs://hdfs:9000/events

# TODO, currently this Job doesn't use any executors. Why???