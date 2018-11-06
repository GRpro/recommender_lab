package lab.reco.common

object Protocol {

  object Event {
    final val sessionIdField: String = "sessionId"
    final val subjectIdField: String = "subjectId"
    final val objectIdField: String = "objectId"
    final val timestampField: String = "timestamp"
    final val indicatorField: String = "indicator"

    final val indexName: String = "event"
    final val typeName: String = "indicator"
  }

  object Recommendation {

    final val typeName: String = "recommendations"
    final def indexName(indicatorName: String): String = s"$indicatorName"
  }

  object Metadata {
    final val primaryIndicatorField: String = "primaryIndicator"
    final val secondaryIndicatorsField: String = "secondaryIndicators"

    final val indexName: String = "metadata"
    final val typeName: String = "metadata"
    final val modelConfigId: String = "model_config"
  }

}
