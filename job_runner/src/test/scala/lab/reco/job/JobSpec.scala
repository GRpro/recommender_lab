package lab.reco.job

import org.scalatest.{Matchers, WordSpec}

import lab.reco.job.CCOJobHelper._
class JobSpec extends WordSpec with Matchers {

  private final val indicators = List("purchase", "cart", "view")

  "Job" should {

    "return correct status" in {

      val job = Job.build(createTasks(indicators))

      job.isFinished shouldBe false
      job.isFailed shouldBe false

      job.succeedTask(ExportEventsTask, 1)
      job.succeedTask(TrainModelTask, 2)
      job.succeedTask(importModelTask("purchase"), 3)
      job.failTask(importModelTask("cart"), 4, "Out of memory error")

      job.isFailed shouldBe true
      job.isFinished shouldBe false

      job.currentStatus shouldBe Task(
        id = ExportEventsTask,
        finishedAt = Some(1),
        children = Seq(
          Task(
            id = TrainModelTask,
            finishedAt = Some(2),
            children = Seq(
              Task(importModelTask("purchase"), Seq.empty, finishedAt = Some(3)),
              Task(importModelTask("cart"), Seq.empty, finishedAt = Some(4), failReason = Some("Out of memory error")),
              Task(importModelTask("view"), Seq.empty)
            )
          )
        )
      )
    }

    "correctly succeed" in {
      val job = Job.build(createTasks(indicators))

      job.succeedTask(ExportEventsTask, 1)
      job.succeedTask(TrainModelTask, 2)
      job.succeedTask(importModelTask("purchase"), 3)
      job.succeedTask(importModelTask("cart"), 4)
      job.succeedTask(importModelTask("view"), 5)

      job.currentStatus shouldBe Task(
        id = ExportEventsTask,
        finishedAt = Some(1),
        children = Seq(
          Task(
            id = TrainModelTask,
            finishedAt = Some(2),
            children = Seq(
              Task(importModelTask("purchase"), Seq.empty, finishedAt = Some(3)),
              Task(importModelTask("cart"), Seq.empty, finishedAt = Some(4)),
              Task(importModelTask("view"), Seq.empty, finishedAt = Some(5))
            )
          )
        )
      )

      job.isFailed shouldBe false
      job.isFinished shouldBe true
    }
  }
}
