package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka;

import java.io.File;
import java.io.IOException;

import org.apache.uima.UimaContext;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.classifier.Classifier;
import org.uimafit.component.initialize.ConfigurationParameterInitializer;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.factory.initializable.Initializable;

public abstract class AbstractWekaClassifierFactory implements WekaClassifierFactory, Initializable {

    public static final String PARAM_ARFF_FILE = "arffFile";
	@ConfigurationParameter(name = PARAM_ARFF_FILE, mandatory = true)
	private File arffFile;
	
	public static final String PARAM_META_FILE = "metaFile";
	@ConfigurationParameter(name = PARAM_META_FILE, mandatory = true)
	private File metaFile;
	
	public static final String PARAM_THRESHOLD = "Threshold";
	@ConfigurationParameter(name = PARAM_THRESHOLD, mandatory = false)
	private double threshold;
	
	@Override
	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		ConfigurationParameterInitializer.initialize(this, context);
	}
	
	public abstract weka.classifiers.Classifier createWekaClassifier();
	
	@Override
	public Classifier<String> createClassifier() throws IOException {
		try {
			return new WekaClassifier(createWekaClassifier(), arffFile, Meta.load(metaFile.getAbsolutePath()), threshold);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

}
