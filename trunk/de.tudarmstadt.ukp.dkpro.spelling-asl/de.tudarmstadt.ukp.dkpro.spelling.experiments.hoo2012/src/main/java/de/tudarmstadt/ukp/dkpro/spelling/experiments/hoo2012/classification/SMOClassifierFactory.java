/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
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
