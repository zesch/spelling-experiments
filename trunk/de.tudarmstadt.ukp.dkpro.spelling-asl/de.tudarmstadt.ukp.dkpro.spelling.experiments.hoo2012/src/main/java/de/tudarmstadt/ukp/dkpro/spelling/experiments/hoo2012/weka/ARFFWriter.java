package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.cleartk.classifier.CleartkProcessingException;
import org.cleartk.classifier.DataWriter;
import org.cleartk.classifier.Feature;
import org.cleartk.classifier.Instance;

import weka.core.Instances;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.feature.ClassFeature;

/**
 * This class writes data to an ARFF file
 */
public class ARFFWriter implements DataWriter<ClassFeature> {

	private Meta meta;
	private BufferedWriter writer;
	
	/**
	 * Creates a new ARFF Writer
	 * @param file the output file
	 * @param meta meta information for the current training task
	 * @throws IOException
	 */
	public ARFFWriter(File file, Meta meta) throws IOException {
		this.meta = meta;
		
		file.getParentFile().mkdirs();
		Instances instances = meta.createInstances();
		writer = new BufferedWriter(new FileWriter(file));
		
		writer.write(instances.toString());
	}

	@Deprecated
	public void write(List<Feature> features, Feature outcome) throws IOException {
		features.add(outcome);
		writer.write(WekaConverter.featuresToInstance(features, meta).toString()+"\n");
	}

	@Override
	public void finish() throws CleartkProcessingException {
		try {
			writer.close();
		} catch (IOException e) {
			throw new CleartkProcessingException(e);
		}
	}

	@Override
	public void write(Instance<ClassFeature> instance) throws CleartkProcessingException {
		try {
			writer.write(WekaConverter.toWekaInstance(instance, meta).toString()+"\n");
		} catch (IOException e) {
			throw new CleartkProcessingException(e);
		}
	}

}
