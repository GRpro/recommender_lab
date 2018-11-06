package lab.reco.job.config

trait RunnerConfig {

  def exportEventsScriptPath: String
  def trainModelScriptPath: String
  def exportModelScriptPath: String
}
