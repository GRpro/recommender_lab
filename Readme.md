Recommender service lab
=======================

This project aims to provide a recommender service which can be run on premise

Ports:

`5555` - event manager

`5556` - recommender

`5557` - job runner


Characteristics
===============

Up to 500 object attributes are supported

Test with retailrocket E-commerce dataset
=========================================

Project provides some utils for setting up things locally.

The system has been tested with Retailrocket dataset https://www.kaggle.com/retailrocket/ecommerce-dataset.

1. Need to allocate 9Gb of RAM to docker. While running project please ensure that amount of free memory is available.
2. Download the dataset from https://www.kaggle.com/retailrocket/ecommerce-dataset and unpack it in `<dataset_path>`
```
ls -lh <dataset_path>/retailrocket-recommender-system-dataset/
total 1941096
-rwxr-xr-x@ 1 grygorii  staff    14K Mar 24  2017 category_tree.csv
-rwxr-xr-x@ 1 grygorii  staff    90M Mar 24  2017 events.csv
-rwxr-xr-x@ 1 grygorii  staff   462M Mar 24  2017 item_properties_part1.csv
-rwxr-xr-x@ 1 grygorii  staff   390M Mar 24  2017 item_properties_part2.csv

```
3. assembly and deploy service
```
$ cd <project_dir>/recommender_lab
$ sbt assembly
```
The following command on the first command takes a while as it builds images before starting the service
```
$ ./dev/start_dev_env.sh
```
See containers running
```
$ docker ps
CONTAINER ID        IMAGE                                                 COMMAND                  CREATED             STATUS              PORTS                                                                            NAMES
61098b3dd690        dev_spark-slave                                       "bash -c 'service ss…"   24 hours ago        Up 24 hours         22/tcp, 0.0.0.0:8081->8081/tcp                                                   dev_spark-slave_2
744104f255e2        dev_spark-slave                                       "bash -c 'service ss…"   24 hours ago        Up 24 hours         22/tcp, 0.0.0.0:8082->8081/tcp                                                   dev_spark-slave_1
d12bb2bea677        dev_spark-master                                      "bash -c 'service ss…"   24 hours ago        Up 24 hours         0.0.0.0:4040->4040/tcp, 0.0.0.0:5557->5557/tcp, 22/tcp, 0.0.0.0:8080->8080/tcp   dev_spark-master_1
a335112caed8        dev_recommender                                       "bash -c 'service ss…"   24 hours ago        Up 24 hours         22/tcp, 0.0.0.0:5556->5556/tcp                                                   dev_recommender_1
8fa8a3dece84        dev_event_manager                                     "bash -c 'service ss…"   24 hours ago        Up 24 hours         22/tcp, 0.0.0.0:5555->5555/tcp                                                   dev_event_manager_1
caf0ab9d42ff        dev_hdfs                                              "bash -c 'service ss…"   24 hours ago        Up 24 hours         0.0.0.0:9000->9000/tcp, 22/tcp, 0.0.0.0:50070->50070/tcp                         dev_hdfs_1
7b3b35cd058e        docker.elastic.co/elasticsearch/elasticsearch:6.4.2   "/usr/local/bin/dock…"   24 hours ago        Up 24 hours         0.0.0.0:9200->9200/tcp, 9300/tcp                                                 elasticsearch
```

4. Upload events, objects, and train model

Configure indicators
```
POST http://localhost:5555/api/model
{
  "primaryIndicator": "transaction",
  "secondaryIndicators": [
    {
      "name": "addtocart" ,
      "priority": 1
    },
    {
      "name": "view" ,
      "priority": 2
    }
  ]  
}
```
Upload events and item properties
Helper script does the work. 
Only first 100 props plus categoryid from the dataset are uploaded.
```
$ python3 ./example/upload_ecom_dataset.py <dataset_path>/retailrocket-recommender-system-dataset/
...
...
elapsed time 1097.7651262283325s: events 606.3574371337891s, props1 267.1118106842041s, props2 224.29587197303772s
```
See events count
```
POST http://localhost:5555/api/events/countAll
No Body

Response:
{
  "number": 2756101
}
```
See objects schema
```
GET http://localhost:5555/api/objects/schema
```

Train model and poll for status until all tasks are finished
for me it took 34 minutes
```
POST http://localhost:5557/api/model/train
```
Example of finished status
```
GET http://localhost:5557/api/model/train

Response:
{
  "id": "export_events",
  "children": [
    {
      "id": "train_model",
      "children": [
        {
          "id": "import_model_transaction",
          "children": [],
          "finishedAt": 1552520328192
        },
        {
          "id": "import_model_addtocart",
          "children": [],
          "finishedAt": 1552520349287
        },
        {
          "id": "import_model_view",
          "children": [],
          "finishedAt": 1552520369010
        }
      ],
      "finishedAt": 1552520303230
    }
  ],
  "finishedAt": 1552518376041
}
```

Get recommendations
```
POST http://localhost:5556/api/recommendation
{
  "history": { 
    "view": ["253185", "443030", "428805", "331725", "372845"],
    "transaction": ["356475"]
  },
  
  "filter": {
    "term": {
      "properties.categoryid" : "1244"
    }
  }
}
```
