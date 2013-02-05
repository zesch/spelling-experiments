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
package de.tudarmstadt.ukp.dkpro.spelling.detector.ngram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.Level;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.type.RWSECandidate;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.utils.SpellingUtils;

public class TrigramProbabilityDetector
    extends LMBasedDetector
{

    private JCas jcas;

    @Override
    public void process(JCas jcas)
        throws AnalysisEngineProcessException
    {

        this.jcas = jcas;
    
        countCache = new HashMap<String,Long>();
        
        // sanity check
        if (JCasUtil.select(jcas, RWSECandidate.class).size() == 0) {
            getContext().getLogger().log(Level.WARNING, "No RWSECandidate annotations present. Probably the pipeline is not properly configured.");
            getContext().getLogger().log(Level.WARNING, jcas.getDocumentText());
            return;
        }
        
        for (Sentence s : JCasUtil.select(jcas, Sentence.class)) {
            List<RWSECandidate> candidates = JCasUtil.selectCovered(jcas, RWSECandidate.class, s);
            
            // nothing to do, if there are no candidates in the sentence, 
            if (candidates.size() == 0) {
                continue;
            }
            
            List<Token> tokens = JCasUtil.selectCovered(jcas, Token.class, s);
            List<String> words = JCasUtil.toText(tokens);
        
            double targetSentenceProb = getSentenceProbability(words) * alpha;
//            System.out.println(words);
//            System.out.println(targetSentenceProb);
            
            double maxSentenceProb = targetSentenceProb;
            SpellingAnomaly anomaly = null;
            double oneMinusAlpha = 1 - alpha;
            for (RWSECandidate candidate : candidates) {
                
                int candidatePosition = NGramDetectorUtils.getCandidatePosition(candidate, tokens);
                
                if (candidatePosition == -1) {
                    throw new AnalysisEngineProcessException(new Throwable("Could not find matching token for candidate: " + candidate));
                }
                
                // do not consider candidates shorter than minLength
                if ((candidate.getEnd() - candidate.getBegin()) < minLength) {
                    continue;
                }
                
                List<String> spellingVariations = getSpellingVariations(candidate);
                int nrOfSpellingVariations = spellingVariations.size();
                for (String variation : spellingVariations) {

                    // TODO do not consider if variation is in DT of candidate
                    
                    List<String> changedWords = NGramDetectorUtils.getChangedWords(variation, words, candidatePosition);
                    
                    double changedSentenceProb = 
                        getSentenceProbability(changedWords) * (oneMinusAlpha / nrOfSpellingVariations);
                    
//                    System.out.println(changedWords.get(candidatePosition));
//                    System.out.println(changedSentenceProb);

                    if (changedSentenceProb > maxSentenceProb) {
                        maxSentenceProb = changedSentenceProb;
                        anomaly = getAnomaly(
                                tokens.get(candidatePosition),
                                changedWords.get(candidatePosition)
                        );
                    }
                }
            }

            // we found a sentence that has a higher probability
            if (maxSentenceProb > targetSentenceProb) {
                // add spelling anomaly 
                anomaly.addToIndexes();
            }
            
            // TODO if we aggregate all sentences with probability higher than we can use the same "permitting multiple corrections" variant from WOH_H_B
        }
    }

    /**
     * @param candidate
     * @return All spelling variations of candidate which can be found in the vocabulary.
     */
    private List<String> getSpellingVariations(RWSECandidate candidate) {
        List<String> variantList = new ArrayList<String>();
        
        for (String edit : SpellingUtils.getEditsInVocabulary(candidate.getCoveredText(), maxEditDistance, vocabulary)) {
            variantList.add(edit);
        }
            
        return variantList;
    }
    
    private SpellingAnomaly getAnomaly(Token token, String correct) {
        SpellingAnomaly anomaly = new SpellingAnomaly(jcas);
        anomaly.setBegin(token.getBegin());
        anomaly.setEnd(token.getEnd());
        anomaly.setSuggestions(SpellingUtils.getSuggestedActionArray(jcas, correct));
        
        return anomaly;
    }
}
