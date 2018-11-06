#!/usr/bin/env bash
# parameters
# $1 - HDFS model dest directory
# $2 - Primary indicator
# $3 - Secondary indicator

hadoop fs -rm -r hdfs://hdfs:9000"${1}"

if [ -z "$3" ]
  then
    mahout spark-itemsimilarity \
--input hdfs:///events \
--output hdfs://$1 \
--master spark://spark-master:7071 \
--filter1 "${2}" \
--itemIDColumn 2 \
--rowIDColumn 0 \
--filterColumn 1 \
--sparkExecutorMem 2G

  else

    echo "Secondary indicator: $3"
    mahout spark-itemsimilarity \
--input hdfs:///events \
--output hdfs://$1 \
--master spark://spark-master:7071 \
--filter1 "${2}" \
--filter2 "${3}" \
--itemIDColumn 2 \
--rowIDColumn 0 \
--filterColumn 1 \
--sparkExecutorMem 2G

fi

