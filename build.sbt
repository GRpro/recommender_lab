import sbt.Keys._

val commonSettings = Seq(
  version := "0.1",
  scalaVersion := "2.11.8",
  javacOptions ++= Seq("-encoding", "UTF-8")
)

lazy val root = project.in(file("."))
  .aggregate(recommender, event_manager, batch_jobs, job_runner, common, batch)
  .settings(commonSettings: _*)
  .settings(
    name := "recommender_lab"
  )

lazy val common = project.in(file("common"))
  .settings(commonSettings: _*)
  .settings(
    name := "common"
  )

lazy val recommender = project.in(file("recommender"))
  .settings(commonSettings: _*)
  .settings(
    name := "recommender"
  )
  .dependsOn(common)

lazy val event_manager = project.in(file("event_manager"))
  .settings(commonSettings: _*)
  .settings(
    name := "event_manager"
  )
  .dependsOn(common)

lazy val job_runner = project.in(file("job_runner"))
  .settings(commonSettings: _*)
  .settings(
    name := "job_runner"
  )
  .dependsOn(common)

lazy val batch_jobs = project.in(file("batch_jobs"))
  .settings(commonSettings: _*)
  .dependsOn(common)

lazy val batch = project.in(file("batch"))
  .settings(commonSettings: _*)
//  .dependsOn(common)