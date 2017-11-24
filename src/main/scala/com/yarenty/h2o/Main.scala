package com.yarenty.h2o

import water.H2OStarter
import water.util.Log

/**
  * Created by yarenty on 15/06/2017.
  */
object Main extends H2OStarter {


  def main(args: Array[String]): Unit = {


    H2OStarter.start(args, System.getProperty("user.dir"))
    println("Hello World!")
    Log.info("Hello World - using H2O logger")


    memoryInfo("start")
//    ModelPrepariaiton.flow()
    ModelPrepariaiton.buildModel()
    memoryInfo("finish")
    // Shutdown Spark cluster and H2O
    //    shutdown()
  }

}