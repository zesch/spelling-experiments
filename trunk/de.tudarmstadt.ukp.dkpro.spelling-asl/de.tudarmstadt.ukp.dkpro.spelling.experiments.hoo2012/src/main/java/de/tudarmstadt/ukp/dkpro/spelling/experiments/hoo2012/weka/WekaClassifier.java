package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

import org.cleartk.classifier.CleartkProcessingException;
import org.cleartk.classifier.Feature;
import org.cleartk.classifier.ScoredOutcome;

import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;

/**
 * A wrapper for weka classifier
 */
public class WekaClassifier implements org.cleartk.classifier.Classifier<String> {
	
	private Classifier classifier;
	private Meta meta;
	private Double th;
	private Instances instances;

	/**
	 * Creates a new instance of this weka classifier
	 * @param classifier A weka classifier
	 * @param arffFile The trained arff file
	 * @param meta the meta information of the training set
	 * @param th An optional threshold. If the weka classifier implements the distributionForInstance method, this can be used
	 * @throws Exception
	 */
	public WekaClassifier(Classifier classifier, File arffFile, Meta meta, Double th) throws Exception {
		this.classifier = classifier;
		this.meta = meta;
		this.th = th;
		
		instances = new Instances(new BufferedReader(new FileReader(arffFile)));
		instances.setClass(instances.attribute(meta.getClassAttribute().getName()));
		classifier.buildClassifier(instances);
	}
	
	public WekaClassifier(Classifier classifier, File arffFile, Meta meta) throws Exception {
		this(classifier, arffFile, meta, null);
	}

	@Override
	public String classify(List<Feature> features)
			throws CleartkProcessingException {
		Instance inst = WekaConverter.featuresToInstance(features, meta, instances);
		
		try {
			if (th != null) {
				double[] data = classifier.distributionForInstance(inst);
				int index = -1;
				double max = -1;
				for (int i = 0; i < data.length; i++) {
					if (data[i] >= th && data[i] > max) {
						index = i;
						max = data[i];
					}
				}
				
				if (index >= 0) {
					return inst.classAttribute().value(index);
				}
				
				return null;
			} else {
				double r = classifier.classifyInstance(inst);
				return inst.classAttribute().value((int) r);
			}
		} catch (Exception e) {
			throw new CleartkProcessingException(e);
		}
	}

	@Override
	public List<ScoredOutcome<String>> score(List<Feature> features,
			int maxResults) throws CleartkProcessingException {
		// TODO Auto-generated method stub
		return null;
	}

}
