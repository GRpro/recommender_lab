I created this project with the purpose of learning recommender systems and improve software design skills. Also I wanted to create something useful. Even though it's a pet project I believe it can be used to solve real information filtering problems which are commonly faced in the E-Commerce world. Feel free to contact me <span style="color:blue">*grigoriyroghkov@gmail.com*</span>.


## What is Recommender Lab ?

Recommender systems play a major role in today's ecommerce industry. Recommender systems suggest items to users such as books, movies, videos, electronic products and many other products in general. Creating a new recommender is costly so companies try to use existing solutions. 

This project aims to provide a RESTful service which accepts events' data and items' properties and provide recomendations. 
Item consists of a set of properties which can be configured. Event means some action made by user towards the item, this action should have a preference evidence.

Modified cross-occurrence correlation algoritm from Apache Mahout alows to use any number of indicators (buy, add to cart, view etc.) to predict user preferences. Model is used to 

## Architecture

The project approaches microservice architecture. Apache Spark and Apache Mahout is used to create item similarity model. The model is trained offline periodically. Trained model it deployed on ElasticSearch, which provides a benefit of fast response for recommendations as well as using it's rich query language to apply business filters to ranked lists of recommendations.
docker-compose is utilized to deploy services.


### Event Manager
Service responsible for managing events and item properties.

### Recommender
Service responsible for providing recommendations.

### Job Runner 
Service conducts model training process, submits Spark jobs and allows to poll for status.

### Spark 
Computes model.

### ElasticSearch
Serves the model, returns recommendations by queries.

### HDFS
Source and target data source for Spark, stores events and intermediate representation of a model.

## REST API

### Event Manager
Runs on port `5555`.

#### Create event
Object properties may be passed along with each event, if object has existing properties they are replaced.

*Request:*

```
POST /api/events/createOne
```

*Response:*

```

```


#### Create events

*Request:*

```
POST /api/events/createMany
```

*Response:*

```

```

#### Count all events

*Request:*

```
POST /api/events/countAll
```

*Response:*

```

```

#### Count events by query

*Request:*

```
POST /api/events/countByQuery
```

*Response:*

```

```

#### Get events by query


*Request:*

```
POST /api/events/getByQuery
```

*Response:*

```

```

#### Delete all events


*Request:*

```
POST /api/events/deleteAll
```

*Response:*

```

```

#### Delete events by query


*Request:*

```
POST /api/events/deleteAll
```

*Response:*

```

```

#### Set object schema
Set object schema or extend existing to add new fields but don't modify existing

*Request:*

```
POST /api/objects/schema
```

*Response:*

```

```

#### Get object schema

*Request:*

```
GET /api/objects/schema
```

*Response:*

```

```


#### Update object
Update or insert single object

*Request:*

```
POST /api/objects/updateById
```

*Response:*

```

```


#### Update multiple objects
Update or insert nultiple objects

*Request:*

```
POST /api/objects/updateMultiById
```

*Response:*

```

```

#### Get object
Get object by id

*Request:*

```
GET /api/objects/getById
```

*Response:*

```

```

#### Delete object
Delete object by id

*Request:*

```
POST /api/objects/deleteById
```

*Response:*

```

```

#### Delete all objects
Delete all objects preserving schema

*Request:*

```
POST /api/objects/deleteAll
```

*Response:*

```

```


#### Delete objects by query
Delete objects by query

*Request:*

```
POST /api/objects/deleteByQuery
```

*Response:*

```

```



#### Set indicators
Configure preference indicators for model

*Request:*

```
POST /api/model
```

*Response:*

```

```

#### Get indicators
Get configured preference indicators for model

*Request:*

```
GET /api/model
```

*Response:*

```

```

### Job Runner
Runs on port `5556`.

#### Train model
*Request:*
```
POST /api/model/train
No body
```
*Response:*
```
{
  "id": "export_events",
  "children": [
    {
      "id": "train_model",
      "children": [
        {
          "id": "import_model_transaction",
          "children": [
            
          ]
        },
        {
          "id": "import_model_addtocart",
          "children": [
            
          ]
        },
        {
          "id": "import_model_view",
          "children": [
            
          ]
        }
      ]
    }
  ]
}
```


#### Get train model status
*Request:*
```
GET /api/model/train
```
*Response:*

The same as for model train submission

```
{
  "id": "export_events",
  "children": [
    {
      "id": "train_model",
      "children": [
        {
          "id": "import_model_transaction",
          "children": [
            
          ]
        },
        {
          "id": "import_model_addtocart",
          "children": [
            
          ]
        },
        {
          "id": "import_model_view",
          "children": [
            
          ]
        }
      ]
    }
  ]
}
```

### Recommender
Runs on port `5556`.

#### Create recommendations
Recommend items based on user history. filter and must_not parts of a query are corresponding properties of [bool ElasticSearch query](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-bool-query.html).
Response contains ranked list of recommended items with item properties.

*Request:*

```
{
  "history": { 
    "<indicator_1>": ["itemId1", "itemId2", ...],
    "<indicator_2>": ["itemId3", "itemId4", ...],
    "<indicator_3>": ["itemId5", "itemId6", ...]
  },
  "filter": <filter part of bool query>,
  "must_not": <must_not part of bool query>,
  ["length": <number>]
}
```

*Response:*

```
[
  {
    "objectId": "<recommended_itemId_1>",
    "objectProperties": {
      "k1": <int>,
      "k2": "<string>
    },
    "score": <int>
  },
  {
    "objectId": "<recommended_itemId_2>",
    "objectProperties": {
      "k1": <int>,
      "k2": "<string>,
      "k3": <bool>
    },
    "score": <int>
  },
  ...
]
```

#### Delete recommendations and object data
Removes all object data and recommendations, should be used carefully.

*Request:*

```
DELETE /api/recommendation
```

*Response:*

```
Status code OK - recommendations deleted
```

## User guide

Project contains deployment scripts to run on docker-compose locally, you need 9GB of RAM to make the thing working. 
The default deployment consists of the following services.

```
dev_spark-slave_2
dev_spark-slave_1
dev_spark-master_1
dev_recommender_1
dev_event_manager_1
dev_hdfs_1
elasticsearch
```

For larger deployments make changes to `./dev/docker-compose.yaml` file.
Item properties are used to filter computed recommendations (e.g return all recommender men's T-Shirts of a red colour). This should be used to apply business rules. Up to 500 searcheable item properties are supported.


### Test with retailrocket dataset

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
See objects schema which was automatically inferred
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

Returns me a list

```
[
  {
    "objectId": "111057",
    "objectProperties": {
      "categoryid": "1503",
      "6": "668584"
    },
    "score": 2
  },
  {
    "objectId": "253615",
    "objectProperties": {
      "49": "484024 661116 1257525",
      "categoryid": "342",
      "6": "1037891"
    },
    "score": 2
  },
  {
    "objectId": "77514",
    "objectProperties": {
      "categoryid": "1051",
      "6": "977762"
    },
    "score": 2
  },
  {
    "objectId": "75490",
    "objectProperties": {
      "6": "203835",
      "76": "769062",
      "28": "150169 435459 16718",
      "categoryid": "358"…
    },
    "score": 1
  },
  {
    "objectId": "237244",
    "objectProperties": {
      "6": "160555 992429",
      "categoryid": "745"
    },
    "score": 1
  }
]
```
