#!/usr/bin/env bash
# parameters
# $1 - HDFS model dest directory
# $2 - Indicator list

hadoop fs -rm -r hdfs://hdfs:9000"${1}"

mahout spark-itemsimilarity \
--input hdfs:///events \
--output hdfs://$1 \
--master spark://spark-master:7071 \
--indicatorList "${2}" \
--itemIDColumn 2 \
--rowIDColumn 0 \
--filterColumn 1 \
--sparkExecutorMem 2G \
--define:spark.executor.cores=4 \
--define:spark.dynamicAllocation.enabled=true \
--define:spark.shuffle.service.enabled=true \
--define:spark.dynamicAllocation.executorIdleTimeout=30s \
--define:spark.dynamicAllocation.cachedExecutorIdleTimeout=30s
