package lab.reco.engine.recommendation

import spray.json.JsObject

case class Query(history: Map[String, Seq[String]],
                 filter: Option[JsObject],
                 must_not: Option[JsObject],
                 length: Option[Int])
