#!/usr/bin/env bash

/usr/local/spark/bin/spark-submit \
--class lab.reco.batch.ImportEventsJob \
--master spark://spark-master:7071 \
--executor-memory 512M \
--executor-cores 1 \
--conf spark.dynamicAllocation.enabled=true \
--conf spark.shuffle.service.enabled=true \
--conf spark.dynamicAllocation.executorIdleTimeout=30s \
--conf spark.dynamicAllocation.cachedExecutorIdleTimeout=30s \
/usr/local/batch_jobs-assembly-0.1.jar \
-eit event/indicator \
-ep 9200 \
-eu elasticsearch \
-eun elastic \
-eup elastic \
-o hdfs://hdfs:9000/events
