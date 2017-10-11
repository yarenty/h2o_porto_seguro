package com.yarenty.h2o

import water.{Futures, Key}
import water.fvec._

object DataMunging {

  def processToInt(h2OFrame: H2OFrame, toProc: Array[String]): Unit = {
    for (col <- toProc) {
      h2OFrame.add(col + "_cat", toInt(h2OFrame.vec(col)))
      println("[toint] PROCSEED:" +col)
    }
  }
  
  def toInt(in: Vec): Vec = {
    val vec = Vec.makeZero(in.length)
    val vw = vec.open
    for (i <- 0 until in.length.toInt) {
      vw.set(i, (in.at(i) * 10).toInt )
    }
    vw.close()
    vec
  }

  def processPower(h2OFrame: H2OFrame, toProc: Array[String]): Unit = {
    for (col <- toProc) {
      h2OFrame.add(col + "_pow2", power(h2OFrame.vec(col)))
      println("[power] PROCSEED:" +col)
    }
  }

  def power(in: Vec): Vec = {
    val vec = Vec.makeZero(in.length)
    val vw = vec.open

    for (i <- 0 until in.length.toInt) {
      vw.set(i, in.at(i) * in.at(i))
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

    H2OFrame( new Frame(key, h2OFrame.names, vecs))

  }

}
