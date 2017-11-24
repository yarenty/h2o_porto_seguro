package com.yarenty.h2o

import breeze.linalg._
import breeze.plot._
import water.fvec.Vec


object Visualization {

//  val input = ModelPrepariaiton.input
//  val onces = ModelPrepariaiton.onces

  def h2oVec2DenseVec(v: Vec): DenseVector[Double] = {
    new DenseVector(vecToArray(v).sorted)
  }

//
//  def do_pictures(xName: String) = {
//    val f = Figure()
//    val p = f.subplot(0)
//
//    val y = h2oVec2DenseVec(input.vec(xName))
//    val x = new DenseVector(Range.Double(0.0, input.vec(xName).length.toDouble, 1.0).toArray)
//    p += plot(x, y)
//
//    p.xlabel = "no"
//    p.ylabel = xName
//
//    val p2 = f.subplot(2, 1, 1)
//    p2 += hist(y, 100)
//    p2.title = "distribution"
//
//    f.saveas(xName + ".png")
//    println(xName + "PNG output")
//
//
//    val f2 = Figure()
//    val p3 = f2.subplot(0)
//
//    val y2 = h2oVec2DenseVec(onces.vec(xName))
//    val x2 = new DenseVector(Range.Double(0.0, onces.vec(xName).length.toDouble, 1.0).toArray)
//    p3 += plot(x2, y2)
//
//    p3.xlabel = "no"
//    p3.ylabel = xName
//
//    val p4 = f2.subplot(2, 1, 1)
//    p4 += hist(y2, 100)
//    p4.title = "distribution"
//
//    f2.saveas(xName + "___1.png")
//    println(xName + "PNG 111 output")
//  }


//  Array("ps_reg_03","ps_car_12", "ps_car_11_cat", "ps_car_11").foreach(do_pictures)
  

}


