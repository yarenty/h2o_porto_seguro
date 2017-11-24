package com.yarenty.h2o

import org.apache.commons.math3.stat.descriptive.rank.Median
import water.fvec._
import water.{Futures, Key}

object DataMunging {


  def processMedian(h2OFrame: H2OFrame): Unit = {
    for (n <- h2OFrame._names) {
      if (n.compareTo("id") != 0 && n.compareTo("target") != 0) {

        if (n.contains("_bin")) {

        } else {
          val median = new Median()
          median.setData(vecToArray(h2OFrame.vec(n)))
          median.evaluate()
          h2OFrame.add(n + "_median_range", calcRange(h2OFrame.vec(n), median.evaluate()))
          h2OFrame.add(n + "_mean_range", calcRange(h2OFrame.vec(n), h2OFrame.vec(n).mean))

        }
        println("[median] PROCESEED:" + n)

      }
    }
  }

  def processHOTEndcoder(h2OFrame: H2OFrame, names: Array[String]): Unit = {
    h2OFrame.colToEnum(names)
    for (n <- names) {
      val dom = h2OFrame.vec(n).domain()
      if (dom.length > 2 && dom.length < 7) {
        for (i <- 0 until dom.length) {
          h2OFrame.add(n + "_oh_" + i, calcOH(h2OFrame.vec(n), i))
          println(s"[hotone] PROCESSED: ${n} cardinal: ${dom.length}")
        }
      } else {
        println(s"[hotone] SKPIPPED: ${n} cardinal: ${dom.length}")
      }
    }

  }


  private def calcOH(in: Vec, v: Int): Vec = {
    val vec = Vec.makeZero(in.length)
    val vw = vec.open

    for (i <- 0 until in.length.toInt) {
      vw.set(i, if (in.at8(i) == v) 1 else 0)
    }
    vw.close()
    vec
  }


  private def calcRange(in: Vec, ran: Double): Vec = {
    val vec = Vec.makeZero(in.length)
    val vw = vec.open

    for (i <- 0 until in.length.toInt) {
      vw.set(i, if (in.at(i) > ran) 1 else 0)
    }
    vw.close()
    vec
  }


  def processToInt(h2OFrame: H2OFrame, toProc: Array[String]): Unit = {
    for (col <- toProc) {
      h2OFrame.add(col + "_cat", toInt(h2OFrame.vec(col)))
      println("[toint] PROCSEED:" + col)
    }
  }

  private def toInt(in: Vec): Vec

  = {
    val vec = Vec.makeZero(in.length)
    val vw = vec.open
    for (i <- 0 until in.length.toInt) {
      vw.set(i, (in.at(i) * 10).toInt)
    }
    vw.close()
    vec
  }

  def processPower(h2OFrame: H2OFrame, toProc: Array[String]): Unit = {
    for (col <- toProc) {
      h2OFrame.add(col + "_pow2", power(h2OFrame.vec(col)))
      println("[power] PROCSEED:" + col)
    }
  }

  private def power(in: Vec): Vec

  = {
    val vec = Vec.makeZero(in.length)
    val vw = vec.open

    for (i <- 0 until in.length.toInt) {
      vw.set(i, in.at(i) * in.at(i))
    }
    vw.close()
    vec
  }


  def processMultiply(h2OFrame: H2OFrame): Unit = {
    h2OFrame.add("ps_car_13_x_ps_reg_03", multiplyVec(h2OFrame.vec("ps_car_13"), h2OFrame.vec("ps_reg_03")))
    println("[multiply] PROCSEED: ps_car_13_x_ps_reg_03")
  }

  def multiplyVec(a: Vec, b: Vec): Vec = {
    val vec = Vec.makeZero(a.length)
    val vw = vec.open

    for (i <- 0 until a.length.toInt) {
      vw.set(i, a.at(i) * b.at(i))
    }
    vw.close()
    vec
  }

  def processNANCat(h2OFrame: H2OFrame, toProc: Array[String]): Unit = {
    for (col <- toProc) {
      val v = nanToCat(h2OFrame.vec(col))
      h2OFrame.remove(col)
      h2OFrame.add(col, v)
      println("[NAN-to-CAT] PROCSEED:" + col)
    }
  }

  private def nanToCat(in: Vec): Vec = {
    val vec = Vec.makeZero(in.length)
    val vw = vec.open
    for (i <- 0 until in.length.toInt) {
      if (in.at(i).isNaN)
        vw.set(i, -1)
      else
        vw.set(i, in.at(i))
    }
    vw.close()
    vec
  }


  def processNANMean(h2OFrame: H2OFrame, toProc: Array[String]): Unit = {
    for (col <- toProc) {
      val v = nanToMean(h2OFrame.vec(col))
      h2OFrame.remove(col)
      h2OFrame.add(col, v)
      println("[NAN-to-CAT] PROCSEED:" + col)
    }
  }

  private def nanToMean(in: Vec): Vec = {
    val vec = Vec.makeZero(in.length)
    val vw = vec.open
    val mm = in.mean
    for (i <- 0 until in.length.toInt) {
      if (in.at(i).isNaN)
        vw.set(i, mm)
      else
        vw.set(i, in.at(i))
    }
    vw.close()
    vec
  }

  def processNANToMinusOne(h2OFrame: H2OFrame, toProc: Array[String]): Unit = {
    for (col <- toProc) {
      val v = nanToMean(h2OFrame.vec(col))
      h2OFrame.remove(col)
      h2OFrame.add(col, v)
      println("[NAN-to-CAT] PROCSEED:" + col)
    }
  }


  private def nanToMinusOne(in: Vec): Vec

  = {
    val vec = Vec.makeZero(in.length)
    val vw = vec.open
    for (i <- 0 until in.length.toInt) {
      if (in.at(i).isNaN)
        vw.set(i, -1.0)
      else
        vw.set(i, in.at(i))
    }
    vw.close()
    vec
  }


  def targetSelector(h2OFrame: H2OFrame): H2OFrame = {

    val targetVecName = "target"
    val targetVec = h2OFrame.vec(targetVecName)
    val len = h2OFrame.names.length

    val fs = new Array[Futures](len)
    val av = new Array[AppendableVec](len)
    val chunks = new Array[NewChunk](len)
    val vecs = new Array[Vec](len)


    for (i <- 0 until len) {
      fs(i) = new Futures()
      av(i) = new AppendableVec(new Vec.VectorGroup().addVec(), Vec.T_NUM)
      chunks(i) = new NewChunk(av(i), 0)
    }

    for (idx <- 0 until len) {
      for (i <- 0 until targetVec.length.toInt) {
        if (targetVec.at(i) == 1.0)
          chunks(idx).addNum(h2OFrame.vec(idx).at(i))
      }
    }

    for (i <- 0 until len) {
      chunks(i).close(0, fs(i))
      vecs(i) = av(i).layout_and_close(fs(i))
      fs(i).blockForPending()
    }

    val key = Key.make("ONCES").asInstanceOf[Key[Frame]]

    H2OFrame(new Frame(key, h2OFrame.names, vecs))

  }

}
