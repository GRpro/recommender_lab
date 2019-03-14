package lab.reco.event


import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.http.{bulk => _, get => _, _}
import com.sksamuel.elastic4s.indexes.IndexRequest
import com.sksamuel.elastic4s.{Index, SimpleFieldValue}
import com.typesafe.scalalogging.LazyLogging
import lab.reco.common.Protocol.{Recommendation, Event => EventProtocol}
import lab.reco.common.util.Implicits._
import spray.json.ParserInput.StringBasedParserInput
import spray.json.{JsArray, JsBoolean, JsNumber, JsObject, JsString, JsonParser}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Try


class EventManagerImpl(esClient: ElasticClient)(implicit executionContext: ExecutionContext) extends EventManager with LazyLogging {

  private final lazy val client: HttpClient = {
    createMapping()
    esClient.client
  }

  private def createMapping(): Unit = {
    esClient
      .execute {
        createIndex(EventProtocol.indexName) mappings (
          mapping(EventProtocol.typeName) as(
            keywordField(EventProtocol.sessionIdField),
            keywordField(EventProtocol.subjectIdField),
            keywordField(EventProtocol.objectIdField),
            longField(EventProtocol.timestampField),
            keywordField(EventProtocol.indicatorField)
          )
          )
      }
      .logFailure(logger, "failed to create mapping to store events")

    esClient
      .execute {
        createIndex(Recommendation.indexName) mappings (
          mapping(Recommendation.typeName) as()
          )
      }
      .logFailure(logger, "failed to create mapping to store objects")
  }



  /**
    * Process event which subject made towards object
    * @param event [[Event]] representing subject's action
    */
  def processEvent(event: Event): Future[Unit] = {
    val query = insertEventQuery(event, defaultEventTimestamp)

    val indexEventFuture = esClient
      .execute {
        query
      }
      .escalateElasticFailure
      .logSuccess(logger, s"indexed 1 event")
      .logFailure(logger, s"indexing event failed")

    val insertObjectFuture = event.objectProperties
      .map(updateObject(event.objectId, _, replace = true))
      .getOrElse(Future.successful())

    Future.sequence(Seq(indexEventFuture, insertObjectFuture))
      .map(_ => ())
  }

  /**
    * Process multiple events which subject made towards object
    * @param events sequence of [[Event]]
    */
  def processEvents(events: Seq[Event]): Future[Unit] = {
    val time = defaultEventTimestamp
    val query = bulk {
      events.map(insertEventQuery(_, time))
    }

    val indexEventsFuture = esClient
      .execute {
        query
      }
      .escalateElasticFailure
      .logSuccess(logger, s"indexed ${events.size} events")
      .logFailure(logger, s"bulk indexing of ${events.size} events failed")

    val insertObjectsFuture = Future.sequence(
      events.map(event => event.objectProperties
        .map(updateObject(event.objectId, _, replace = true))
        .getOrElse(Future.successful())
      )
    )

    Future.sequence(Seq(indexEventsFuture, insertObjectsFuture))
      .map(_ => ())
  }

  /**
    * Delete events matching predicate
    * @param jsonQuery ElasticSearch json query
    * @return [[DeleteResult]] representing changes happened
    */
  def deleteEvents(jsonQuery: String): Future[Int] = {
    val request = ElasticRequest("POST", s"${EventProtocol.indexName}/_delete_by_query",
      HttpEntity(
        s"""
           |{
           |  "query": $jsonQuery
           |}
      """.stripMargin)
    )

    val promise = Promise[HttpResponse]()
    client.send(request, {
      case Left(e) => promise.failure(e)
      case Right(result) => promise.success(result)
    })

    promise.future.map { response =>
      logger.info(s"delete by query returned response [$response]")

      response.statusCode match {
        case 200 =>

        case _ =>
      }
      val entity = JsonParser(new StringBasedParserInput(response.entity.get.content)).asJsObject
      val deleted = entity.fields("deleted").asInstanceOf[JsNumber].value.toInt
      val errorsList = entity.fields("failures").asInstanceOf[JsArray].elements.toList

      if (errorsList.nonEmpty) {
        throw FromElasticException(errorsList.toString())
      }
      deleted
    }
  }

  /**
    * Delete all events
    * @return [[DeleteResult]] representing changes happened
    */
  def deleteAllEvents(): Future[Int] = {
    val query = """{"match_all": {}}"""
    deleteEvents(query)
  }

  /**
    * Get number of events matching predicate
    * @param jsonQuery ElasticSearch json query
    * @return [[CountResult]] representing event count
    */
  def getEventsCount(jsonQuery: String): Future[Int] = {
    val request = ElasticRequest("GET", s"${EventProtocol.indexName}/${EventProtocol.typeName}/_count",
      HttpEntity(
        s"""
           |{
           |  "query": $jsonQuery
           |}
      """.stripMargin)
    )

    val promise = Promise[HttpResponse]()
    client.send(request, {
      case Left(e) => promise.failure(e)
      case Right(result) => promise.success(result)
    })

    promise.future.map { response =>
      logger.info(s"count events response [$response]")
      val entity = JsonParser(new StringBasedParserInput(response.entity.get.content)).asJsObject
      val count = entity.fields("count").asInstanceOf[JsNumber].value.toInt
      count
    }
  }

