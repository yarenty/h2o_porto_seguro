package com.yarenty.h2o

import java.net.URI

import hex.Model.Parameters.CategoricalEncodingScheme
import hex.ScoreKeeper.StoppingMetric
import hex.deeplearning.DeepLearningModel.DeepLearningParameters
import hex.deeplearning.{DeepLearning, DeepLearningModel}
import hex.tree.gbm.GBMModel.GBMParameters
import hex.tree.gbm.{GBM, GBMModel}
import hex.tree.xgboost.XGBoostModel.XGBoostParameters
import hex.tree.xgboost.XGBoostModel.XGBoostParameters.Backend
import hex.tree.xgboost.{XGBoost, XGBoostModel}
import water.fvec.H2OFrame

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


    //this must be here as next step sreates a lots of _cat columns ;-)
    val toBeEnums = input.names.filter(n => n.contains("_cat")) ++ Array(
      //      "ps_ind_01", "ps_ind_03", "ps_ind_14","ps_ind_15", 
      //      "ps_reg_01", "ps_reg_02",
      //      "ps_car_11", "ps_car_15",
      "ps_calc_01", "ps_calc_02", "ps_calc_03", "ps_calc_04", "ps_calc_05",
      "ps_calc_06", "ps_calc_07", "ps_calc_08", "ps_calc_09", "ps_calc_10",
      "ps_calc_11", "ps_calc_12", "ps_calc_13", "ps_calc_14"
      //      "ps_car_15_pow2"
    )


    // cool thing  - get binary median/mean  colums
    // TODO test changing them to enums!
    DataMunging.processMedian(input)
    DataMunging.processMedian(test)

    // TODO check which will be better - reg 03 is important!!
    val vecNanToMean = Array("ps_reg_03", "ps_car_11", "ps_car_12", "ps_car_14")
    println("TO BE NAN replaced by mean:" + vecNanToMean.mkString(","))
    //    DataMunging.processNANMean(input, vecNanToMean)
    //    DataMunging.processNANMean(test, vecNanToMean)
    DataMunging.processNANToMinusOne(input, vecNanToMean)
    DataMunging.processNANToMinusOne(test, vecNanToMean)


    val vecToInts = Array("ps_reg_01", "ps_reg_02", "ps_calc_01", "ps_calc_02", "ps_calc_03")
    println("TO BE INTED:" + vecToInts.mkString)
    DataMunging.processToInt(input, vecToInts)
    DataMunging.processToInt(test, vecToInts)


    val tobePowered = Array("ps_car_12", "ps_car_14", "ps_car_15")
    println("TO BE POWERED:" + tobePowered.mkString(","))
    DataMunging.processPower(input, tobePowered)
    DataMunging.processPower(test, tobePowered)

// really not sure ...
    // check again with NANCat
    val vecNanToCat = input.names.filter(n => n.endsWith("_cat"))
    println("TO BE NAN replaced as new category:" + vecNanToCat.mkString(","))
    //    DataMunging.processNANCat(input, vecNanToCat)
    //    DataMunging.processNANCat(test, vecNanToCat)
    DataMunging.processNANToMinusOne(input, vecNanToCat)
    DataMunging.processNANToMinusOne(test, vecNanToCat)


    // create hot encoders colum based on categorical values!
    DataMunging.processHOTEndcoder(input, toBeEnums)
    DataMunging.processHOTEndcoder(test, toBeEnums)


    test.add(Array("target"), Array(test.vec("ps_calc_15_bin"))) //just fake column otherwise XGBoost is blowing up ;-)


    input.colToEnum(Array("target"))
    test.colToEnum(Array("target"))

    // strange one - two most importan values here .. muliplied 
    DataMunging.processMultiply(input)
    DataMunging.processMultiply(test)

    
    // no more removing
    //    val toRemove = Array("id",
    //      "ps_car_03_cat", "ps_car_05_cat" // to many missing values no point
    //    ) 
    //    println("TO BE Removed:" + toRemove.mkString(","))
    //    input.remove(toRemove)
    //    test.remove(toRemove)
    //    


    val (train, valid) = split(input, 0.9) // this is split 0.8/0.2

    val model = xgbModel(H2OFrame(train), H2OFrame(valid))
    println(model)
    val prediction = model.score(test)

    //test.delete()
    saveCSV(prediction, datadir + "/out.csv")

    val model2 = xgbModel(H2OFrame(train), H2OFrame(valid))
    println(model2)
    val prediction2 = model2.score(test)
    saveCSV(prediction, datadir + "/out2.csv")


    //m1*0.4 + m2*0.6 - 1.0  //clip (0,1) run1 0.274
    //exp(m1*0.6 + m2*0.4 - 1.0)  //clip (0,1) run2 0.275

    //TODO!!!
    //exp(m1*0.6 + m2*0.4 - 1.0)   min/2 over * 1.5//clip (0,1) run2 
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

  /*
  * 
  * buildModel 'xgboost', {"model_id":"xgboost-e3ac41d2-a596-4aca-9d63-7f8c1acd1c6e",
  * "training_frame":"train_0.8","validation_frame":"test_0.8","nfolds":0,"response_column":"target",
  * "ignored_columns":[],"ignore_const_cols":true,"seed":-1,"ntrees":"500","max_depth":"4",
  * "min_rows":1,"min_child_weight":"0.77","learn_rate":0.3,"eta":"0.09","sample_rate":1,
  * "subsample":"0.8","col_sample_rate":"1","colsample_bylevel":"1","score_each_iteration":false,
  * "stopping_rounds":"20","stopping_metric":"AUC","stopping_tolerance":0.001,"max_runtime_secs":0,
  * "distribution":"AUTO","categorical_encoding":"OneHotExplicit","col_sample_rate_per_tree":1,
  * "colsample_bytree":"0.8","score_tree_interval":0,"min_split_improvement":0,"gamma":"10","max_leaves":0,
  * "tree_method":"auto","grow_policy":"depthwise","dmatrix_type":"auto","quiet_mode":true,"max_abs_leafnode_pred":0,
  * "max_delta_step":0,"max_bins":256,"min_sum_hessian_in_leaf":100,"min_data_in_leaf":0,"sample_type":"uniform",
  * "normalize_type":"tree","rate_drop":0,"one_drop":false,"skip_drop":0,
  * "booster":"gbtree","reg_lambda":"1.3","reg_alpha":"8","backend":"auto","gpu_id":0}
  * 
  * */



