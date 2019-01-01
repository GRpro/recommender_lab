package lab.reco.batch

import org.apache.spark.SparkConf
import scopt.OptionParser

import scala.collection.immutable

/**
  * Defines oft-repeated options and their parsing. Provides the option groups and parsing helper methods to
  * keep both standarized.
  * @param programName Name displayed in help message, the name by which the driver is invoked.
  * @note options are engine neutral by convention. See the engine specific extending class for
  *       to add Spark or other engine options.
  */
class CustomMahoutOptionParser(programName: String) extends OptionParser[Map[String, Any]](programName: String) {
  // build options from some stardard CLI param groups
  // Note: always put the driver specific options at the last so they can override any previous options!

  var opts = Map.empty[String, Any]

  override def showUsageOnError = Some(true)

  def parseIOOptions(numInputs: Int = 1) = {
    opts = opts ++ CustomMahoutOptionParser.FileIOOptions
    note("Input, output options")
    opt[String]('i', "input") required() action { (x, options) =>
      options + ("input" -> x)
    } text ("Input path, may be a filename, directory name, or comma delimited list of HDFS supported URIs" +
      " (required)")

    opt[String]('o', "output") required() action { (x, options) =>
      if (x.endsWith("/")) {
        options + ("output" -> x)
      } else {
        options + ("output" -> (x + "/"))
      }
    } text ("Path for output directory, any HDFS supported URI (required)")

  }

  def parseGenericOptions() = {
    opts = opts ++ CustomMahoutOptionParser.GenericOptions
    opt[Int]("randomSeed") abbr ("rs") action { (x, options) =>
      options + ("randomSeed" -> x)
    } validate { x =>
      if (x > 0) success else failure("Option --randomSeed must be > 0")
    }

    //output both input IndexedDatasets
    opt[Unit]("writeAllDatasets") hidden() action { (_, options) =>
      options + ("writeAllDatasets" -> true)
    }//Hidden option, though a user might want this.
  }

  def parseElementInputSchemaOptions() = {
    //Input text file schema--not driver specific but input data specific, elements input,
    // not rows of IndexedDatasets
    opts = opts ++ CustomMahoutOptionParser.TextDelimitedElementsOptions
    note("\nInput text file schema options:")
    opt[String]("inDelim") abbr ("id") text ("Input delimiter character (optional). Default: \"[ ,\\t]\"") action {
      (x, options) =>
        options + ("inDelim" -> x)
    }

    opt[String]("indicatorList") abbr ("ind") action { (x, options) =>
      options + ("indicatorList" -> x)
    } text (" TODO Comma-separated String (or regex) whose presence indicates a datum for the primary item set (optional). " +
      "Default: no filter, all data is used")

    opt[Int]("rowIDColumn") abbr ("rc") action { (x, options) =>
      options + ("rowIDColumn" -> x)
    } text ("Column number (0 based Int) containing the row ID string (optional). Default: 0") validate {
      x =>
        if (x >= 0) success else failure("Option --rowIDColNum must be >= 0")
    }

    opt[Int]("itemIDColumn") abbr ("ic") action { (x, options) =>
      options + ("itemIDColumn" -> x)
    } text ("Column number (0 based Int) containing the item ID string (optional). Default: 1") validate {
      x =>
        if (x >= 0) success else failure("Option --itemIDColNum must be >= 0")
    }

    opt[Int]("filterColumn") abbr ("fc") action { (x, options) =>
      options + ("filterColumn" -> x)
    } text ("Column number (0 based Int) containing the filter string (optional). Default: -1 for no " +
      "filter") validate { x =>
      if (x >= -1) success else failure("Option --filterColNum must be >= -1")
    }

    note("\nUsing all defaults the input is expected of the form: \"userID<tab>itemId\" or" +
      " \"userID<tab>itemID<tab>any-text...\" and all rows will be used")

    //check for column consistency
    checkConfig { options: Map[String, Any] =>
      if (options("filterColumn").asInstanceOf[Int] == options("itemIDColumn").asInstanceOf[Int]
        || options("filterColumn").asInstanceOf[Int] == options("rowIDColumn").asInstanceOf[Int]
        || options("rowIDColumn").asInstanceOf[Int] == options("itemIDColumn").asInstanceOf[Int])
        failure("The row, item, and filter positions must be unique.") else success
    }
  }

