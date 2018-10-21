import sbt.Keys._

val commonSettings = Seq(
  version := "0.1",
  scalaVersion := "2.11.8",
  javacOptions ++= Seq("-encoding", "UTF-8")
)

lazy val root = project.in(file("."))
  .aggregate(recommender, common)
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

lazy val batch_jobs = project.in(file("batch_jobs"))
  .settings(commonSettings: _*)
  .dependsOn(common)