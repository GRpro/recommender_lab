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
    final val indexName: String = "object"
    final val typeName: String = "info"
    final val propertiesField: String = "properties"

    final def recommendationsField(indicator: String, modelVersion: String): String =
      s"recommendation_${indicator}_$modelVersion"
  }

  object Metadata {
    final val indexName: String = "metadata"
    final val typeName: String = "metadata"

    final val primaryIndicatorField: String = "primaryIndicator"
    final val secondaryIndicatorsField: String = "secondaryIndicators"
    final val indicatorsConfigId: String = "indicators_config"


    final val modelConfigId: String = "model_config"
    final val modelVersionField: String = "version"
  }

}
