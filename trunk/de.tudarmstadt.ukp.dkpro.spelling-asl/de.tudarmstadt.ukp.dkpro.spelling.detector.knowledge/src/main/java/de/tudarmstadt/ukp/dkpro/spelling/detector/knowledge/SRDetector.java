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
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.type.RWSECandidate;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.utils.SpellingUtils;
import de.tudarmstadt.ukp.similarity.algorithms.api.SimilarityException;

/**
 * Detects whether a RWSECandidate has sufficient lexical cohesion with its context
 * to safely assume that it is not a RWSE.
 * 
 * Lexical cohesion is measured using the semantic relatedness of the spelling variations with the context.
 * 
 * @author zesch
 *
 */
public class SRDetector
    extends KnowledgeBasedDetector
{

    @Override
    protected void generateAndTest(JCas jcas) throws AnalysisEngineProcessException
    {
        for (RWSECandidate candidate : JCasUtil.select(jcas, RWSECandidate.class)) {

            String target = candidate.getCoveredText(); 

            // generate spelling variants
            Set<String> variations = SpellingUtils.getEditsInVocabulary(
                    target,
                    maxEditDistance,
                    vocabulary
            );
            
            // no variants => nothing to do for this candidate
            if (variations.size() == 0) {
                continue;
            }
            
            double bestScore = getContextualRelatedness(jcas, target);
            String bestVariation = target;
            for (String variation : variations) {
                double score = getContextualRelatedness(jcas, variation); 
                if (score > bestScore) {
                    bestScore = score;
                    bestVariation = variation;
                }
            }

            if (!bestVariation.equals(target)) {
                SpellingAnomaly anomaly = new SpellingAnomaly(jcas);
                anomaly.setBegin(candidate.getBegin());
                anomaly.setEnd(candidate.getEnd());
                anomaly.setSuggestions(SpellingUtils.getSuggestedActionArray(jcas, bestVariation));
                anomaly.addToIndexes();
            }
        }
        
    }


    private double getContextualRelatedness(JCas jcas, String term) throws AnalysisEngineProcessException {
        double score = 0;
        for (Lemma l : JCasUtil.select(jcas, Lemma.class)) {
            try {
                score += measure.getSimilarity(term, l.getValue());
            }
            catch (SimilarityException e) {
                throw new AnalysisEngineProcessException(e);
            }
        }
        return score;
    }
}
