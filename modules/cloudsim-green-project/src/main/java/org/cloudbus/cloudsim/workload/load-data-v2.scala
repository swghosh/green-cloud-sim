import org.apache.spark.sql._
import org.apache.spark.sql.types._
import spark.implicits._

val spark = SparkSession.builder().appName("SchemaLoader").getOrCreate()

val schemaDF = spark.read.option("header", "true").csv("/data/schema.csv")

def mapFormatToSparkType(format: String) = format match {
  case "INTEGER" => IntegerType
  case "FLOAT" => FloatType
  case "STRING_HASH" => StringType
  case "STRING_HASH_OR_INTEGER" => StringType
  case "BOOLEAN" => BooleanType
  case _ => StringType
}

val fileSchemas = schemaDF.rdd.groupBy(row => row.getString(0)).map { case (filePattern, rows) =>
    val fields = rows.map { row =>
      StructField(row.getString(2), mapFormatToSparkType(row.getString(3)), row.getString(4) == "NO")
    }.toArray
    (filePattern, StructType(fields))
}.collectAsMap()

def readCsvWithSchema(spark: SparkSession, filePattern: String, schema: StructType): DataFrame = {
  spark.read
    .format("csv")
    .option("header", "false")
    .schema(schema)
    .load(s"/data/$filePattern")
}

val dfs = fileSchemas.map { case (filePattern, schema) =>
  val df = readCsvWithSchema(spark, filePattern, schema)
  df
}.toArray

// 6 dataframes in total:
// job_events
// task_events
// machine_events
// machine_attributes
// task_constraints
// task_usage

dfs.foreach { df =>
  df.show(5)
  println(s"Count: ${df.count()}")
}

spark.stop()
