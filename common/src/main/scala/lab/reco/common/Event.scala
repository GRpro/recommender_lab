package lab.reco.common


case class Event(subjectId: String,
                 objectId: String,
                 timestamp: Option[Long],
                 indicator: String)

case class StoreEventResponse(sessionId: String)