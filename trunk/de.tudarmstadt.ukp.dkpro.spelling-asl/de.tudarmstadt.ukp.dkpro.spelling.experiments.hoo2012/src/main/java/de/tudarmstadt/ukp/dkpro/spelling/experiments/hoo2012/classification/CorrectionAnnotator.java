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

import java.util.Collection;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.classifier.Instance;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.descriptor.ExternalResource;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.frequency.provider.FrequencyCountProvider;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.utils.SpellingUtils;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.featureextraction.SpellingCorrectionFeatureBuilder;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.util.MyJCasUtil;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.WekaSequenceAnnotator;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.feature.ClassFeature;

/**
 * This annotator corrects incorrect word
 * based on a trained ARFF file. Currently
 * the SMO algorithm from weka is used to classify
 * the words as correct/incorrect.
 * 
 * Use the score parameter to reduce the 
 * amount of incorrect classify results.
 */
public class CorrectionAnnotator extends WekaSequenceAnnotator {
	
	private SpellingCorrectionFeatureBuilder builder;
	
    public final static String FREQUENCY_PROVIDER_RESOURCE = "FrequencyProvider";
    @ExternalResource(key = FREQUENCY_PROVIDER_RESOURCE)
    protected FrequencyCountProvider provider;
	
	public static final String PARAM_CONTEXT_SIZE = "contextSize";
	@ConfigurationParameter(name = PARAM_CONTEXT_SIZE, mandatory = false, defaultValue="2")
	private int contextSize;
	
	public static final String PARAM_CONFUSION_SET = "ConfusionSet";
	@ConfigurationParameter(name=PARAM_CONFUSION_SET, mandatory=true)
	private String[] confusionSetArray;
		
	public static final String PARAM_CATEGORY_CLASS = "CategoryClass";
	@ConfigurationParameter(name = PARAM_CATEGORY_CLASS, mandatory = true)
	private String categoryClass;

	@Override
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
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
			if (i > contextSize && i < tokens.size()-contextSize && builder.getConfusionSet().contains(t.getCoveredText())) {
				try {
					Instance<ClassFeature> features = builder.extract(cas, t, isTraining());
					
					if (isTraining()) {
						this.dataWriter.write(features);
					} else {
						String res = this.classifier.classify(features.getFeatures());
						if (res != null) {
							if (!t.getCoveredText().equals(res)) {
								boolean nextWithVowel = false;
								Token next = MyJCasUtil.selectRelative(cas, Token.class, t, 1);
								if (next != null && next.getCoveredText().toLowerCase().matches("^[aeiou].*$")) {
									nextWithVowel = true;
								}
								
								String clazz = (res == "a" && nextWithVowel) ? "an" : res;
								
								SpellingAnomaly ann = new SpellingAnomaly(cas, t.getBegin(), t.getEnd());
								ann.setCategory(categoryClass);
								ann.setSuggestions(SpellingUtils.getSuggestedActionArray(cas, clazz));
								ann.addToIndexes();
							}
						}
					}
				} catch (Exception e) {
					throw new AnalysisEngineProcessException(e);
				}
			}
			i++;
		}
	}
}
