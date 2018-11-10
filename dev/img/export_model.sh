#!/usr/bin/env bash
# parameters
# $1 - HDFS model directory
# $2 - Index/Type to store model
/usr/local/spark/bin/spark-submit \
--class lab.reco.batch.ExportModelJob \
--master spark://spark-master:7071 \
--executor-memory 512M \
--executor-cores 1 \
--conf spark.dynamicAllocation.enabled=true \
--conf spark.shuffle.service.enabled=true \
--conf spark.dynamicAllocation.executorIdleTimeout=30s \
--conf spark.dynamicAllocation.cachedExecutorIdleTimeout=30s \
/usr/local/batch_jobs-assembly-0.1.jar \
-i hdfs://hdfs:9000"${1}" \
-eit "${2}" \
-ep 9200 \
-eu elasticsearch \
-eun elastic \
-eup elastic
