package lab.reco.common

case class SimilarObjectsRecommendationRequest(objectId: String, limit: Option[Int])
case class SimilarObjectsRecommendation(objectId: String, recommendedObjectIds: Seq[String])