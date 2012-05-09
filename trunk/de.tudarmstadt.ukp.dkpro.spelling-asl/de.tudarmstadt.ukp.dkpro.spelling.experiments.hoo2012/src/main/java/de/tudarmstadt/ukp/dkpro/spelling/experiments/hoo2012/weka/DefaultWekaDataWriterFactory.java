package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka;

import java.io.File;
import java.io.IOException;

import org.apache.uima.UimaContext;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.classifier.DataWriter;
import org.uimafit.component.initialize.ConfigurationParameterInitializer;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.factory.initializable.Initializable;

import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.feature.ClassFeature;

public class DefaultWekaDataWriterFactory implements
		WekaDataWriterFactory, Initializable {
	
    public static final String PARAM_ARFF_FILE = "arffFile";
	@ConfigurationParameter(name = PARAM_ARFF_FILE, mandatory = true)
	private File arffFile;
	
	public static final String PARAM_META_FILE = "metaFile";
	@ConfigurationParameter(name = PARAM_META_FILE, mandatory = true)
	private File metaFile;

	@Override
	public DataWriter<ClassFeature> createDataWriter() throws IOException {
		try {
			return new ARFFWriter(arffFile, Meta.load(metaFile.getAbsolutePath()));
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		ConfigurationParameterInitializer.initialize(this, context);
	}

}
