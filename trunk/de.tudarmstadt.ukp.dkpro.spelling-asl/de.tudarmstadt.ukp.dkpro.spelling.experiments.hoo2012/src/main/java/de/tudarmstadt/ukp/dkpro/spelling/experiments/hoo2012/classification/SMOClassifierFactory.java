package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.classification;

import weka.classifiers.Classifier;
import weka.classifiers.functions.SMO;
import weka.classifiers.functions.supportVector.PolyKernel;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.AbstractWekaClassifierFactory;

public class SMOClassifierFactory extends AbstractWekaClassifierFactory {

	@Override
	public Classifier createWekaClassifier() {
		// Poly kernel for the SVM classifier
		PolyKernel kernel = new PolyKernel();
	    kernel.setCacheSize(250007);
    	kernel.setExponent(1.0);
    	kernel.setUseLowerOrder(false);
		
    	// Default settings of weka SMO classifier
    	// We set BuildLogisticModels to have a scored output
		SMO r = new SMO();
	    r.setBuildLogisticModels(true);
	    r.setC(1);
	    r.setChecksTurnedOff(false);
	    r.setDebug(false);
	    r.setEpsilon(1.0E-12);
	    r.setNumFolds(-1);
	    r.setRandomSeed(1);
	    r.setToleranceParameter(0.001);
	    r.setKernel(kernel);
	    
	    return r;
	}

}
