package com.yarenty

import java.io.{File, PrintWriter}

import hex.FrameSplitter
import org.apache.commons.io.FilenameUtils
import water.Key
import water.fvec.{Frame, H2OFrame, Vec}
import water.parser.{DefaultParserProviders, ParseSetup}
import water.util.Log

import scala.reflect.io.Directory

/**
  * Created by yarenty on 16/06/2017.
  */
package object h2o {


  def getSimpleCSVParser: ParseSetup = {
    val p = new ParseSetup()
    p.setParseType(DefaultParserProviders.CSV_INFO)
    p.setSeparator(44)
    p.setSingleQuotes(false)
    p.setCheckHeader(1)
    p
  }

  def saveCSV(f: Frame, fileName: String): Unit = {
    Log.debug("CSV export::" + fileName)
    val csv = f.toCSV(true, false)
    val csv_writer = new PrintWriter(new File(fileName))
    while (csv.available() > 0) {
      csv_writer.write(csv.read.toChar)
    }
    csv_writer.close()
  }


  def createOutputDirectory(fileName: String, force: Boolean = false): Boolean = {
    val dir = FilenameUtils.getFullPathNoEndSeparator(fileName)
    Log.debug(s"Create output directory: $dir")
    val out = Directory(dir)
    out.createDirectory(force = force)
    if (force && !out.exists) {
      Log.err(s"Could not create output directory: $dir")
      System.exit(-1)
    }
    out.exists
  }


  def vecToArray(v: Vec): Array[Double] = {
    val arr = Array.ofDim[Double](v.length.toInt)
    for (i <- 0 until v.length.toInt) {
      arr(i) = v.at(i)
    }
    arr
  }


  def arrayToVec(arr: Array[Double]): Vec = {
    val vec = Vec.makeZero(arr.length)
    val vw = vec.open

    for (i <- arr.indices) {
      vw.set(i, arr(i))
    }
    vw.close()
    vec
  }


  def arrayToTimeVec(arr: Array[Long]): Vec = {
    val vec = Vec.makeZero(arr.length, Vec.T_TIME)
    val vw = vec.open

    for (i <- arr.indices) {
      vw.set(i, arr(i))
    }
    vw.close()
    vec
  }


  def split(in: H2OFrame, ratio: Double): (Frame, Frame) = {

    val keys = Array[String]("train_"+ratio, "test_"+ratio)
    val ratios = Array[Double](ratio)

    val frs = split(in, keys, ratios)
    (frs(0), frs(1))
  }


  def split[T <: Frame](fr: T, keys: Seq[String], ratios: Seq[Double]): Array[Frame] = {
    val ks = keys.map(Key.make[Frame](_)).toArray
    val splitter = new FrameSplitter(fr, ratios.toArray, ks, null)
    water.H2O.submitTask(splitter)
    // return results
    splitter.getResult
  }


  def memoryInfo(point:String): Unit = {
    // memory info
    val mb = 1024 *1024
    val runtime = Runtime.getRuntime
    Log.err(s"[$point]** Used Memory:  " + (runtime.totalMemory - runtime.freeMemory) / mb +"MB")
    Log.err(s"[$point]** Free Memory:  " + runtime.freeMemory / mb +"MB" )
    Log.err(s"[$point]** Total Memory: " + runtime.totalMemory / mb +"MB")
    Log.err(s"[$point]** Max Memory:   " + runtime.maxMemory / mb +"MB")
  }
  

}
