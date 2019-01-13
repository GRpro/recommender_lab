import sbt.Keys._

val commonSettings = Seq(
  version := "0.1",
  scalaVersion := "2.11.8",
  javacOptions ++= Seq("-encoding", "UTF-8")
)

lazy val root = project.in(file("."))
  .aggregate(
    recommender,
    event_manager,
    import_job,
    export_job,
    similarity_job,
    job_runner,
    common
  )
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

lazy val import_job = project.in(file("import_job"))
  .settings(commonSettings: _*)
  .dependsOn(common)
  .settings(
    name := "import_job"
  )

lazy val export_job = project.in(file("export_job"))
  .settings(commonSettings: _*)
  .dependsOn(common)
  .settings(
    name := "export_job"
  )

lazy val similarity_job = project.in(file("similarity_job"))
  .settings(commonSettings: _*)
  .dependsOn(common)
  .settings(
    name := "similarity_job"
  )