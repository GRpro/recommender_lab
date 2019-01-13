package lab.reco.batch

import org.apache.mahout.math.drm.DistributedContext

/** Extended by a platform specific version of this class to create a Mahout CLI driver. */
abstract class MahoutDriver {

  implicit protected var mc: DistributedContext = _
  implicit protected var parser: CustomMahoutOptionParser = _

  var _useExistingContext: Boolean = false // used in the test suite to reuse one context per suite

  /** must be overriden to setup the DistributedContext mc*/
  protected def start() : Unit

  /** Override (optionally) for special cleanup */
  protected def stop(): Unit = {
    if (!_useExistingContext) mc.close
  }

  /** This is where you do the work, call start first, then before exiting call stop */
  protected def process(): Unit

  /** Parse command line and call process */
  def main(args: Array[String]): Unit

}