  /**
    * Get all events count
    * @return [[CountResult]] representing event count
    */
  def getAllEventsCount(): Future[Int] = {
    val query = """{"match_all": {}}"""
    getEventsCount(query)
  }

  /**
    * Get events matching predicate
    * @param jsonQuery ElasticSearch json query
    * @return
    */
  def getEvents(jsonQuery: String): Future[Seq[Event]] = {
    val request = ElasticRequest("GET", s"${EventProtocol.indexName}/_search",
      HttpEntity(
        s"""
           |{
           |  "query": $jsonQuery
           |}
      """.stripMargin)
    )

    val promise = Promise[HttpResponse]()
    client.send(request, {
      case Left(e) => promise.failure(e)
      case Right(result) => promise.success(result)
    })

    promise.future.map { response =>
      logger.info(s"get events response [$response]")
      val entity = JsonParser(new StringBasedParserInput(response.entity.get.content)).asJsObject

      entity
        .fields.get("hits").map { hits =>
        hits.asJsObject.fields("hits")
          .asInstanceOf[JsArray].elements.map { e =>
          val doc = e.asJsObject.fields("_source").asJsObject
          Event(
            doc.fields(EventProtocol.subjectIdField).asInstanceOf[JsString].value,
            doc.fields(EventProtocol.objectIdField).asInstanceOf[JsString].value,
            None,
            Some(doc.fields(EventProtocol.timestampField).asInstanceOf[JsNumber].value.toLong),
            doc.fields(EventProtocol.indicatorField).asInstanceOf[JsString].value
          )
        }
      }.getOrElse(/* index not found */ Seq.empty)
    }
  }


  private def insertEventQuery(event: Event, defaultTimestamp: => Long): IndexRequest =
    indexInto(EventProtocol.indexName, EventProtocol.typeName) fieldValues(
      SimpleFieldValue(EventProtocol.subjectIdField, event.subjectId),
      SimpleFieldValue(EventProtocol.objectIdField, event.objectId),
      SimpleFieldValue(EventProtocol.timestampField, event.timestamp.getOrElse(defaultTimestamp)),
      SimpleFieldValue(EventProtocol.indicatorField, event.indicator)
    )

  private def defaultEventTimestamp: Long = System.currentTimeMillis()


  //  override def deleteAllEvents(): Future[Unit] = {
  //    esClient execute deleteIndex(EventProtocol.indexName) map { result =>
  //      logger.info(s"delete index [${EventProtocol.indexName}] result [$result]")
  //    } logFailure(logger, s"delete index operation [${EventProtocol.indexName}] failed") map { _ =>
  //      createMapping()
  //    }
  //  }

  def getObjectSchema(): Future[Option[JsObject]] = {
    val request = ElasticRequest("GET", s"${Recommendation.indexName}/_mapping/${Recommendation.typeName}")

    val promise = Promise[HttpResponse]()
    client.send(request, {
      case Left(e) => promise.failure(e)
      case Right(result) => promise.success(result)
    })

    promise.future.map { response =>
      logger.info(s"get object mapping response [$response]")
      response.statusCode match {
        case 200 =>
          val obj = JsonParser(new StringBasedParserInput(response.entity.get.content)).asJsObject
          Try {
            obj
              .fields(Recommendation.indexName).asJsObject
              .fields("mappings").asJsObject
              .fields(Recommendation.typeName).asJsObject
              .fields("properties").asJsObject
              .fields(Recommendation.propertiesField).asJsObject
              .fields("properties").asJsObject
          }.toOption
        case 404 =>
          None
      }
    }
  }

  def setObjectSchema(jsonMapping: JsObject): Future[Unit] = {
    val request = ElasticRequest("PUT", s"${Recommendation.indexName}/_mapping/${Recommendation.typeName}",
      HttpEntity(
        s"""
           |{
           |   "properties": {
           |       "${Recommendation.propertiesField}": {
           |           "properties": ${jsonMapping.compactPrint}
           |       }
           |   }
           |}
        """.stripMargin
      )
    )

    println(request)
    val promise = Promise[HttpResponse]()
    client.send(request, {
      case Left(e) => promise.failure(e)
      case Right(result) => promise.success(result)
    })

    promise.future.map { response =>
      logger.info(s"set object mapping response [$response]")
      response.statusCode match {
        case 200 =>
          val acknowledged = JsonParser(new StringBasedParserInput(response.entity.get.content)).asJsObject
            .fields("acknowledged").asInstanceOf[JsBoolean].value
          if (!acknowledged) {
            throw FromElasticException("Set mapping is not acknowledged")
          }
        case _ =>
          throw FromElasticException(response.toString)
      }
    }
  }

