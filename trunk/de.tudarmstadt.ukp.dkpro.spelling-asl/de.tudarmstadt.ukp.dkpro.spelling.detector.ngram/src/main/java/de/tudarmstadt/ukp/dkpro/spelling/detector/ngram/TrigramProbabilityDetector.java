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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.Level;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.ngrams.util.NGramStringIterable;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.type.RWSECandidate;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.utils.SpellingUtils;

public class TrigramProbabilityDetector
    extends LMBasedDetector
{

    JCas jcas;

    // local cache, global cache for all files would be too big
    private Map<String,Long> countCache;
    
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
                
                int candidatePosition = getCandidatePosition(candidate, tokens);
                
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

                    List<String> changedWords = getChangedWords(variation, words, candidatePosition);
                    
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
    
    private double getSentenceProbability(List<String> words) throws AnalysisEngineProcessException  {
        double sentenceProbability = 0.0;
        
        if (words.size() < 1) {
            return 0.0;
        }
        
        long nrOfUnigrams;
        try {
            nrOfUnigrams = provider.getNrOfTokens();
        }
        catch (Exception e) {
            throw new AnalysisEngineProcessException(e);
        }
        
        List<String> trigrams = new ArrayList<String>();

        // in the google n-grams this is not represented (only single BOS markers)
        // but I leave it in place in case we add another n-gram provider
        trigrams.add(getTrigram(BOS, BOS, words.get(0)));
        
        if (words.size() > 1) {
            trigrams.add(getTrigram(BOS, words.get(0), words.get(1)));
        }
        
        for (String trigram : new NGramStringIterable(words, 3, 3)) {
            trigrams.add(trigram);
        }
        
        // FIXME - implement backoff or linear interpolation

        for (String trigram : trigrams) {
            long trigramFreq = getNGramCount(trigram);

            String[] parts = StringUtils.split(trigram, " ");
            
            String bigram = StringUtils.join(Arrays.copyOfRange(parts, 0, 2), " ");
            long bigramFreq = getNGramCount(bigram);
            
            String unigram = StringUtils.join(Arrays.copyOfRange(parts, 0, 1), " ");
            long unigramFreq = getNGramCount(unigram);

            if (trigramFreq < 1) {
                trigramFreq = 1;
            }
            if (bigramFreq < 1) {
                bigramFreq = 1;
            }
            if (unigramFreq < 1) {
                unigramFreq = 1;
            }
            
            double trigramProb = Math.log( (double) trigramFreq / bigramFreq);
            double bigramProb  = Math.log( (double) bigramFreq  / unigramFreq);
            double unigramProb = Math.log( (double) unigramFreq / nrOfUnigrams);

            double interpolated = (trigramProb + bigramProb + unigramProb) / 3.0;
            
            sentenceProbability += interpolated;
        }
        
        return Math.exp(sentenceProbability);
    }

    private String getTrigram(String s1, String s2, String s3) {
        StringBuilder sb = new StringBuilder();
        sb.append(s1);
        sb.append(" ");
        sb.append(s2);
        sb.append(" ");
        sb.append(s3);
        return sb.toString();
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
    
    private List<String> getChangedWords(String edit, List<String> words, int offset) {
        List<String> changedWords = new ArrayList<String>(words);
        changedWords.set(offset, edit);
            
        return changedWords;
    }

    private SpellingAnomaly getAnomaly(Token token, String correct) {
        SpellingAnomaly anomaly = new SpellingAnomaly(jcas);
        anomaly.setBegin(token.getBegin());
        anomaly.setEnd(token.getEnd());
        anomaly.setSuggestions(SpellingUtils.getSuggestedActionArray(jcas, correct));
        
        return anomaly;
    }

    private int getCandidatePosition(RWSECandidate candidate, List<Token> tokens)
    {
        int position = -1;
        
        for (int i=0; i<tokens.size(); i++) {
            if (tokens.get(i).getBegin() == candidate.getBegin() &&
                tokens.get(i).getEnd()   == candidate.getEnd())
            {
                position = i;
            }
        }

        return position;
    }
    
    private long getNGramCount(String ngram) throws AnalysisEngineProcessException {
        if (!countCache.containsKey(ngram)) {
            try {
                countCache.put(ngram, provider.getFrequency(ngram));
            }
            catch (Exception e) {
                throw new AnalysisEngineProcessException(e);
            }
        }
        
        return countCache.get(ngram);
    }
}
