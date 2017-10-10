package com.yarenty.h2o

import java.net.URI

import hex.Model.Parameters.CategoricalEncodingScheme
import hex.ScoreKeeper.StoppingMetric
import hex.deeplearning.{DeepLearning, DeepLearningModel}
import hex.deeplearning.DeepLearningModel.DeepLearningParameters
import hex.tree.xgboost.{XGBoost, XGBoostModel}
import hex.tree.xgboost.XGBoostModel.XGBoostParameters
import water.fvec.Frame.DeepSelect
import water.fvec.{H2OFrame, Vec}

/**
  * Created by yarenty on 20/06/17.
  */
object ModelPrepariaiton {

  val datadir = "/opt/data/porto_seguro"
  val trainFile = datadir + "/train.csv"
  val testFile = datadir + "/test.csv"

  val input = new H2OFrame(getSimpleCSVParser, new URI(trainFile))
  val test = new H2OFrame(getSimpleCSVParser, new URI(testFile))
  val onces = DataMunging.targetSelector(input)
  
  // Visualization
  
  def flow(): Unit = {
    
    val processedNames = input.names.filter(n => n.contains("_cat") || n.contains("_bin")) ++ Array("target",
      "ps_ind_01", "ps_ind_03", "ps_ind_14","ps_ind_15", 
      "ps_reg_01", "ps_reg_02", 
      "ps_car_11", "ps_car_15",
      "ps_calc_01", "ps_calc_02", "ps_calc_03","ps_calc_04", "ps_calc_05",
      "ps_calc_11", "ps_calc_12", "ps_calc_13", "ps_calc_14"
    ) 
    
    println(processedNames mkString ",")

    input.colToEnum(processedNames)
    input.remove("id")
    val tobeproc = input.names.filter(n => !processedNames.contains(n) && n != "id")

    println("TO BE PROCESSED:" + tobeproc.mkString)


    test.add(Array("target"), Array(test.vec("ps_calc_15_bin").makeCopy())) //just fake column otherwise XGBoost is blowing up ;-)
    test.colToEnum(processedNames)
    test.remove("id")

    DataMunging.processPower(input, tobeproc)
    DataMunging.processPower(test, tobeproc)


    val (train, valid) = split(input, 0.8) // this is split 0.8/0.2

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
    params._ntrees = 50
    params._eta = 0.3
    params._learn_rate = 0.3
    params._max_depth = 3
    params._reg_alpha = 0.01f
    params._reg_lambda = 0.01f
    //    params._ignored_columns = Array("id")

    params._stopping_rounds = 5
    params._stopping_metric = StoppingMetric.AUC
    params._categorical_encoding = CategoricalEncodingScheme.OneHotExplicit

    val dl = new XGBoost(params)
    dl.trainModel.get

  }

}
