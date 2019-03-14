package lab.reco.job

import java.util.concurrent.atomic.AtomicBoolean

import lab.reco.common.Protocol.Recommendation.{indexName, recommendationsField, typeName}
import lab.reco.common.model.{EventConfigService, IndicatorConfig, IndicatorsConfig}
import lab.reco.job.config.RunnerConfig
import org.scalatest.{Matchers, WordSpec}
import org.scalamock.scalatest.MockFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}

class ModelServiceSpec extends WordSpec with MockFactory with Matchers {

  "train" should {

    "correctly in order follow steps to create new model" in {
      val eventServiceConfigMock = stub[EventConfigService]

      (eventServiceConfigMock.getIndicatorsConfig _).when()
        .returns(Future.successful(Some(IndicatorsConfig("primary", Seq(IndicatorConfig("secondary", 1))))))

      (eventServiceConfigMock.getModelVersion _).when()
        .returns(Future.successful(0))
      (eventServiceConfigMock.setModelVersion _).when(1)
        .returns(Future.successful())


      val modelOperationsMock = stub[ModelOperations]

      (modelOperationsMock.runExportEvents _).when().once().returns(Future.successful(1))
      (modelOperationsMock.runTrainModel _).when("/model", List("primary", "secondary")).once().returns(Future.successful(1))
      (modelOperationsMock.runImportModel _).when(s"/model/similarity-matrix-primary", s"$indexName/$typeName", recommendationsField("primary", 1.toString)).once().returns(Future.successful(1))
      (modelOperationsMock.runImportModel _).when(s"/model/similarity-matrix-secondary", s"$indexName/$typeName", recommendationsField("secondary", 1.toString)).once().returns(Future.successful(1))


      val modelService = new ModelServiceImpl(eventServiceConfigMock, modelOperationsMock)


      modelService.train()



      // wait 10 seconds at most until finished
      val end = System.currentTimeMillis() + 10.seconds.toMillis
      while (!modelService.jobInfo.get.isFinished && System.currentTimeMillis() < end) {
        Thread.sleep(1.second.toMillis)
      }
      if (!modelService.jobInfo.get.isFinished) {
        fail()
      }

      (modelOperationsMock.runExportEvents _).verify().once()
      (modelOperationsMock.runTrainModel _).verify("/model", List("primary", "secondary")).once()
      (modelOperationsMock.runImportModel _).verify(s"/model/similarity-matrix-primary", s"$indexName/$typeName", recommendationsField("primary", 1.toString)).once()
      (modelOperationsMock.runImportModel _).verify(s"/model/similarity-matrix-secondary", s"$indexName/$typeName", recommendationsField("secondary", 1.toString)).once()

    }
  }
}
