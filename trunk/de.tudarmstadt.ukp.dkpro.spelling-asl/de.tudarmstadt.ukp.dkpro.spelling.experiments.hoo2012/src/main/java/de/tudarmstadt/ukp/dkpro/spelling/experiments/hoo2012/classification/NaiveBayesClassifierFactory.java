package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.classification;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.AbstractWekaClassifierFactory;

public class NaiveBayesClassifierFactory extends AbstractWekaClassifierFactory {

	@Override
	public Classifier createWekaClassifier() {
	    return new NaiveBayes();
	}

}
