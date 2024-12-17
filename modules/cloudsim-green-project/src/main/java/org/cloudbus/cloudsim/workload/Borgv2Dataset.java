package org.cloudbus.cloudsim.workload; 

// import org.apache.spark.SparkConf;
// import org.apache.spark.api.java.JavaSparkContext;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;


public class Borgv2Dataset {
    static String appName = "CloudSim";

    public static void main(String[] args) {
        //  SparkConf sparkConf = new SparkConf()
        //         .setMaster("local")
        //         .setAppName(appName);
        // JavaSparkContext sparkContext = new JavaSparkContext(sparkConf);

        SparkSession spark = SparkSession.builder().appName(appName).getOrCreate();

        StructType schema = new StructType(new StructField[]{
            new StructField("time", DataTypes.IntegerType, true, null),
            new StructField("missing_info", DataTypes.IntegerType, false, null),
            new StructField("job_id", DataTypes.IntegerType, true, null),
            new StructField("event_type", DataTypes.IntegerType, true, null),
            new StructField("user", DataTypes.StringType, false, null),
            new StructField("scheduling_class", DataTypes.IntegerType, false, null),
            new StructField("job_name", DataTypes.StringType, false, null),
            new StructField("logical_job_name", DataTypes.StringType, false, null)
        });

        String dataDir = "/data/job_events/";
        Dataset<Row> df = spark.read().option("header", "false").schema(schema).csv(dataDir + "*.csv.gz");

        df.show();

        long count = df.count();
    }
}

