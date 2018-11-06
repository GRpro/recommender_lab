package lab.reco.common.util

trait Timed {

  def timed[T](block: => T)(action: Long => Unit): T = {
    val start = System.currentTimeMillis()
    try {
      block
    } finally {
      val elapsed = System.currentTimeMillis() - start
      action(elapsed)
    }
  }

}
