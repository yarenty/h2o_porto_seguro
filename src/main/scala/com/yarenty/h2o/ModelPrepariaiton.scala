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


    val NAs = Array(
      Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"),
      Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"),
      Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"),
      Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"),
      Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN"), Array("NAN")
    )

    val input = new H2OFrame(getSimpleCSVParser, new URI(trainFile))
    val test = new H2OFrame(getSimpleCSVParser, new URI(testFile))


    val processedNames = input.names.filter(n => n.contains("_cat") || n.contains("_bin")) ++ Array("target") //|| n.contains("_bin") ) 
    println(processedNames mkString ",")

    input.colToEnum(processedNames)
    input.remove("id")
    val tobeproc = input.names.filter(n => !processedNames.contains(n) && n != "id")

    println("TO BE PROCESSED:" + tobeproc.mkString)


    test.add(Array("target"), Array(test.vec("ps_calc_15_bin").makeCopy())) //just fake column otherwise XGBoost is blowing up ;-)
    test.colToEnum(processedNames)
    test.remove("id")

    process(input, tobeproc)
    process(test, tobeproc)


    val (train, valid) = split(input, 0.8) // this is split 0.8/0.2

    val model = dlModel(H2OFrame(train), H2OFrame(valid))
    println(model)

    val prediction = model.score(test)

    //test.delete()
    saveCSV(prediction, datadir + "/out.csv")
  }


  def process(h2OFrame: H2OFrame, toProc: Array[String]): Unit = {
    for (col <- toProc) {
      h2OFrame.add(col + "_pow2", power(h2OFrame.vec(col)))
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
