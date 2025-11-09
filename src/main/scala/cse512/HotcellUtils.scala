package cse512

/**
 * Hot Cell Analysis Utility Functions
 * Part of CSE 511 Project 2: Hot Spot Analysis
 * Author: Yuntao (Kevin) Tan
 * Institution: Arizona State University
 * Course: CSE 511 - Data Processing at Scale
 */

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar

object HotcellUtils {
  val coordinateStep = 0.01

  def CalculateCoordinate(inputString: String, coordinateOffset: Int): Int =
  {
    // Configuration variable:
    // Coordinate step is the size of each cell on x and y
    var result = 0
    coordinateOffset match
    {
      case 0 => result = Math.floor((inputString.split(",")(0).replace("(","").toDouble/coordinateStep)).toInt
      case 1 => result = Math.floor(inputString.split(",")(1).replace(")","").toDouble/coordinateStep).toInt
      // We only consider the data from 2009 to 2012 inclusively, 4 years in total. Week 0 Day 0 is 2009-01-01
      case 2 => {
        val timestamp = HotcellUtils.timestampParser(inputString)
        result = HotcellUtils.dayOfMonth(timestamp) // Assume every month has 31 days
      }
    }
    return result
  }

  def timestampParser (timestampString: String): Timestamp =
  {
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss")
    val parsedDate = dateFormat.parse(timestampString)
    val timeStamp = new Timestamp(parsedDate.getTime)
    return timeStamp
  }

  def dayOfYear (timestamp: Timestamp): Int =
  {
    val calendar = Calendar.getInstance
    calendar.setTimeInMillis(timestamp.getTime)
    return calendar.get(Calendar.DAY_OF_YEAR)
  }

  def dayOfMonth (timestamp: Timestamp): Int =
  {
    val calendar = Calendar.getInstance
    calendar.setTimeInMillis(timestamp.getTime)
    return calendar.get(Calendar.DAY_OF_MONTH)
  }

  // Check if two cells are neighbors (within 1 unit distance in x, y, z)
  // Two cells are neighbors if |x1-x2| <= 1 AND |y1-y2| <= 1 AND |z1-z2| <= 1
  def isNeighbor(x1: Int, y1: Int, z1: Int, x2: Int, y2: Int, z2: Int): Boolean = {
    val dx = Math.abs(x1 - x2)
    val dy = Math.abs(y1 - y2)
    val dz = Math.abs(z1 - z2)
    return (dx <= 1 && dy <= 1 && dz <= 1)
  }

  // Calculate Getis-Ord G* statistic for a cell
  // G* = (Σwj*xj - X̄*Σwj) / (S*√((n*Σwj² - (Σwj)²)/(n-1)))
  // Where:
  // - wj = 1 if cell j is a neighbor (within valid range), 0 otherwise
  // - xj = value (count) of cell j (0 if cell has no points)
  // - X̄ = mean of all cell values
  // - S = standard deviation of all cell values
  // - n = total number of cells
  def calculateGStatistic(
    x: Int, y: Int, z: Int,
    cellValues: scala.collection.mutable.Map[(Int, Int, Int), Int],
    mean: Double,
    stdDev: Double,
    numCells: Int,
    minX: Int, maxX: Int, minY: Int, maxY: Int, minZ: Int, maxZ: Int
  ): Double = {
    var sumWjXj = 0.0  // Sum of wj * xj (weighted sum of neighbor values)
    var sumWj = 0.0    // Sum of weights (number of neighbors within valid range)
    var sumWjSquared = 0.0  // Sum of wj^2 (same as sumWj since wj is 0 or 1)
    
    // Check all possible neighbors within 1 unit distance
    // A neighbor is within valid range AND |dx| <= 1, |dy| <= 1, |dz| <= 1
    for (dx <- -1 to 1) {
      for (dy <- -1 to 1) {
        for (dz <- -1 to 1) {
          val x2 = x + dx
          val y2 = y + dy
          val z2 = z + dz
          
          // Check if neighbor is within valid range
          if (x2 >= minX && x2 <= maxX && y2 >= minY && y2 <= maxY && z2 >= minZ && z2 <= maxZ) {
            // This is a valid neighbor
            sumWj += 1.0
            sumWjSquared += 1.0
            
            // Get cell value (0 if cell has no points)
            val cellValue = cellValues.getOrElse((x2, y2, z2), 0)
            sumWjXj += cellValue
          }
        }
      }
    }
    
    // Calculate numerator: Σwj*xj - X̄*Σwj
    val numerator = sumWjXj - (mean * sumWj)
    
    // Calculate denominator: S*√((n*Σwj² - (Σwj)²)/(n-1))
    val denominatorPart = (numCells * sumWjSquared - (sumWj * sumWj)) / (numCells - 1.0)
    
    // Avoid negative values under square root
    if (denominatorPart < 0.0) {
      return 0.0
    }
    
    val denominator = stdDev * Math.sqrt(denominatorPart)
    
    // Avoid division by zero
    if (denominator == 0.0) {
      return 0.0
    }
    
    // Calculate G* statistic
    val gStat = numerator / denominator
    return gStat
  }
}
