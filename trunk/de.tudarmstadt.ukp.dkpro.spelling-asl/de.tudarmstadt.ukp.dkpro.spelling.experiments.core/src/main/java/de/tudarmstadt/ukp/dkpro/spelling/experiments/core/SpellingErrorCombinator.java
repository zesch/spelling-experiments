/*******************************************************************************
 * Copyright 2010
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
package de.tudarmstadt.ukp.dkpro.spelling.experiments.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.util.FSCollectionFactory;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.type.GoldSpellingAnomaly;

/**
 * Combines SpellingAnomaly annotations at the same offset given the configured combination strategy.
 * 
 * @author zesch
 *
 */
public class SpellingErrorCombinator extends JCasAnnotator_ImplBase {

    public enum CombinationStrategy {
        join,               // join all anomalies into a single annotation joining suggestions with maximum certainty values
        onlyKeepMultiple    // only keep anomalies that were detected by multiple detectors
    }

    public static final String PARAM_COMBINATION_STRATEGY = "CombinationStrategy";
    @ConfigurationParameter(name=PARAM_COMBINATION_STRATEGY, mandatory=true)
    protected CombinationStrategy strategy;

    private JCas jcas;
    
    @Override
    public void process(JCas jcas)
        throws AnalysisEngineProcessException
    {

        this.jcas = jcas;
        
        Map<String,List<SpellingAnomaly>> errorMap = new HashMap<String,List<SpellingAnomaly>>();

        // retrieve SpellingAnomaly annotations, but filter out the GoldSpellingAnomalies
        List<SpellingAnomaly> anomalies = new ArrayList<SpellingAnomaly>();
        for (SpellingAnomaly anomaly : JCasUtil.select(jcas, SpellingAnomaly.class)) {
            if (!(anomaly instanceof GoldSpellingAnomaly)) {
                anomalies.add(anomaly);
                
                String fingerprint = getFingerprint(anomaly);
                
                List<SpellingAnomaly> errorsOnSamePosition;
                if (errorMap.containsKey(fingerprint)) {
                    errorsOnSamePosition = errorMap.get(fingerprint);
                }
                else {
                    errorsOnSamePosition = new ArrayList<SpellingAnomaly>();
                }
                errorsOnSamePosition.add(anomaly);
                errorMap.put(fingerprint, errorsOnSamePosition);
            }
        }

        // now combine if multiple matching anomalies were found
        List<String> fingerprintsToRemove = new ArrayList<String>();
        List<SpellingAnomaly> anomaliesToAdd = new ArrayList<SpellingAnomaly>();
        
        for (String fingerprint : errorMap.keySet()) {
            List<SpellingAnomaly> errorList = errorMap.get(fingerprint); 
            if (errorList.size() > 1) {
                anomaliesToAdd.add(join(errorList));
                fingerprintsToRemove.add(fingerprint);
            }
            else if (strategy.equals(CombinationStrategy.onlyKeepMultiple)) {
                fingerprintsToRemove.add(fingerprint);
            }
        }
        
        for (String fingerprint : fingerprintsToRemove) {
            for (SpellingAnomaly anomaly : errorMap.get(fingerprint)) {
                anomaly.removeFromIndexes();
            }
        }

        for (SpellingAnomaly combinedAnomaly : anomaliesToAdd) {
            combinedAnomaly.addToIndexes();
        }
    }

    private String getFingerprint(SpellingAnomaly a) {
        return a.getBegin() + "-" + a.getEnd();
    }

    private SpellingAnomaly join(List<SpellingAnomaly> errorList) throws AnalysisEngineProcessException {
        int begin = 0;
        int end = 0;
        List<SuggestedAction> actions = new ArrayList<SuggestedAction>();
        
        for (SpellingAnomaly error : errorList) {
            begin = error.getBegin();
            end = error.getEnd();

            for (FeatureStructure fs : FSCollectionFactory.create(error.getSuggestions())) {
                actions.add((SuggestedAction) fs);
            }
        }
        
        List<SuggestedAction> joinedActions = joinActions(actions);
        
        FSArray array = new FSArray(jcas, joinedActions.size());
        for (int i=0; i<joinedActions.size(); i++) {
            array.set(i, joinedActions.get(i));
        }
        SpellingAnomaly joinedError = new SpellingAnomaly(jcas, begin, end);
        joinedError.setSuggestions(array);
        
        return joinedError;
    }

    private List<SuggestedAction> joinActions(List<SuggestedAction> actions) {

        Map<String, Float> replacementCertaintyMap = new HashMap<String,Float>();
        for (SuggestedAction action : actions) {
            String replacement = action.getReplacement();
            float certainty = action.getCertainty();
            if (replacementCertaintyMap.containsKey(replacement)) {
                if (certainty > replacementCertaintyMap.get(replacement)) {
                    replacementCertaintyMap.put(replacement, certainty);
                }
            }
            else {
                replacementCertaintyMap.put(replacement, certainty);
            }
        }
        
        List<SuggestedAction> joinedActions = new ArrayList<SuggestedAction>();
        for (String replacement : replacementCertaintyMap.keySet()) {
            SuggestedAction action = new SuggestedAction(jcas);
            action.setReplacement(replacement);
            action.setCertainty(replacementCertaintyMap.get(replacement));
            joinedActions.add(action);
        }

        return joinedActions;
    }
}
