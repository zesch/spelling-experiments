package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka;

import java.util.ArrayList;
import java.util.List;

import org.cleartk.classifier.Feature;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute.ClassAttribute;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute.DateAttribute;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute.NominalAttribute;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.feature.ClassFeature;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.feature.DoubleFeature;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.feature.FloatFeature;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.feature.IntegerFeature;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.feature.LongFeature;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.feature.NominalFeature;

/**
 * This class converts features and attributes from and to weka
 */
public class WekaConverter {

	/**
	 * Converts a IWekaAttribute to a Weka attribute
	 * @param attr
	 * @return
	 */
	public static Attribute convertAttribute(IWekaAttribute attr) {
		if (attr instanceof NominalAttribute) {
			NominalAttribute a = (NominalAttribute) attr;
			List<String> nominals = new ArrayList<String>(a.getNominals());
			nominals.add(a.getFallback());
			return new Attribute(a.getName(), nominals, 0);
		} else if (attr instanceof ClassAttribute) {
			ClassAttribute a = (ClassAttribute) attr;
			List<String> nominals = new ArrayList<String>(a.getNominals());
			return new Attribute(a.getName(), nominals, 0);
		} else if (attr instanceof INumericAttribute) {
			return new Attribute(attr.getName());
		} else if (attr instanceof DateAttribute) {
			DateAttribute a = (DateAttribute) attr;
			return new Attribute(a.getName(), a.getFormat());
		}
		
		throw new IllegalArgumentException("Can not cast this attribute to weka: " + attr);
	}
	
	/**
	 * Converts a list of features to a weka instance
	 * @param features
	 * @param meta
	 * @param instances
	 * @return
	 */
	public static Instance featuresToInstance(List<Feature> features, Meta meta, Instances instances) {
		Instance inst = new SparseInstance(instances.numAttributes());
		inst.setDataset(instances);
		
		for (Feature f: features) {
			if (meta.getAttributeIndex().keySet().contains(f.getName())) {
				applyValue(inst, f, meta);
			}
		}
		
		return inst;
	}

	private static void applyValue(Instance inst, Feature f, Meta meta) {
		int index = inst.dataset().attribute(f.getName()).index();
		
		if (f instanceof NominalFeature) {
			inst.setValue(index, norminalValue((NominalFeature) f, meta));
		} else if (f instanceof ClassFeature) {
			inst.setValue(index, ((ClassFeature)f).getTypedValue());
		} else if (f instanceof DoubleFeature) {
			inst.setValue(index, ((DoubleFeature)f).getTypedValue().doubleValue());
		} else if (f instanceof FloatFeature) {
			inst.setValue(index, ((FloatFeature)f).getTypedValue().doubleValue());
		} else if (f instanceof IntegerFeature) {
			inst.setValue(index, ((IntegerFeature)f).getTypedValue().doubleValue());
		} else if (f instanceof LongFeature) {
			inst.setValue(index, ((LongFeature)f).getTypedValue().doubleValue());
		} else {
			inst.setValue(index, f.getValue().toString());
		}
		
	}

	private static String norminalValue(NominalFeature f, Meta meta) {
		NominalAttribute a = meta.getNominalIndex().get(f.getName());
		
		if (a == null) {
			return null;
		}
		
		if (a.getNominals().contains(f.getValue())) {
			return f.getTypedValue();
		} else {
			return a.getFallback();
		}
	}

	public static Instance featuresToInstance(List<Feature> features, Meta meta) {
		return featuresToInstance(features, meta, meta.createInstances());
	}
	
	public static Instance toWekaInstance(org.cleartk.classifier.Instance<ClassFeature> instance, Meta meta) {
		List<Feature> features = instance.getFeatures();
		if (instance.getOutcome() != null) {
			features.add(instance.getOutcome());
		}
		
		return featuresToInstance(features, meta);
	}
}
