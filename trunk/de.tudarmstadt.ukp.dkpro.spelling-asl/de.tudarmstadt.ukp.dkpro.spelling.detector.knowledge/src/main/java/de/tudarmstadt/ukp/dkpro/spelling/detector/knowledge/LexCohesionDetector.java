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
package de.tudarmstadt.ukp.dkpro.spelling.detector.knowledge;

import java.util.Set;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.type.RWSECandidate;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.utils.SpellingUtils;

/**
 * Detects whether a RWSECandidate has sufficient lexical cohesion with its context
 * to safely assume that it is not a RWSE.
 * 
 * Reimplementation of the method by Hirst & Budanitsky (2005).
 * 
 * @author zesch
 *
 */
public class LexCohesionDetector
    extends KnowledgeBasedDetector
{
    
    @Override
    protected void generateAndTest(JCas jcas) throws AnalysisEngineProcessException
    {
        for (RWSECandidate candidate : JCasUtil.select(jcas, RWSECandidate.class)) {

            // generate spelling variants
            Set<String> variations = SpellingUtils.getEditsInVocabulary(
                    candidate.getCoveredText(),
                    maxEditDistance,
                    vocabulary
            );
            
            // no variants => nothing to do for this candidate
            if (variations.size() == 0) {
                continue;
            }
            
            // adds a SpellingAnomaly annotation if the spelling variation has lexical cohesion with the context
            for (String variation : variations) {
                if (testSpellingVariation(variation)) {
                    SpellingAnomaly anomaly = new SpellingAnomaly(jcas);
                    anomaly.setBegin(candidate.getBegin());
                    anomaly.setEnd(candidate.getEnd());
                    anomaly.setSuggestions(SpellingUtils.getSuggestedActionArray(jcas, variation));
                    anomaly.addToIndexes();

                    break;
                }
            }
        }
    }

    private boolean testSpellingVariation(String variation) throws AnalysisEngineProcessException
    {
        
        if (variation.length() < minLength) {
            return false;
        }
        
        if (tokenAppearsInContext(variation) ||
            lemmaAppearsInContext(variation) ||
            isSemanticallyRelated(variation))   // FIXME if variation is not a lemma - semantic relatedness cannot be computed -> how to lemmatize a token out of context?
        {
            return true;
        }
        
        return false;
    }
}
