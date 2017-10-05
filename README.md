# H2O - sparkling Water Startup


## Dependencies
Sparkling Water 2.2.0 which integrates:
 - Spark 2.2.0
 - H2O 3.14.0.2 

## Status

- Step 1: Data preparation


## Project structure

```
├─ gradle/        - Gradle definition files
├─ src/           - Source code
│  ├─ main/       - Main implementation code 
│  │  ├─ scala/
│  ├─ test/       - Test code
│  │  ├─ scala/
├─ build.gradle   - Build file for this project
├─ gradlew        - Gradle wrapper 
```



## Project building

For building, please, use provided `gradlew` command:

```
./gradlew build
```

### Run
For running an application:

```
./gradlew run
```

## Running tests

To run tests, please, run:

```
./gradlew test
```



# Checking code style

To check codestyle:

```
./gradlew scalaStyle
```

## Creating and Running Spark Application

Create application assembly which can be directly submitted to Spark cluster:

```
./gradlew shadowJar
```

The command creates jar file `build/libs/porto_seguro.jar` containing all necessary classes to run application on top of Spark cluster.

## SBT
```
sbt assembly
```


Submit application to Spark cluster (in this case, local cluster is used):

```
export MASTER='local-cluster[3,2,1024]'
SPARK_HOME/bin/spark-submit --class com.yarenty.h2o.Main build/libs/porto_seguro.jar
```