//  model	XGBoost_model_1511463550144_1
//    model_checksum	-4691526202905552896
//  frame	train_0.9
//  frame_checksum	109074937358700432
//  description	Metrics reported on training frame
//    model_category	Binomial
//    scoring_time	1511464659982
//  predictions	·
//  MSE	0.034604
//  RMSE	0.186023
//  nobs	535690
//  r2	0.013994
//  logloss	0.150638
//  AUC	0.657293
//  Gini	0.314585
//  mean_per_class_error	0.421468

  
  // valid
  
  
  

  private def xgbModel(train: H2OFrame, valid: H2OFrame): XGBoostModel = {
    val params = new XGBoostParameters()
    params._train = train.key
    params._valid = valid.key
    params._response_column = "target"
    params._ntrees = 500
    params._max_depth = 4
    params._min_child_weight = 0.77
    params._eta = 0.09
    params._learn_rate = 0.3
    params._subsample = 0.8
    params._colsample_bytree = 0.8
    params._gamma = 10f
    params._max_bins = 256
    params._reg_alpha = 8f
    params._reg_lambda = 1.3f
    //    params._ignored_column.s = Array("id")

    params._backend = Backend.cpu
    params._stopping_rounds = 20
    params._stopping_metric = StoppingMetric.AUC
    
    //TODO: test with other!!!
    params._categorical_encoding = CategoricalEncodingScheme.OneHotExplicit

    params._seed = 666L


    val dl = new XGBoost(params)
    dl.trainModel.get

  }


  /*
  * 
  * buildModel 'gbm', {"model_id":"gbm","training_frame":"train_0.9","validation_frame":"test_0.9","nfolds":0,
  * "response_column":"target","ignored_columns":[],"ignore_const_cols":true,"ntrees":"500","max_depth":"4",
  * "min_rows":10,"nbins":"10","seed":-1,"learn_rate":"0.02","sample_rate":"0.8","col_sample_rate":1,
  * "score_each_iteration":false,"score_tree_interval":0,"balance_classes":false,"nbins_top_level":"20",
  * "nbins_cats":"20","r2_stopping":1.7976931348623157e+308,"stopping_rounds":"20","stopping_metric":"AUC",
  * "stopping_tolerance":0.001,"max_runtime_secs":0,"learn_rate_annealing":1,"distribution":"AUTO",
  * "huber_alpha":0.9,"checkpoint":"","col_sample_rate_per_tree":"0.8","min_split_improvement":0.00001,
  * "histogram_type":"AUTO","categorical_encoding":"OneHotExplicit","build_tree_one_node":false,
  * "sample_rate_per_class":[],"col_sample_rate_change_per_level":1,"max_abs_leafnode_pred":1.7976931348623157e+308,
  * "pred_noise_bandwidth":0,"calibrate_model":false}
  * */

  
  
//  model	gbm
//  model_checksum	-1409264291168570112
//  frame	train_0.9
//  frame_checksum	109074937358700432
//  description	·
//  model_category	Binomial
//    scoring_time	1511468096575
//  predictions	·
//  MSE	0.034509
//  RMSE	0.185765
//  nobs	535690
//  r2	0.016723
//  logloss	0.150247
//  AUC	0.658398
//  Gini	0.316796
//  mean_per_class_error	0.425025
  
  //valid:

//  model	gbm
//    model_checksum	-1409264291168570112
//  frame	test_0.9
//  frame_checksum	-8978652314917430272
//  description	·
//  model_category	Binomial
//    scoring_time	1511468100974
//  predictions	·
//  MSE	0.034949
//  RMSE	0.186945
//  nobs	59522
//  r2	0.010808
//  logloss	0.152702
//  AUC	0.638738
//  Gini	0.277475
//  mean_per_class_error	0.430980



//  model	XGBoost_model_1511463550144_1
//    model_checksum	-4691526202905552896
//  frame	test_0.9
//  frame_checksum	-8978652314917430272
//  description	Metrics reported on validation frame
//    model_category	Binomial
//    scoring_time	1511464660634
//  predictions	·
//  MSE	0.034936
//  RMSE	0.186912
//  nobs	59522
//  r2	0.011162
//  logloss	0.152519
//  AUC	0.640187
//  Gini	0.280375
//  mean_per_class_error	0.435849


  private def gbmModel(train: H2OFrame, valid: H2OFrame): GBMModel = {
    val params = new GBMParameters
    params._train = train.key
    params._valid = valid.key
    params._response_column = "target"
    params._ntrees = 500
    params._max_depth = 4
    params._learn_rate = 0.02
    params._sample_rate = 0.8
    params._nbins_top_level = 20
    params._nbins_cats = 20
    params._col_sample_rate_per_tree = 0.8


    params._stopping_rounds = 20
    params._stopping_metric = StoppingMetric.AUC

    //TODO: test with other!!!
    params._categorical_encoding = CategoricalEncodingScheme.OneHotExplicit

    params._seed = 666L


    val dl = new GBM(params)
    dl.trainModel.get

  }
}
