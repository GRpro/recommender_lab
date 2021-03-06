I created this project with the purpose of learning recommender systems and improve software design skills. Also I wanted to create something useful. Even though it's a pet project I believe it can be used to solve real information filtering problems which are commonly faced in the E-Commerce world. Feel free to contact me <span style="color:blue">*grigoriyroghkov@gmail.com*</span>.

- [What is Recommender Lab](#what-is-recommender-lab)
- [Architecture](#architecture)
- [REST API](#rest-api)
  - [Event Manager](#event-manager)
	- [Create event](#create-event)
	- [Create events](#create-events)
	- [Count all events](#count-all-events)
	- [Count events by query](#count-events-by-query)
	- [Get events by query](#get-events-by-query)
	- [Delete all events](#delete-all-events)
	- [Delete events by query](#delete-events-by-query)
	- [Set object schema](#set-object-schema)
	- [Get object schema](#get-object-schema)
	- [Update object](#update-object)
	- [Update multiple objects](#update-multiple-objects)
	- [Get object](#get-object)
	- [Delete object](#delete-object)
	- [Delete all objects](#delete-all-objects)
	- [Delete objects by query](#delete-objects-by-query)
	- [Set indicators](#set-indicators)
	- [Get indicators](#get-indicators)
  - [Job Runner](#job-runner)
  	- [Train model](#train-model)
	- [Get train model status](#get-train-model-status)
  - [Recommender](#recommender)
	- [Create recommendations](#create-recommendations)
	- [Delete recommendations and object data](#delete-recommendations-and-object-data)
- [User guide](#user-guide)
  - [Test with retailrocket dataset](#test-with-retailrocket-dataset)


## What is Recommender Lab

Recommender systems play a major role in today's ecommerce industry. Recommender systems suggest items to users such as books, movies, videos, electronic products and many other products in general. Creating a new recommender is costly so companies try to use existing solutions. 

This project aims to provide a RESTful service which accepts events' data and items' properties and provide recomendations. 
Item consists of a set of properties which can be configured. Event means some action made by user towards the item, this action should have a preference evidence.

Modified cross-occurrence correlation algoritm from Apache Mahout alows to use any number of indicators (buy, add to cart, view etc.) to predict user preferences.

## Architecture

The recommender system implemented in this project uses Correlated Cross-Occurrence algorithm which allows using multiple indicators effectively. 

Few links:
- [Mahout CCO](http://mahout.apache.org/users/recommender/intro-cooccurrence-spark.html)
- [CCO Presentation](https://www.slideshare.net/pferrel/unified-recommender-39986309)

The project approaches microservice architecture. Apache Spark and Apache Mahout is used to create item similarity model. The model is trained offline periodically. Trained model it deployed on ElasticSearch, which provides a benefit of fast response for recommendations as well as using it's rich query language to apply business filters to ranked lists of recommendations.
docker-compose is utilized to deploy services.


Event Manager - Service responsible for managing events and item properties.
Recommender - Service responsible for providing recommendations.
Job Runner - Service conducts model training process, submits Spark jobs and allows to poll for status.
Spark - Computes model.
ElasticSearch - Serves the model, returns recommendations by queries.
HDFS - Source and target data source for Spark, stores events and intermediate representation of a model.

## REST API
In this section REST endpoints of the service are documented. 
In the json representation fields marked in curlu braces `()` are optional.

### Event Manager
Runs on port `5555`.

#### Create event
Register new user event in the system, the event will have been considered when model is trained.
If *timestamp* field of an event isn't present the event is registered with time when request is made.
If *objectProperties* field is defined the object *objectId* will be set to these properties, if object has existing properties they are replaced. That is useful when running recommender without existing dataset so item properties are populated along the way.

*Request:*

```
POST /api/events/createOne
{
  "subjectId": "<user_id>",
  "objectId": "<item_id>",
  ("timestamp": <number>,)
  "indicator": "<type_of_action>",
  ("objectProperties": {<item_properties_json>})
}
```

*Response:*

```
Status code - 200
```


#### Create events
This endpoint allows register multiple events at a time.

*Request:*

```
POST /api/events/createMany
[
  {
    "subjectId": "<user_id>",
    "objectId": "<item_id>",
    ("timestamp": <number>,)
    "indicator": "<type_of_action>",
    ("objectProperties": {<item_properties_json>})
  },
  {
    "subjectId": "<user_id>",
    "objectId": "<item_id>",
    ("timestamp": <number>,)
    "indicator": "<type_of_action>",
    ("objectProperties": {<item_properties_json>})
  },
  ...
]
```

*Response:*

```
Status code - 200
```

#### Count all events
Get count of all events in the system.

*Request:*

```
POST /api/events/countAll
```

*Response:*

```
{
  "number" : <number>
}
Status code - OK
```

#### Count events by query
Get count of all events matching query. *query* field is a valid [ElasticSearch query](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html).

*Request:*

```
POST /api/events/countByQuery
{
  "query": {
    "term" : { "objectId" : "5421" } 
  }
}
```

*Response:*

```
{
  "number" : <number>
}
Status code - OK
```

#### Get events by query
Get events matching query. *query* field is a valid [ElasticSearch query](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html).

*Request:*

```
POST /api/events/getByQuery
{
  "query": {
    "term" : { "subjectId" : "2" } 
  }
}
```

*Response:*

```
[
  {
    "subjectId": "2",
    "objectId": "325215",
    "timestamp": 1552515374808,
    "indicator": "view"
  },
  {
    "subjectId": "2",
    "objectId": "342816",
    "timestamp": 1552515375917,
    "indicator": "view"
  },
  ...
]
Status code - OK
```

#### Delete all events
Delete all events from the system. Returns number of deleted events.

*Request:*

```
POST /api/events/deleteAll
```

*Response:*

```
{
  "deleted": <number>
}
Status code - OK
```

#### Delete events by query
Delete events matching query. *query* field is a valid [ElasticSearch query](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html). Returns number of deleted events.

*Request:*

```
POST /api/events/deleteAll
{
  "query": {
    "range" : {
      "timestamp" : {
        "lte" : 1265276482312
      }
    }
  }
}
```

*Response:*

```
{
  "deleted": <number>
}
Status code - 200
```

#### Set object schema
Set object schema or extend existing to add new fields but don't modify existing. This is optional API, every time you add new object system inferres schema. The json body is valid [ElasticSearch mapping under mappings.<index_name>.properties field](https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping.html).

*Request:*

```
POST /api/objects/schema
{
  "field1": {
    "type": "text",
    "fields": {
      "keyword": {
        "type": "keyword",
        "ignore_above": 256
      }
    }
  },
  "field2": {
    "type": "text",
    "fields": {
      "keyword": {
        "type": "keyword",
        "ignore_above": 256
      }
    }
  },
  ...
}
```

*Response:*

```
Status code - OK
```

#### Get object schema
Get set or inferred object schema.

*Request:*

```
GET /api/objects/schema
```

*Response:*

```
{
  "field1": {
    "type": "text",
    "fields": {
      "keyword": {
        "type": "keyword",
        "ignore_above": 256
      }
    }
  },
  "field2": {
    "type": "text",
    "fields": {
      "keyword": {
        "type": "keyword",
        "ignore_above": 256
      }
    }
  },
  ...
}
Status code - OK
```


#### Update object
Update or insert single object. If *replace* is true new properties will completely replace existing, if it's false these new properties will be merged with existing one by one (add or replace).

*Request:*

```
POST /api/objects/updateById
{
  "objectId": "<item_id>",
  "replace": <bool>,
  "objectProperties": {<valid_json>}
}
```

*Response:*

```
Status code - OK
```


#### Update multiple objects
Update or insert multiple object properties.

*Request:*

```
POST /api/objects/updateMultiById
[
  {
    "objectId": "<item_id>",
    "replace": <bool>,
    "objectProperties": {<valid_json>}
  },
  {
    "objectId": "<item_id>",
    "replace": <bool>,
    "objectProperties": {<valid_json>}
  },
  ...
]
```

*Response:*

```
Status code - OK
```

#### Get object
Get object by id.

*Request:*

```
GET /api/objects/getById
{
  "objectId": "<item_id>"
}
```

*Response:*

```
{
  "field1": <bool>,
  "field2": <number>,
  "field3": "<string>"
}
Status code - 200
```

#### Delete object
Delete object by id. Returns number of deleted objects (0 or 1).

*Request:*

```
POST /api/objects/deleteById
{
  "field1": <bool>,
  "field2": <number>,
  "field3": "<string>"
}
```

*Response:*

```
{
  "deleted": <number>
}
Status code - 200
```

#### Delete all objects
Delete all objects preserving schema. Returns number of deleted objects.

*Request:*

```
POST /api/objects/deleteAll
```

*Response:*

```
{
  "deleted": <number>
}
Status code - 200
```


#### Delete objects by query
Delete objects by query. *query* field is a valid [ElasticSearch query](https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html). Object fields in a query should be prefixed with *properties.<field_name>*.

*Request:*

```
POST /api/objects/deleteByQuery
{
  "query": {
    "term" : { "properties.<field_name>" : "<some_value>" } 
  }
}
```

*Response:*

```
{
  "deleted": <number>
}
Status code - 200
```

#### Set indicators
Configure preference indicators for model. The first indicator is considered primary and model is targeted to predict events of primary indicator (e.g. purchase), when model is computed the events of other indicators are filtered, events which correlate with primary indicator are remained. Other indicators may be "view", "add to cart" etc.

*Request:*

```
POST /api/model
{
  "primaryIndicator": "<indicator_1>",
  "secondaryIndicators": [
    {
      "name": "<indicator_2>" ,
      "priority": 1
    },
    {
      "name": "<indicator_3>" ,
      "priority": 2
    }
  ]  
}
```

*Response:*

```
Status code - 200
```

#### Get indicators
Get configured preference indicators for model

*Request:*

```
GET /api/model
```

*Response:*

```
{
  "primaryIndicator": "<indicator_1>",
  "secondaryIndicators": [
    {
      "name": "<indicator_2>" ,
      "priority": 1
    },
    {
      "name": "<indicator_3>" ,
      "priority": 2
    }
  ]  
}
Status code - 200
```

### Job Runner
Runs on port `5556`.

#### Train model
Train new model using snapshot of current events in the system. Recommender can server the requests while the model is being trained.

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
Get status of the current process of model training. Response body contains hyerarchical structure which shows up completeness of the steps required to train model. Once the step is finished the field *finishedAt* appears.

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

`./dev/start_dev_env.sh` - rebuild docker images and start all services
`./dev/stop_dev_env.sh` - stop all services
`./dev/follow_logs.sh` - see logs of running services

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
