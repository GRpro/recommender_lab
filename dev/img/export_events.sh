#!/usr/bin/env bash

/usr/local/spark/bin/spark-submit \
--class lab.reco.batch.ImportEventsJob \
--master spark://spark-master:7071 \
--executor-memory 1G \
--total-executor-cores 2 \
/usr/local/batch_jobs-assembly-0.1.jar \
-eit event/indicator \
-ep 9200 \
-eu elasticsearch \
-eun elastic \
-eup elastic \
-o hdfs://hdfs:9000/events
