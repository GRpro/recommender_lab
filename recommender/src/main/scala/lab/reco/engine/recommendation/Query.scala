package lab.reco.engine.recommendation

case class Query(history: Map[String, Seq[String]],
                 length: Option[Int])
