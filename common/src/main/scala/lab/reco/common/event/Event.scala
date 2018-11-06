package lab.reco.common.event

case class Event(subjectId: String,
                 objectId: String,
                 timestamp: Option[Long],
                 indicator: String)

case class StoreEventResponse(sessionId: String)