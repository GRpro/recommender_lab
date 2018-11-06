package lab.reco.engine.recommendation

case class SimilarObjectsRecommendationRequest(objectId: String, size: Option[Int])
case class SimilarObjectsRecommendation(objectId: String, recommendedObjectIds: Seq[String])