package lab.reco.engine.recommendation

import spray.json.JsObject

case class Recommendation(objectId: String, objectProperties: JsObject, score: Double)
