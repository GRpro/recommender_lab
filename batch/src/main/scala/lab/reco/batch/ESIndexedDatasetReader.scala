package lab.reco.batch

import org.apache.mahout.math.drm.DistributedContext
import org.apache.mahout.math.indexeddataset.{BiDictionary, Reader, Schema}
import org.apache.mahout.sparkbindings.indexeddataset.IndexedDatasetSpark


/**
  * Created by grygorii on 11/20/18.
  */
class ESIndexedDatasetReader extends Reader[IndexedDatasetSpark] {
  override val mc: DistributedContext = ???
  override val readSchema: Schema = ???

  override protected def elementReader(mc: DistributedContext, readSchema: Schema, source: String, existingRowIDs: Option[BiDictionary]): IndexedDatasetSpark = ???

  override protected def rowReader(mc: DistributedContext, readSchema: Schema, source: String, existingRowIDs: Option[BiDictionary]): IndexedDatasetSpark = ???
}
