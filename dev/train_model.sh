#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

docker cp $DIR/../batch_jobs/target/scala-2.11/batch_jobs-assembly-0.1.jar dev_spark-master_1:/usr/local

docker exec -it dev_spark-master_1 bash -c "/usr/local/spark/bin/spark-submit \
--class lab.reco.batch.ImportEventsJob \
--master spark://spark-master:7071 \
--executor-memory 1G \
--total-executor-cores 2 \
batch_jobs-assembly-0.1.jar \
-eit event/indicator \
-ep 9200 \
-eu elasticsearch \
-eun elastic \
-eup elastic \
-o hdfs://hdfs:9000/events
"

docker exec -it dev_spark-master_1 bash -c "hadoop fs -rm -r hdfs://hdfs:9000/itemsimilarity"
docker exec -it dev_spark-master_1 bash -c "mahout spark-itemsimilarity \
--input hdfs:///events \
--output hdfs:///itemsimilarity \
--master spark://spark-master:7071 \
--filter1 purchase \
--filter2 view \
--itemIDColumn 2 \
--rowIDColumn 0 \
--filterColumn 1 \
--sparkExecutorMem 2G
"

docker exec -it dev_spark-master_1 bash -c "hadoop fs -rm -r hdfs://hdfs:9000/itemsimilarity1"
docker exec -it dev_spark-master_1 bash -c "mahout spark-itemsimilarity \
--input hdfs:///events \
--output hdfs:///itemsimilarity1 \
--master spark://spark-master:7071 \
--filter1 purchase \
--filter2 open \
--itemIDColumn 2 \
--rowIDColumn 0 \
--filterColumn 1 \
--sparkExecutorMem 2G
"

docker exec -it dev_spark-master_1 bash -c "hadoop fs -rm -r hdfs://hdfs:9000/itemsimilarity2"
docker exec -it dev_spark-master_1 bash -c "mahout spark-itemsimilarity \
--input hdfs:///events \
--output hdfs:///itemsimilarity2 \
--master spark://spark-master:7071 \
--itemIDColumn 2 \
--rowIDColumn 0 \
--filterColumn 1 \
--sparkExecutorMem 2G
"

#docker exec -it dev_spark-master_1 bash -c "/usr/local/spark/bin/spark-submit \
#--class lab.reco.batch.ExportModelJob \
#--master spark://spark-master:7071 \
#--executor-memory 1G \
#--total-executor-cores 2 \
#batch_jobs-assembly-0.1.jar \
#-i hdfs://hdfs:9000/itemsimilarity/cross-similarity-matrix \
#-eit recommendation/similarObjects \
#-ep 9200 \
#-eu elasticsearch \
#-eun elastic \
#-eup elastic
#"