package cse512

/**
 * Hot Zone Analysis Utility Functions
 * Part of CSE 511 Project 2: Hot Spot Analysis
 * Author: Yuntao (Kevin) Tan
 * Institution: Arizona State University
 * Course: CSE 511 - Data Processing at Scale
 */

object HotzoneUtils {

  def ST_Contains(queryRectangle: String, pointString: String ): Boolean = {
    // Parse rectangle coordinates: format is "minLon,minLat,maxLon,maxLat"
    // Example: "-74.189999,40.671001,-74.153071,40.707982"
    val rectCoords = queryRectangle.split(",")
    if (rectCoords.length != 4) {
      return false
    }
    
    val minLon = rectCoords(0).toDouble
    val minLat = rectCoords(1).toDouble
    val maxLon = rectCoords(2).toDouble
    val maxLat = rectCoords(3).toDouble
    
    // Parse point coordinates: format is "(lon,lat)"
    // Example: "(-74.001580000000004,40.719382000000003)"
    val cleanedPoint = pointString.replace("(", "").replace(")", "")
    val pointCoords = cleanedPoint.split(",")
    if (pointCoords.length != 2) {
      return false
    }
    
    val pointLon = pointCoords(0).toDouble
    val pointLat = pointCoords(1).toDouble
    
    // Check if point is within rectangle boundaries
    // Point is contained if: minLon <= pointLon <= maxLon AND minLat <= pointLat <= maxLat
    val isContained = (pointLon >= minLon && pointLon <= maxLon) && 
                      (pointLat >= minLat && pointLat <= maxLat)
    
    return isContained
  }

}
