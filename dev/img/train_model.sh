#!/usr/bin/env bash
# parameters
# $1 - HDFS model dest directory
# $2 - Indicator list

hadoop fs -rm -r "hdfs://hdfs:9000${1}"

/usr/local/spark/bin/spark-submit \
--class lab.reco.batch.ItemSimilarityDriver \
--master spark://spark-master:7071 \
--driver-memory 1500M \
--executor-memory 1900M \
--executor-cores 2 \
--num-executors 2 \
/usr/local/similarity_job-assembly-0.1.jar \
--input hdfs:///events \
--output "hdfs://${1}" \
--master spark://spark-master:7071 \
--indicatorList "${2}" \
--itemIDColumn 2 \
--rowIDColumn 0 \
--filterColumn 1
