package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.classification;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.classifier.Feature;
import org.cleartk.classifier.Instance;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.descriptor.ExternalResource;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.frequency.provider.FrequencyCountProvider;
import de.tudarmstadt.ukp.dkpro.core.api.frequency.util.FrequencyDistribution;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.featureextraction.SpellingCorrectionFeatureBuilder;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.IWekaAttribute;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.Meta;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute.ClassAttribute;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute.DoubleAttribute;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute.FloatAttribute;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute.IntegerAttribute;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute.NominalAttribute;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.feature.ClassFeature;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.feature.DoubleFeature;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.feature.FloatFeature;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.feature.IntegerFeature;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.feature.LongFeature;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.feature.NominalFeature;

/**
 * Collects the meta information of the training corpus.
 * It collects all features of nominal attributes. All
 * nominal values that do not appear more that two
 * times will removed. To each nominal value a default
 * value is added. This can be used in testing mode
 * if a new nominal value appears.
 * 
 * It also writes down all other attributes with name and type
 */
public class MetaCollector extends JCasAnnotator_ImplBase {

	private SpellingCorrectionFeatureBuilder builder;
	private Map<String, FrequencyDistribution<String>> map = new HashMap<String, FrequencyDistribution<String>>();
	private List<IWekaAttribute> attributes;
	
    public final static String FREQUENCY_PROVIDER_RESOURCE = "FrequencyProvider";
    @ExternalResource(key = FREQUENCY_PROVIDER_RESOURCE)
    protected FrequencyCountProvider provider;

    public static final String PARAM_RELATION = "relation";
	@ConfigurationParameter(name = PARAM_RELATION, mandatory = true)
	private String relation;
	
	public static final String PARAM_META_FILE = "metaFile";
	@ConfigurationParameter(name = PARAM_META_FILE, mandatory = true)
	private File metaFile;
	
	public static final String PARAM_CONTEXT_SIZE = "contextSize";
	@ConfigurationParameter(name = PARAM_CONTEXT_SIZE, mandatory = false, defaultValue="2")
	private int contextSize = 2;
	
	public static final String PARAM_CONFUSION_SET = "ConfusionSet";
	@ConfigurationParameter(name=PARAM_CONFUSION_SET, mandatory=true)
	private String[] confusionSetArray;
	
	@Override
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException
	{
		super.initialize(aContext);

		try {
			builder = new SpellingCorrectionFeatureBuilder(provider, confusionSetArray);
		} catch (Exception e) {
			throw new ResourceInitializationException(e);
		}
	}

	@Override
	public void process(JCas cas) throws AnalysisEngineProcessException {
		Collection<Token> tokens = JCasUtil.select(cas, Token.class);
		int i = 0;
		for (Token t: tokens) {
			// FIXME this does not extract features at the fringes of a sentence
			if (i > contextSize && i < tokens.size()-contextSize && builder.getConfusionSet().contains(t.getCoveredText())) {
				try {
					Instance<ClassFeature> features = builder.extract(cas, t);
					List<Feature> featureList = features.getFeatures();
					featureList.add(features.getOutcome());
					
					for (Feature f: featureList) {
						if (f instanceof NominalFeature) {
							NominalFeature nf = (NominalFeature) f;
							if (map.containsKey(nf.getName())) {
								map.get(nf.getName()).inc(nf.getTypedValue());
							} else {
								FrequencyDistribution<String> fd = new FrequencyDistribution<String>();
								fd.inc(nf.getTypedValue());
								map.put(nf.getName(), fd);
							}
						} else if (f instanceof ClassFeature) {
							ClassFeature cf =(ClassFeature) f;
							if (map.containsKey(cf.getName())) {
								map.get(cf.getName()).inc(cf.getTypedValue());
							} else {
								FrequencyDistribution<String> fd = new FrequencyDistribution<String>();
								fd.inc(cf.getTypedValue());
								map.put(cf.getName(), fd);
							}
						} else {
							// No collection for other features types. They have no nominal attributes
						}
					}
					
					// Convert all features to attributes
					if (attributes == null) {
						attributes = new ArrayList<IWekaAttribute>();
						for (Feature f: featureList) {
							attributes.add(featureToAttribute(f));
						}
					}
				} catch (Exception e) {
					throw new AnalysisEngineProcessException(e);
				}
			}
			i++;
		}
	}

	@Override
	public void collectionProcessComplete()
			throws AnalysisEngineProcessException {
		super.collectionProcessComplete();
		
		List<IWekaAttribute> finalAttr = new ArrayList<IWekaAttribute>();
		for (IWekaAttribute attr: this.attributes) {
			if (attr instanceof NominalAttribute) {
				Set<String> nominals = new HashSet<String>();
				for (String s: map.get(attr.getName()).getKeys()) {
					if (map.get(attr.getName()).getCount(s) > 1) {
						nominals.add(s);
					}
				}
				
				finalAttr.add(new NominalAttribute(attr.getName(), nominals, "XXX"));
			} else if (attr instanceof ClassAttribute) {
				finalAttr.add(new ClassAttribute(attr.getName(), map.get(attr.getName()).getKeys()));
			} else {
				finalAttr.add(attr);
			}
		}
		
		try {
			metaFile.getParentFile().mkdirs();
			Meta.save(metaFile.getAbsolutePath(), new Meta(relation, finalAttr));
		} catch (IOException e) {
			throw new AnalysisEngineProcessException(e);
		}
	}

	private IWekaAttribute featureToAttribute(Feature f) {
		if (f instanceof NominalFeature) {
			return new NominalAttribute(f.getName(), new HashSet<String>());
		} else if (f instanceof ClassFeature) {
			return new ClassAttribute(f.getName(), new HashSet<String>());
		} else if (f instanceof DoubleFeature) {
			return new DoubleAttribute(f.getName());
		} else if (f instanceof FloatFeature) {
			return new FloatAttribute(f.getName());
		} else if (f instanceof IntegerFeature) {
			return new IntegerAttribute(f.getName());
		} else if (f instanceof LongFeature) {
			return new IntegerAttribute(f.getName());
		}
		
		throw new IllegalArgumentException("Can not convert to attribute: " + f);
	}
}
