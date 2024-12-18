package org.cloudbus.cloudsim.workload; 

// Run with: 
// $SPARK_PATH~/bin/spark-submit --class "org.cloudbus.cloudsim.workload.Borgv2Dataset"  --master "local[*]" modules/cloudsim-green-project/target/cloudsim-green-project-7.0.0-alpha.jar /data/clusterdata-2011-2/job_events

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;


public class Borgv2Dataset {
    static String appName = "CloudSim";

    public static void main(String[] args) {
        // SparkConf sparkConf = new SparkConf()
        //         .setMaster("local[4]")
        //         .setAppName(appName);
        // JavaSparkContext sparkContext = new JavaSparkContext(sparkConf);

        // SparkSession spark = new SparkSession(sparkContext.sc());

        SparkSession spark = SparkSession.builder().appName(appName).getOrCreate();

        StructType schema = new StructType(new StructField[]{
            new StructField("time", DataTypes.IntegerType, true, Metadata.empty()),
            new StructField("missing_info", DataTypes.IntegerType, false, Metadata.empty()),
            new StructField("job_id", DataTypes.IntegerType, true, Metadata.empty()),
            new StructField("event_type", DataTypes.IntegerType, true, Metadata.empty()),
            new StructField("user", DataTypes.StringType, false, Metadata.empty()),
            new StructField("scheduling_class", DataTypes.IntegerType, false, Metadata.empty()),
            new StructField("job_name", DataTypes.StringType, false, Metadata.empty()),
            new StructField("logical_job_name", DataTypes.StringType, false, Metadata.empty())
        });

        String dataDir = args[0];
        Dataset<Row> df = spark.read().option("header", "false").schema(schema).csv(dataDir + "/*.csv.gz");
        df.show();

        System.out.println(df.count());
    }
}

