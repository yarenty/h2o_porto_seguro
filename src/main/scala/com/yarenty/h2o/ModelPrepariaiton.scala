package com.yarenty.h2o

import java.net.URI

import hex.Model.Parameters.CategoricalEncodingScheme
import hex.ScoreKeeper.StoppingMetric
import hex.deeplearning.{DeepLearning, DeepLearningModel}
import hex.deeplearning.DeepLearningModel.DeepLearningParameters
import hex.tree.xgboost.{XGBoost, XGBoostModel}
import hex.tree.xgboost.XGBoostModel.XGBoostParameters
import water.fvec.{H2OFrame, Vec}

/**
  * Created by yarenty on 20/06/17.
  */
object ModelPrepariaiton {


  def flow(): Unit = {

    val datadir = "/opt/data/porto_seguro"
    val trainFile = datadir + "/train.csv"
    val testFile = datadir + "/test.csv"


    val input = new H2OFrame(getSimpleCSVParser, new URI(trainFile))
    val test = new H2OFrame(getSimpleCSVParser, new URI(testFile))

    val processedNames = input.names.filter( n => n.contains("_cat") ) ++ Array("target")//|| n.contains("_bin") ) 
    println(processedNames mkString ",")

    input.colToEnum(processedNames)
    
    test.add(Array("target"), Array(test.vec("ps_calc_15_bin").makeCopy())) //just fake column otherwise XGBoost is blowing up ;-)
    test.colToEnum(processedNames)

    val (train, valid) = split(input,0.8) // this is split 0.8/0.2

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
    params._ntrees=200
    params._ignored_columns = Array("id")

    params._stopping_rounds = 5
    params._stopping_metric = StoppingMetric.AUC
    params._categorical_encoding = CategoricalEncodingScheme.OneHotInternal
    
    
    val dl = new XGBoost(params)
    dl.trainModel.get

  }

}