  // TODO protect against setting field as []
  def withSquareBrackets(json: JsObject): String = {
    val jsonStr = json.compactPrint
    val result = s"[${jsonStr.substring(1, jsonStr.length - 1).replace("\"", "\\\"")}]"
    if (result == "[]") throw new IllegalArgumentException("cannot set [] in update with replace mode")
    else result
  }

  override def updateObjects(updates: Seq[(String, JsObject)], replace: Boolean): Future[Unit] = {

    def updateReq(objectId: String) =
      s"""{ "update" : {"_id" : "$objectId", "_type" : "${Recommendation.typeName}", "_index" : "${Recommendation.indexName}", "retry_on_conflict" : 5} }""".stripMargin

    val entity = updates.map {
      case (objectId, properties) =>

        val entity =
          if (replace) {
            s"""${updateReq(objectId)}
               |{ "script" : "ctx._source.${Recommendation.propertiesField} = ${withSquareBrackets(properties)}", "upsert" : {}, "scripted_upsert" : true }
               |""".stripMargin
          } else {
            s"""${updateReq(objectId)}
               |{ "doc" : { "${Recommendation.propertiesField}": $properties }, "doc_as_upsert" : true }
               |""".stripMargin
          }
        entity
    }.mkString("")

    // , Map("Content-Type" -> "application/x-ndjson")
    println(entity)
    val request = ElasticRequest("POST", "_bulk", HttpEntity(entity))

    val promise = Promise[HttpResponse]()
    client.send(request, {
      case Left(e) => promise.failure(e)
      case Right(result) => promise.success(result)
    })

    promise
      .future.map { response =>
      logger.info(s"update object returned response $response")
      response.statusCode match {
        case 200 | 201 =>
          true
        case _ =>
          throw FromElasticException(response.toString)
      }
    }
  }

  def updateObject(objectId: String, objectProperties: JsObject, replace: Boolean): Future[Boolean] = {
    val entity = if (replace) {



      HttpEntity(
        s"""
           |{
           |  "scripted_upsert":true,
           |  "script" : "ctx._source.${Recommendation.propertiesField} = ${withSquareBrackets(objectProperties)}",
           |  "upsert" : {}
           |}
      """.stripMargin
      )
    } else {
      HttpEntity(
        s"""
           |{
           |  "doc" : {
           |    "${Recommendation.propertiesField}": $objectProperties
           |  },
           |  "doc_as_upsert" : true
           |}
      """.stripMargin
      )
    }

    val endpoint = s"${Recommendation.indexName}/${Recommendation.typeName}/$objectId/_update"
    val params = Map("retry_on_conflict" -> 5)
    val request = ElasticRequest("POST", endpoint, params, entity)

    val promise = Promise[HttpResponse]()
    client.send(request, {
      case Left(e) => promise.failure(e)
      case Right(result) => promise.success(result)
    })

    promise
      .future.map { response =>
      logger.info(s"update object returned response $response")
      response.statusCode match {
        case 200 | 201 =>
          true
        case _ =>
          throw FromElasticException(response.toString)
      }
    }
  }

  def deleteObject(objectId: String): Future[Boolean] = {
    val query =
      s"""{
         |  "ids" : {
         |    "type" : "${Recommendation.typeName}",
         |    "values" : ["$objectId"]
         |  }
         |}""".stripMargin
    deleteObjects(query).map(_ == 1)
  }

  def deleteObjects(jsonQuery: String): Future[Int] = {
    val request = ElasticRequest("POST", s"${Recommendation.indexName}/_update_by_query",
      HttpEntity(
        s"""{
           |  "script" : "ctx._source.remove('${Recommendation.propertiesField}')",
           |  "query": {
           |    "bool": {
           |      "filter": [
           |      {
           |        "exists": {"field": "${Recommendation.propertiesField}"}
           |      },
           |      $jsonQuery
           |      ]
           |    }
           |  },
           |  "conflicts": "proceed"
           |}
      """.stripMargin)
    )

    val promise = Promise[HttpResponse]()
    client.send(request, {
      case Left(e) => promise.failure(e)
      case Right(result) => promise.success(result)
    })

    promise.future.map { response =>
      logger.info(s"delete by query returned response [$response]")
      response.statusCode match {
        case 200 =>
          val entity = JsonParser(new StringBasedParserInput(response.entity.get.content)).asJsObject
          val deleted = entity.fields("updated").asInstanceOf[JsNumber].value.toInt
          val errorsList = entity.fields("failures").asInstanceOf[JsArray].elements.toList

          if (errorsList.nonEmpty) {
            throw FromElasticException(response.toString)
          }
          deleted
        case _ =>
          throw FromElasticException(response.toString)
      }
    }
  }

  def deleteAllObjects(): Future[Int] = {
    val query = """{"match_all": {}}"""
    deleteObjects(query)
  }

  def getObject(objectId: String): Future[JsObject] = {
    esClient.execute {
      get(Index.toIndex(Recommendation.indexName), Recommendation.typeName, objectId)
    } map { result =>
      val response = result.body.get

      JsonParser(new StringBasedParserInput(response)).asJsObject
        .fields
        .get("_source").flatMap { doc =>
        doc.asJsObject
          .fields
          .get("properties").map(_.asJsObject)
      }.getOrElse(JsObject())
    }
  }

}
