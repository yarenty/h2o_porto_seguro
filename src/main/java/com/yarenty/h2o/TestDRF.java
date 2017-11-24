package com.yarenty.h2o;

import hex.Model;
import hex.ScoreKeeper;
import hex.tree.drf.DRF;
import hex.tree.drf.DRFModel;
import hex.tree.drf.DRFModel.DRFParameters;

import water.fvec.Frame;

public class TestDRF {
    
    public DRFModel drfModel(Frame train, Frame valid) {
        DRFParameters params = new DRFParameters();
        params._train = train._key;
        params._valid = valid._key;
        params._response_column = "target";
        params._ntrees = 500;
        params._max_depth = 4;
        params._sample_rate = 0.8;
        params._nbins_top_level = 20;
        params._nbins_cats = 20;
        params._col_sample_rate_per_tree = 0.8;


        params._stopping_rounds = 20;
        params._stopping_metric = ScoreKeeper.StoppingMetric.AUC;

        //TODO: test with other!!!
        params._categorical_encoding = Model.Parameters.CategoricalEncodingScheme.OneHotExplicit;

        params._seed = 666L;


        DRF dl = new DRF(params);
        return dl.trainModel().get();
    }


    
}
