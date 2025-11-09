# CSE511 Project 2: Hot Spot Analysis

**Author**: Yuntao (Kevin) Tan  
**Institution**: Arizona State University  
**Course**: CSE 511 - Data Processing at Scale

## 📋 Table of Contents
- [Project Overview](#-project-overview)
- [Introduction](#-introduction)
- [Project Structure](#-project-structure)
- [Implementation Details](#-implementation-details)
- [Getting Started](#-getting-started)
- [Usage](#-usage)
- [Algorithm Explanations](#-algorithm-explanations)
- [Output Format](#-output-format)
- [Testing](#-testing)
- [Troubleshooting](#-troubleshooting)
- [Additional Resources](#-additional-resources)
- [Version History](#-version-history)
- [Contributing](#-contributing)
- [License](#-license)
- [Acknowledgments](#-acknowledgments)

---

## 🎯 Project Overview

This project implements **spatial hot spot analysis** on large-scale geospatial datasets using Apache Spark and Scala. The project consists of two main analysis tasks:

1. **Hot Zone Analysis**: Performs range join operations to identify rectangles (zones) with high point density
2. **Hot Cell Analysis**: Applies Getis-Ord G* statistic to identify statistically significant spatial hot spots in spatio-temporal data

### Key Technologies
- **Apache Spark 2.2.0**: Distributed data processing framework
- **Scala 2.11.11**: Programming language
- **Spark SQL**: For efficient data querying and transformations
- **SBT**: Build tool for Scala projects

---

## 📖 Introduction

### What is Hot Spot Analysis?

Hot spot analysis is a spatial analysis technique used to identify areas with statistically significant clustering of events or phenomena. This project applies this concept to NYC taxi trip data to discover:

- **Hot Zones**: Geographic rectangles with high concentrations of taxi pickups
- **Hot Cells**: Spatio-temporal cells (3D: longitude, latitude, time) that show statistically significant clustering patterns

### Use Cases

- **Urban Planning**: Identify high-traffic areas for infrastructure development
- **Transportation**: Optimize taxi fleet distribution based on demand patterns
- **Business Intelligence**: Discover prime locations for business placement
- **Public Safety**: Identify crime or accident hotspots

---

## 📁 Project Structure

```
CSE511-Project-Hotspot-Analysis/
├── src/
│   └── main/
│       ├── scala/
│       │   └── cse512/
│       │       ├── Entrance.scala          # Main entry point, argument parser
│       │       ├── HotzoneAnalysis.scala   # Hot zone analysis implementation
│       │       ├── HotzoneUtils.scala      # Utility functions for hot zone
│       │       ├── HotcellAnalysis.scala   # Hot cell analysis implementation
│       │       └── HotcellUtils.scala      # Utility functions for hot cell
│       └── resources/
│           ├── point_hotzone.csv           # Sample point data
│           ├── yellow_trip_sample_100000.csv  # Sample taxi trip data
│           └── zone-hotzone.csv            # Sample rectangle/zone data
├── testcase/
│   ├── hotzone/
│   │   ├── hotzone-example-input.txt       # Example input command
│   │   └── hotzone-example-answer.csv      # Expected output
│   └── hotcell/
│       ├── hotcell-example-input.txt       # Example input command
│       ├── hotcell-example-answer.csv      # Expected output (top 50)
│       └── hotcell-example-answer-withZscore.csv  # Output with G* scores
├── target/
│   └── scala-2.11/
│       └── CSE511-Hotspot-Analysis-Template-assembly-0.1.0.jar  # Compiled JAR (matches build.sbt)
├── build.sbt                               # Build configuration
└── README.md                               # This file
```

---

## 🔧 Implementation Details

### Hot Zone Analysis

**Purpose**: Count the number of points (taxi pickups) within each rectangle (zone).

**Implementation**:
- **File**: `HotzoneUtils.scala` and `HotzoneAnalysis.scala`
- **Key Function**: `ST_Contains(queryRectangle, pointString)`
  - Parses rectangle coordinates: `"minLon,minLat,maxLon,maxLat"`
  - Parses point coordinates: `"(lon,lat)"`
  - Checks if point is within rectangle boundaries
- **Process**:
  1. Load point and rectangle datasets
  2. Perform spatial join using `ST_Contains` UDF
  3. Group by rectangle and count points
  4. Sort by rectangle string in ascending order

**Algorithm Complexity**: O(n × m) where n = number of points, m = number of rectangles

### Hot Cell Analysis

**Purpose**: Identify statistically significant hot spots using Getis-Ord G* statistic.

**Implementation**:
- **File**: `HotcellUtils.scala` and `HotcellAnalysis.scala`
- **Key Functions**:
  - `CalculateCoordinate()`: Converts lat/lon/time to cell coordinates (x, y, z)
  - `isNeighbor()`: Checks if two cells are neighbors (within 1 unit in all dimensions)
  - `calculateGStatistic()`: Computes Getis-Ord G* statistic

**Getis-Ord G* Formula**:
```
G* = (Σwj×xj - X̄×Σwj) / (S×√((n×Σwj² - (Σwj)²)/(n-1)))
```

Where:
- `wj` = 1 if cell j is a neighbor, 0 otherwise
- `xj` = value (count) of cell j
- `X̄` = mean of all cell values
- `S` = standard deviation of all cell values
- `n` = total number of cells

**Process**:
1. Convert pickup locations to 3D cell coordinates (x, y, z)
   - x, y: Spatial coordinates (0.01° × 0.01° cells)
   - z: Temporal coordinate (day of month, 1-31)
2. Count points per cell
3. Calculate mean and standard deviation for all cells (including empty cells)
4. For each cell with points:
   - Find all neighbors (within 1 unit in x, y, z)
   - Calculate G* statistic
5. Sort by G* score (descending)
6. Return top 50 hottest cells

**Spatial Bounds**:
- Longitude: -74.50° to -73.70° (x: -7450 to -7370)
- Latitude: 40.50° to 40.90° (y: 4050 to 4090)
- Time: Day 1 to 31 (z: 1 to 31)

**Algorithm Complexity**: O(k × m) where k = cells with points, m = average neighbors per cell (typically 27)

---

## 🚀 Getting Started

### Prerequisites

1. **Java JDK 8+**: Required for Scala and Spark
   ```bash
   java -version
   ```

2. **Scala Build Tool (SBT)**: Install from [scala-sbt.org](https://www.scala-sbt.org/)
   ```bash
   sbt --version
   ```

3. **Apache Spark 2.2.0**: Download from [spark.apache.org](https://spark.apache.org/downloads.html)
   - Extract and set `SPARK_HOME` environment variable
   - Add `$SPARK_HOME/bin` to your PATH

### Building the Project

1. **Navigate to project directory**:
   ```bash
   cd CSE511-Project-Hotspot-Analysis
   ```

2. **Clean and build**:
   ```bash
   sbt clean assembly
   ```

3. **Find the compiled JAR**:
   ```
   target/scala-2.11/CSE511-Hotspot-Analysis-Template-assembly-0.1.0.jar
   ```

### IDE Setup (Optional)

For debugging in IntelliJ IDEA or other IDEs:

1. **Add Scala plugin** to your IDE
2. **Modify `Entrance.scala`** temporarily:
   ```scala
   .config("spark.some.config.option", "some-value").master("local[*]")
   ```
3. **Modify `build.sbt`** temporarily:
   ```scala
   "org.apache.spark" %% "spark-core" % "2.2.0" % "compile"  // Change from "provided"
   ```
4. **Run and debug** in IDE
5. **Revert changes** before building for Spark submission

---

## 💻 Usage

### Command Format

```bash
spark-submit <jar-path> <output-path> <task1> <task1-params> [<task2> <task2-params> ...]
```

### Parameters

- **Output path** (Mandatory): Directory where results will be saved
- **Task name**: `hotzoneanalysis` or `hotcellanalysis`
- **Task parameters**:
  - **Hot Zone**: 2 parameters
    1. Point data path (CSV file with taxi pickup locations)
    2. Zone/Rectangle data path (CSV file with rectangle coordinates)
  - **Hot Cell**: 1 parameter
    1. Taxi trip data path (CSV file with monthly trip data)

### Examples

#### Example 1: Hot Zone Analysis Only

```bash
spark-submit \
  target/scala-2.11/CSE511-Hotspot-Analysis-Template-assembly-0.1.0.jar \
  test/output \
  hotzoneanalysis \
  src/resources/point_hotzone.csv \
  src/resources/zone-hotzone.csv
```

#### Example 2: Hot Cell Analysis Only

```bash
spark-submit \
  target/scala-2.11/CSE511-Hotspot-Analysis-Template-assembly-0.1.0.jar \
  test/output \
  hotcellanalysis \
  src/resources/yellow_trip_sample_100000.csv
```

#### Example 3: Both Analyses

```bash
spark-submit \
  target/scala-2.11/CSE511-Hotspot-Analysis-Template-assembly-0.1.0.jar \
  test/output \
  hotzoneanalysis \
  src/resources/point_hotzone.csv \
  src/resources/zone-hotzone.csv \
  hotcellanalysis \
  src/resources/yellow_trip_sample_100000.csv
```

### Input Data Format

#### Point Data (Hot Zone & Hot Cell)
- **Format**: CSV with semicolon delimiter
- **Column 5** (`_c5`): Point coordinates as `"(longitude,latitude)"`
- **Column 1** (`_c1`): Timestamp (for hot cell analysis) as `"yyyy-MM-dd hh:mm:ss"`
- **Example**:
  ```
  DDS;2009-01-24 16:18:23;...;(-74.00158,40.71938);...
  ```

#### Zone Data (Hot Zone Only)
- **Format**: CSV with tab delimiter
- **Column 0** (`_c0`): Rectangle coordinates as `"minLon,minLat,maxLon,maxLat"`
- **Example**:
  ```
  -74.189999,40.671001,-74.153071,40.707982
  ```

---

## 📊 Output Format

### Hot Zone Analysis Output

**Format**: CSV with rectangle string and count, sorted by rectangle string (ascending)

```
"-73.789411,40.666459,-73.756364,40.680494",1
"-73.793638,40.710719,-73.752336,40.730202",1
"-73.832707,40.620010,-73.746541,40.665414",20
```

**Interpretation**: Each line shows a rectangle and the number of points it contains.

### Hot Cell Analysis Output

**Format**: CSV with x, y, z coordinates (top 50), sorted by G* score (descending)

```
-7399,4075,15
-7399,4075,29
-7399,4075,22
```

**Interpretation**: 
- Each line represents a hot cell (x, y, z)
- x, y: Spatial coordinates (scaled by 0.01)
- z: Day of month (1-31)
- Cells are ordered from hottest to least hot

---

## 🧪 Testing

### Test Cases

Example test cases are provided in the `testcase/` directory:

1. **Hot Zone Test**:
   ```bash
   # Input command in: testcase/hotzone/hotzone-example-input.txt
   # Expected output in: testcase/hotzone/hotzone-example-answer.csv
   ```

2. **Hot Cell Test**:
   ```bash
   # Input command in: testcase/hotcell/hotcell-example-input.txt
   # Expected output in: testcase/hotcell/hotcell-example-answer.csv
   ```

### Running Tests

1. **Run hot zone test**:
   ```bash
   spark-submit \
     target/scala-2.11/CSE511-Hotspot-Analysis-Template-assembly-0.1.0.jar \
     test/output/hotzone \
     hotzoneanalysis \
     src/resources/point_hotzone.csv \
     src/resources/zone-hotzone.csv
   ```

2. **Compare output**:
   ```bash
   diff test/output/hotzone/part-*.csv testcase/hotzone/hotzone-example-answer.csv
   ```

3. **Run hot cell test**:
   ```bash
   spark-submit \
     target/scala-2.11/CSE511-Hotspot-Analysis-Template-assembly-0.1.0.jar \
     test/output/hotcell \
     hotcellanalysis \
     src/resources/yellow_trip_sample_100000.csv
   ```

---

## 🔍 Algorithm Explanations

### Spatial Join (Hot Zone)

The hot zone analysis uses a **range join** operation:

1. **Spatial Predicate**: `ST_Contains(rectangle, point)`
   - Checks if point's longitude is between rectangle's min/max longitude
   - Checks if point's latitude is between rectangle's min/max latitude

2. **Optimization**: Uses Spark SQL's distributed join capabilities
   - Points and rectangles are partitioned across cluster
   - Join is performed in parallel

### Getis-Ord G* Statistic (Hot Cell)

The G* statistic measures **local spatial autocorrelation**:

1. **Neighborhood Definition**: 
   - A cell's neighbors are all cells within 1 unit in x, y, and z dimensions
   - Maximum 27 neighbors (3×3×3 cube minus center cell)

2. **Statistical Significance**:
   - **Positive G***: Indicates hot spot (high values surrounded by high values)
   - **Negative G***: Indicates cold spot (low values surrounded by low values)
   - **Near zero**: No significant clustering

3. **Why G*?**:
   - Accounts for spatial autocorrelation
   - Provides statistical significance testing
   - Identifies true hot spots vs. random clustering

### Performance Considerations

- **Hot Zone**: O(n × m) - Can be optimized with spatial indexing (not implemented)
- **Hot Cell**: 
  - Cell counting: O(n) where n = number of points
  - G* calculation: O(k × 27) where k = cells with points
  - Most expensive: Collecting cell counts to driver for G* calculation

---

## 🐛 Troubleshooting

### Common Issues

#### 1. **Compilation Errors**

**Error**: `type mismatch; found: Double, required: Int`

**Solution**: Ensure `numCells` is converted to Int:
```scala
val numCells = ((maxX - minX + 1)*(maxY - minY + 1)*(maxZ - minZ + 1)).toInt
```

#### 2. **ClassNotFoundException**

**Error**: `java.lang.ClassNotFoundException: cse512.Entrance`

**Solution**: 
- Ensure JAR is built with `sbt clean assembly`
- Check that main class is set in `build.sbt`: `mainClass := Some("cse512.Entrance")`

#### 3. **Out of Memory**

**Error**: `java.lang.OutOfMemoryError`

**Solution**: Increase Spark executor memory:
```bash
spark-submit --executor-memory 4g --driver-memory 2g ...
```

#### 4. **File Not Found**

**Error**: `FileNotFoundException`

**Solution**: 
- Use absolute paths or paths relative to Spark working directory
- Ensure input files exist and are accessible

#### 5. **Incorrect Output Format**

**Issue**: Output doesn't match expected format

**Solution**:
- Check sorting order (hot zone: ascending by rectangle, hot cell: descending by G*)
- Verify coordinate parsing (check for parentheses, commas)
- Ensure only top 50 cells are returned for hot cell analysis

### Debugging Tips

1. **Enable Spark UI**: Add `--conf spark.eventLog.enabled=true` to see job details
2. **Check logs**: Look for errors in Spark driver/executor logs
3. **Test with small data**: Use sample files first before running on full dataset
4. **Validate input**: Ensure input files match expected format

---

## 📚 Additional Resources

### Documentation
- [Apache Spark Documentation](https://spark.apache.org/docs/2.2.0/)
- [Scala Documentation](https://docs.scala-lang.org/)
- [Getis-Ord G* Statistic](https://pro.arcgis.com/en/pro-app/latest/tool-reference/spatial-statistics/h-how-hot-spot-analysis-getis-ord-gi-spat-stati.htm)

### Data Sources
- [NYC Taxi Trip Data (S3)](https://datasyslab.s3.amazonaws.com/index.html?prefix=nyctaxitrips/)
- [ACM SIGSPATIAL GISCUP 2016](http://sigspatial2016.sigspatial.org/giscup2016/problem)

### Related Projects
- This project is based on ACM SIGSPATIAL GISCUP 2016 challenge
- Part of CSE 511: Data Processing at Scale course

---

## 📝 Version History

- **v1.1** (Nov 16): Fixed bug in `Entrance.scala`
- **v1.0** (Nov 13): Initial version with template code
- **Current**: Full implementation of hot zone and hot cell analysis

---

## 👥 Contributing

This is a course project. For questions or issues:
1. Check the troubleshooting section
2. Review test cases in `testcase/` directory
3. Consult course materials and documentation

---

## 📄 License

This project is part of an academic course. Please refer to your course guidelines for usage and distribution policies.

---

## 🙏 Acknowledgments

- **Author**: Yuntao (Kevin) Tan
- **Institution**: Arizona State University
- **Course**: CSE 511 - Data Processing at Scale
- **Challenge**: ACM SIGSPATIAL GISCUP 2016
- **Framework**: Apache Spark
- **Data**: NYC Taxi and Limousine Commission

---

**Last Updated**: 2025

**Author & Maintainer**: Yuntao (Kevin) Tan - Arizona State University
