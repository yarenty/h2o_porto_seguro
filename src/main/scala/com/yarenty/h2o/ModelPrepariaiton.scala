package com.yarenty.h2o

import java.net.URI

import hex.Model.Parameters.CategoricalEncodingScheme
import hex.ScoreKeeper.StoppingMetric
import hex.deeplearning.{DeepLearning, DeepLearningModel}
import hex.deeplearning.DeepLearningModel.DeepLearningParameters
import hex.tree.xgboost.{XGBoost, XGBoostModel}
import hex.tree.xgboost.XGBoostModel.XGBoostParameters
import hex.tree.xgboost.XGBoostModel.XGBoostParameters.Backend
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
    
    
    val vecNanToMean = Array ("ps_reg_03","ps_car_11","ps_car_12","ps_car_14")
    println("TO BE NAN replaced by mean:" + vecNanToMean.mkString(","))
    DataMunging.processNANMean(input, vecNanToMean)
    DataMunging.processNANMean(test, vecNanToMean)

    
    val vecToInts = Array("ps_reg_01", "ps_reg_02", "ps_calc_01", "ps_calc_02", "ps_calc_03")
    println("TO BE INTED:" + vecToInts.mkString)
    DataMunging.processToInt(input, vecToInts)
    DataMunging.processToInt(test, vecToInts)

    
    val tobePowered =  Array("ps_car_12", "ps_car_14", "ps_car_15")
    println("TO BE POWERED:" + tobePowered.mkString(","))
    DataMunging.processPower(input, tobePowered)
    DataMunging.processPower(test, tobePowered)


    val vecNanToCat = input.names.filter(n => n.contains("_cat"))
    println("TO BE NAN replaced as new category:" + vecNanToCat.mkString(","))
    DataMunging.processNANCat(input, vecNanToCat)
    DataMunging.processNANCat(test, vecNanToCat)

    
    val toBeEnums = input.names.filter(n => n.contains("_cat")) ++ Array("target", //|| n.contains("_bin")
      //      "ps_ind_01", "ps_ind_03", "ps_ind_14","ps_ind_15", 
      //      "ps_reg_01", "ps_reg_02",
      //      "ps_car_11", "ps_car_15",
      "ps_calc_01", "ps_calc_02", "ps_calc_03","ps_calc_04", "ps_calc_05",
      "ps_calc_06", "ps_calc_07", "ps_calc_08","ps_calc_09", "ps_calc_10",
      "ps_calc_11", "ps_calc_12", "ps_calc_13", "ps_calc_14"
      //      "ps_car_15_pow2"
    )
    
    println("TO BE ENUMED:" + toBeEnums.mkString(","))
    
    input.colToEnum(toBeEnums)
    test.add(Array("target"), Array(test.vec("ps_calc_15_bin").makeZero())) //just fake column otherwise XGBoost is blowing up ;-)
    test.colToEnum(toBeEnums)

    
    
    val toRemove = Array("id",
      "ps_car_03_cat", "ps_car_05_cat" // to many missing values no point
    ) 
    println("TO BE Removed:" + toRemove.mkString(","))
    input.remove(toRemove)
    test.remove(toRemove)
    



    val (train, valid) = split(input, 0.8) // this is split 0.8/0.2

    val model = dlModel(H2OFrame(train), H2OFrame(valid))
    println(model)

    val prediction = model.score(test)

    
    //test.delete()
    saveCSV(prediction, datadir + "/out.csv")
  }



  private def dlModel(train: H2OFrame, valid: H2OFrame): DeepLearningModel = {
    val params = new DeepLearningParameters()
    params._train = train.key
    params._valid = valid.key
    params._response_column = "target"


    params._epochs = 1
    params._stopping_rounds = 5
    params._stopping_metric = StoppingMetric.AUC
//    params._categorical_encoding = CategoricalEncodingScheme.OneHotExplicit

    val dl = new DeepLearning(params)
    dl.trainModel.get

  }

  private def xgbModel(train: H2OFrame, valid: H2OFrame): XGBoostModel = {
    val params = new XGBoostParameters()
    params._train = train.key
    params._valid = valid.key
    params._response_column = "target"
    params._ntrees = 100
    params._eta = 0.3
    params._learn_rate = 0.3
    params._max_depth = 3
    params._reg_alpha = 0.01f
    params._reg_lambda = 0.01f
    //    params._ignored_columns = Array("id")

    params._backend = Backend.cpu
    params._stopping_rounds = 5
    params._stopping_metric = StoppingMetric.AUC
    params._categorical_encoding = CategoricalEncodingScheme.OneHotExplicit

    val dl = new XGBoost(params)
    dl.trainModel.get

  }

}
