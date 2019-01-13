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
    private def handleCompletion(job: Job, taskName: String)(implicit executionContext: ExecutionContext): Future[Unit] =
      future.map { time =>
        job.succeedTask(taskName, time)
      }.recover {
        case e => job.failTask(taskName, System.currentTimeMillis(), e.getMessage)
          throw e
      }

    @inline
    def exportEventsFinished(job: Job)(implicit executionContext: ExecutionContext): Future[Unit] =
      handleCompletion(job, ExportEventsTask)

    @inline
    def trainModelFinished(job: Job)(implicit executionContext: ExecutionContext): Future[Unit] =
      handleCompletion(job, TrainModelTask)

    @inline
    def importModelFinished(job: Job, indicator: String)(implicit executionContext: ExecutionContext): Future[Unit] =
      handleCompletion(job, importModelTask(indicator))
  }
}
