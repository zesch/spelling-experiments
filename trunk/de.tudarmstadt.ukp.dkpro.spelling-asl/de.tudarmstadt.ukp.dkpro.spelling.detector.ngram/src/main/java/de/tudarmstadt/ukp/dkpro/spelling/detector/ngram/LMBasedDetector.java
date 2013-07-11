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
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;

import de.tudarmstadt.ukp.dkpro.core.api.frequency.provider.FrequencyCountProvider;
import de.tudarmstadt.ukp.dkpro.core.ngrams.util.NGramStringIterable;
import de.tudarmstadt.ukp.dkpro.spelling.api.detector.Detector_Base;

/**
 * An abstract base class for LM-based RWSE detectors.
 * 
 * @author zesch
 *
 */
public abstract class LMBasedDetector
    extends Detector_Base
{
 
    public final static String FREQUENCY_PROVIDER_RESOURCE = "FrequencyProvider";
    @ExternalResource(key = FREQUENCY_PROVIDER_RESOURCE)
    protected FrequencyCountProvider provider;

    /** 
     * The parameter alpha of the MDM model which controls how much probability mass is assigned to the spelling variations.
     */
    public static final String PARAM_ALPHA = "Alpha";
    @ConfigurationParameter(name = PARAM_ALPHA, mandatory=true, defaultValue="0.99")
    protected float alpha;    

    protected static final String BOS = "<S>";

    // local cache, global cache for all files would be too big
    protected Map<String,Long> countCache;
    
    protected double getSentenceProbability(List<String> words) throws AnalysisEngineProcessException  {
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
        trigrams.add(NGramDetectorUtils.getTrigram(BOS, BOS, words.get(0)));
        
        if (words.size() > 1) {
            trigrams.add(NGramDetectorUtils.getTrigram(BOS, words.get(0), words.get(1)));
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
    
    protected long getNGramCount(String ngram) throws AnalysisEngineProcessException {
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
