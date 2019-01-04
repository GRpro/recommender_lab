package lab.reco.job

import scala.concurrent.{ExecutionContext, Future}


object CCOJobHelper {

  final val ExportEventsTask: String = "export_events"
  final val TrainModelTask: String = "train_model"

  final def importModelTask(indicator: String): String = s"import_model_$indicator"

  def createTasks(indicators: Seq[String]) = Task(
    id = ExportEventsTask,
    children = Seq(
      Task(
        id = TrainModelTask,
        children = indicators.map { name =>
          Task(importModelTask(name), Seq.empty)
        }
      )
    )
  )

  implicit class JobOperations(val future: Future[Long]) extends AnyVal {

    @inline
    private def handleCompletion(jobOption: Option[Job], taskName: String)(implicit executionContext: ExecutionContext): Future[Unit] =
      future.map { time =>
        jobOption.foreach(_.succeedTask(taskName, time))
      }.recover {
        case e => jobOption.foreach(_.failTask(taskName, System.currentTimeMillis(), e.getMessage))
          throw e
      }

    @inline
    def exportEventsFinished(jobOption: Option[Job])(implicit executionContext: ExecutionContext): Future[Unit] =
      handleCompletion(jobOption, ExportEventsTask)

    @inline
    def trainModelFinished(jobOption: Option[Job])(implicit executionContext: ExecutionContext): Future[Unit] =
      handleCompletion(jobOption, TrainModelTask)

    @inline
    def importModelFinished(jobOption: Option[Job], indicator: String)(implicit executionContext: ExecutionContext): Future[Unit] =
      handleCompletion(jobOption, importModelTask(indicator))
  }
}
