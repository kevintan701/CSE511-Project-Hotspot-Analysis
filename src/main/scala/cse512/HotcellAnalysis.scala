package cse512

/**
 * Hot Cell Analysis Implementation
 * Part of CSE 511 Project 2: Hot Spot Analysis
 * Author: Yuntao (Kevin) Tan
 * Institution: Arizona State University
 * Course: CSE 511 - Data Processing at Scale
 */

import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.udf
import org.apache.spark.sql.functions._

object HotcellAnalysis {
  Logger.getLogger("org.spark_project").setLevel(Level.WARN)
  Logger.getLogger("org.apache").setLevel(Level.WARN)
  Logger.getLogger("akka").setLevel(Level.WARN)
  Logger.getLogger("com").setLevel(Level.WARN)

def runHotcellAnalysis(spark: SparkSession, pointPath: String): DataFrame =
{
  // Load the original data from a data source
  var pickupInfo = spark.read.format("com.databricks.spark.csv").option("delimiter",";").option("header","false").load(pointPath);
  pickupInfo.createOrReplaceTempView("nyctaxitrips")
  pickupInfo.show()

  // Assign cell coordinates based on pickup points
  spark.udf.register("CalculateX",(pickupPoint: String)=>((
    HotcellUtils.CalculateCoordinate(pickupPoint, 0)
    )))
  spark.udf.register("CalculateY",(pickupPoint: String)=>((
    HotcellUtils.CalculateCoordinate(pickupPoint, 1)
    )))
  spark.udf.register("CalculateZ",(pickupTime: String)=>((
    HotcellUtils.CalculateCoordinate(pickupTime, 2)
    )))
  pickupInfo = spark.sql("select CalculateX(nyctaxitrips._c5),CalculateY(nyctaxitrips._c5), CalculateZ(nyctaxitrips._c1) from nyctaxitrips")
  var newCoordinateName = Seq("x", "y", "z")
  pickupInfo = pickupInfo.toDF(newCoordinateName:_*)
  pickupInfo.show()

  // Define the min and max of x, y, z
  val minX = -74.50/HotcellUtils.coordinateStep
  val maxX = -73.70/HotcellUtils.coordinateStep
  val minY = 40.50/HotcellUtils.coordinateStep
  val maxY = 40.90/HotcellUtils.coordinateStep
  val minZ = 1
  val maxZ = 31
  val numCells = ((maxX - minX + 1)*(maxY - minY + 1)*(maxZ - minZ + 1)).toInt

  // Count points per cell
  pickupInfo.createOrReplaceTempView("cellCoordinates")
  val cellCountDf = spark.sql("select x, y, z, count(*) as count from cellCoordinates group by x, y, z")
  cellCountDf.createOrReplaceTempView("cellCounts")
  
  // Collect cell counts to driver for G* calculation
  // We need all cell values to calculate mean and std dev
  val cellCounts = cellCountDf.collect()
  val cellValuesMap = scala.collection.mutable.Map[(Int, Int, Int), Int]()
  
  // Build map of cell values (only cells with points)
  for (row <- cellCounts) {
    val x = row.getInt(0)
    val y = row.getInt(1)
    val z = row.getInt(2)
    val count = row.getLong(3).toInt
    cellValuesMap((x, y, z)) = count
  }
  
  // Calculate mean and standard deviation
  // Mean = sum of all cell values / number of cells
  // For cells without points, count is 0
  var sumValues = 0.0
  for ((key, value) <- cellValuesMap) {
    sumValues += value
  }
  // Cells without points have count 0, so total sum is just sum of cells with points
  // But we need to account for all cells in the range
  val mean = sumValues / numCells
  
  // Calculate standard deviation
  // S = sqrt(sum((xi - mean)^2) / n)
  var sumSquaredDiff = 0.0
  // For cells with points
  for ((key, value) <- cellValuesMap) {
    val diff = value - mean
    sumSquaredDiff += diff * diff
  }
  // For cells without points (they have value 0)
  val cellsWithPoints = cellValuesMap.size
  val cellsWithoutPoints = numCells - cellsWithPoints
  sumSquaredDiff += cellsWithoutPoints * mean * mean
  
  val variance = sumSquaredDiff / numCells
  val stdDev = Math.sqrt(variance)
  
  // Register UDF for calculating G* statistic
  // Pass min/max bounds so we can check all neighbors within valid range
  spark.udf.register("CalculateGStat", (x: Int, y: Int, z: Int) => {
    HotcellUtils.calculateGStatistic(x, y, z, cellValuesMap, mean, stdDev, numCells, minX.toInt, maxX.toInt, minY.toInt, maxY.toInt, minZ, maxZ)
  })
  
  // Calculate G* statistic for each cell, sort by G* score descending, and return top 50
  // Output only x, y, z (without G score)
  val finalResultDf = spark.sql("""
    select x, y, z
    from (
      select x, y, z, CalculateGStat(x, y, z) as gStat
      from cellCounts
    ) ordered
    order by gStat desc
    limit 50
  """)

  return finalResultDf
}
}