  def parseFileDiscoveryOptions() = {
    //File finding strategy--not driver specific
    opts = opts ++ CustomMahoutOptionParser.FileDiscoveryOptions
    note("\nFile discovery options:")
    opt[Unit]('r', "recursive") action { (_, options) =>
      options + ("recursive" -> true)
    } text ("Searched the -i path recursively for files that match --filenamePattern (optional), Default: false")

    opt[String]("filenamePattern") abbr ("fp") action { (x, options) =>
      options + ("filenamePattern" -> x)
    } text ("Regex to match in determining input files (optional). Default: filename in the --input option " +
      "or \"^part-.*\" if --input is a directory")

  }

  def parseIndexedDatasetFormatOptions(notice: String = "\nOutput text file schema options:") = {
    opts = opts ++ CustomMahoutOptionParser.TextDelimitedIndexedDatasetOptions
    note(notice)
    opt[String]("rowKeyDelim") abbr ("rd") action { (x, options) =>
      options + ("rowKeyDelim" -> x)
    } text ("Separates the rowID key from the vector values list (optional). Default: \"\\t\"")

    opt[String]("columnIdStrengthDelim") abbr ("cd") action { (x, options) =>
      options + ("columnIdStrengthDelim" -> x)
    } text ("Separates column IDs from their values in the vector values list (optional). Default: \":\"")

    opt[String]("elementDelim") abbr ("td") action { (x, options) =>
      options + ("elementDelim" -> x)
    } text ("Separates vector element values in the values list (optional). Default: \" \"")

    opt[Unit]("omitStrength") abbr ("os") action { (_, options) =>
      options + ("omitStrength" -> true)
    } text ("Do not write the strength to the output files (optional), Default: false.")
    note("This option is used to output indexable data for creating a search engine recommender.")

    note("\nDefault delimiters will produce output of the form: " +
      "\"itemID1<tab>itemID2:value2<space>itemID10:value10...\"")
  }

  def parseSparkOptions()(implicit sparkConf: SparkConf) = {
    opts = opts ++ CustomMahoutOptionParser.SparkOptions
    opts = opts + ("appName" -> programName)
    note("\nSpark config options:")

    opt[String]("master") abbr "ma" text ("Spark Master URL (optional). Default: \"local\". Note that you can " +
      "specify the number of cores to get a performance improvement, for example \"local[4]\"") action { (x, options) =>
      options + ("master" -> x)
    }

    opt[String]("sparkExecutorMem") abbr "sem" text ("Max Java heap available as \"executor memory\" on each " +
      "node (optional). Default: as Spark config specifies") action { (x, options) =>
      options + ("sparkExecutorMem" -> x)
    }

    opt[(String, String)]("define") abbr "D" unbounded() foreach { case (k, v) =>
      sparkConf.set(k, v)
    } validate { x =>
      if (x._2 != "") success else failure("Value <sparkConfValue> must be non-blank")
    } keyValueName("<sparkConfKey>", "<sparkConfValue>") text ("Set the <sparkConfKey> to <sparkConfValue> before " +
      "creating this job's Spark context (optional)")

  }

}

/**
  * Companion object defines default option groups for reference in any driver that needs them.
  * @note not all options are platform neutral so other platforms can add default options here if desired
  */
object CustomMahoutOptionParser {

  // set up the various default option groups
  final val GenericOptions = immutable.HashMap[String, Any](
    "randomSeed" -> System.currentTimeMillis().toInt,
    "writeAllDatasets" -> false)

  final val SparkOptions = immutable.HashMap[String, Any](
    "master" -> "local",
    "sparkExecutorMem" -> "",
    "appName" -> "Generic Spark App, Change this.")

  final val FileIOOptions = immutable.HashMap[String, Any](
    "input" -> null.asInstanceOf[String],
    "output" -> null.asInstanceOf[String])

  final val FileDiscoveryOptions = immutable.HashMap[String, Any](
    "recursive" -> false,
    "filenamePattern" -> "^part-.*")

  final val TextDelimitedElementsOptions = immutable.HashMap[String, Any](
    "rowIDColumn" -> 0,
    "itemIDColumn" -> 1,
    "filterColumn" -> -1,
    "indicatorList" -> null.asInstanceOf[String],
    "inDelim" -> "[,\t ]")

  final val TextDelimitedIndexedDatasetOptions = immutable.HashMap[String, Any](
    "rowKeyDelim" -> "\t",
    "columnIdStrengthDelim" -> ":",
    "elementDelim" -> " ",
    "omitStrength" -> false)
}


