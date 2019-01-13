#!/usr/bin/env bash
# parameters
# $1 - HDFS model directory
# $2 - Index/Type to store model
/usr/local/spark/bin/spark-submit \
--class lab.reco.batch.ExportModelJob \
--master spark://spark-master:7071 \
--executor-memory 1000M \
--executor-cores 2 \
--num-executors 2 \
/usr/local/export_job-assembly-0.1.jar \
-i hdfs://hdfs:9000"${1}" \
-eit "${2}" \
-ep 9200 \
-eu elasticsearch \
-eun elastic \
-eup elastic