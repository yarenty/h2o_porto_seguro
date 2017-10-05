package com.yarenty.h2o

import java.net.URI

import hex.deeplearning.{DeepLearning, DeepLearningModel}
import hex.deeplearning.DeepLearningModel.DeepLearningParameters
import hex.tree.xgboost.{XGBoost, XGBoostModel}
import hex.tree.xgboost.XGBoostModel.XGBoostParameters
import water.fvec.H2OFrame

/**
  * Created by yarenty on 20/06/17.
  */
object ModelPrepariaiton {


  def flow(): Unit = {

    val datadir = "/opt/data/porto_seguro"
    val trainFile = datadir + "/train.csv"
    val testFile = datadir + "/test.csv"


    // We do not need to wait for H2O cloud since it will be launched by backend
    val input = new H2OFrame(getSimpleCSVParser, new URI(trainFile))
    val test = new H2OFrame(getSimpleCSVParser, new URI(testFile))

    val processedNames = input.names.filter( n => n.contains("_cat") || n.contains("_bin") )
    println(processedNames mkString ",")

    //to make sure that they are enums (difefrent versions of default parsers in different releases h2o behave... differently ;-)
    input.colToEnum(processedNames)
    test.colToEnum(processedNames)


    val (train, valid) = split(input,0.8) // this is split 089/0.1

    val model = dlModel(H2OFrame(train), H2OFrame(valid))
    println(model)

    val prediction = model.score(test)

    //test.delete()
    saveCSV(prediction, datadir + "/out.csv")
  }



  private def dlModel(train: H2OFrame, valid: H2OFrame): XGBoostModel = {
    val params = new XGBoostParameters()
    params._train = train.key
    params._valid = valid.key
    params._response_column = "target"
//    params._ntrees=100
    params._ignored_columns = Array("id")

    
    val dl = new XGBoost(params)
    dl.trainModel.get

  }

}
