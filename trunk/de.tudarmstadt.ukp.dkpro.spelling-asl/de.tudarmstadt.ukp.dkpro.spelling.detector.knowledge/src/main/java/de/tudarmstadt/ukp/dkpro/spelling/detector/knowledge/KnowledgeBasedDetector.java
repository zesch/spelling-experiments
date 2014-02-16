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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.Level;

import de.tudarmstadt.ukp.dkpro.core.api.frequency.util.FrequencyDistribution;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.type.RWSECandidate;
import de.tudarmstadt.ukp.dkpro.spelling.api.detector.Detector_Base;
import dkpro.similarity.algorithms.api.SimilarityException;
import dkpro.similarity.algorithms.api.TermSimilarityMeasure;

/**
 * An abstract base class for knowledge-based RWSE detectors.
 * 
 * @author zesch
 *
 */
public abstract class KnowledgeBasedDetector
    extends Detector_Base
{
    
    protected FrequencyDistribution<String> freqDistTokens;
    protected FrequencyDistribution<String> freqDistLemmas;
    
    public final static String SR_RESOURCE = "SemanticRelatednessResource";
    @ExternalResource(key = SR_RESOURCE)
    protected TermSimilarityMeasure measure;

    /**
     * The relatedness threshold for the given measure.
     * For relatedness measures, a value higher than threshold is considered to be semantically related.
     * For distance measures, the value needs to be lower. 
     */
    public static final String PARAM_THRESHOLD = "Threshold";
    @ConfigurationParameter(name = PARAM_THRESHOLD, mandatory=true)
    protected float threshold;
    
    @Override
    public void process(JCas jcas)
        throws AnalysisEngineProcessException
    {
        
        // sanity check
        if (JCasUtil.select(jcas, RWSECandidate.class).size() == 0) {
            getContext().getLogger().log(Level.WARNING, "No RWSECandidate annotations present. Probably the pipeline is not properly configured.");
            return;
        }
        
        // get a freqDist of all tokens and all lemmas and check whether the candidate token or lemma appears more than once
        // (it needs to appear at least once, as it is contained in the freqDist itself)
        freqDistTokens = new FrequencyDistribution<String>();
        freqDistLemmas = new FrequencyDistribution<String>();
        for (Token t : JCasUtil.select(jcas, Token.class)) {
            String token = t.getCoveredText();
            if (lowerCase) {
                token.toLowerCase();
            }
            freqDistTokens.inc(token);
        }
        for (Lemma l : JCasUtil.select(jcas, Lemma.class)) {
            String lemmaString = l.getValue(); 
            if (lowerCase) {
                lemmaString.toLowerCase();
            }
            freqDistLemmas.inc(lemmaString);
        }
        
        // first step
        //
        // remove candidates  
        // a) whose token/lemma appears again in the context
        // b) that are semantically related to some word in the context
        // c) that appear in a known multiword compound
        removeLexicalCohesiveCandidates(jcas);
        
        
        // second step
        //
        // for remaining candidates -> generate spelling variations and test lexical cohesion
        generateAndTest(jcas);
    }

    protected abstract void generateAndTest(JCas jcas) throws AnalysisEngineProcessException;
    
    private void removeLexicalCohesiveCandidates(JCas jcas) throws AnalysisEngineProcessException {
        
        Collection<RWSECandidate> toRemove = new ArrayList<RWSECandidate>();
        
        for (RWSECandidate candidate : JCasUtil.select(jcas, RWSECandidate.class)) {
            List<Lemma> lemmaList = JCasUtil.selectCovered(jcas, Lemma.class, candidate);
            Lemma lemma = null;
            if (lemmaList.size() > 0) {
                lemma = lemmaList.get(0);
            }
            if (tokenAppearsInContext(candidate.getCoveredText()) ||
                lemmaAppearsInContext(lemma.getValue()) ||
                isSemanticallyRelated(lemma.getValue()))
            {
                this.getContext().getLogger().log(
                        Level.FINE,
                        "Removed cohesive candidate: " + candidate.getCoveredText()
                );
                
                toRemove.add(candidate);
            }
        }
        
        // TODO multiword compounds mutually confirming - how to obtain the multiword list?
        // -> we decided not to use this step, as it
        //    (a) is another parameter (how to obtain this list?), and
        //    (b) is strongly connected to the language model based approach 
        
        for (RWSECandidate c : toRemove) {
            c.removeFromIndexes();
        }
    }
    
    protected boolean tokenAppearsInContext(String term) {
        if (term != null) {
            if (lowerCase) {
                term = term.toLowerCase();
            }
            
            if (freqDistTokens.getCount(term) > 1) {
                return true;
            }
        }
        return false;
    }

    // > 1 as the term/lemma itself is always in the freqDist too
    protected boolean lemmaAppearsInContext(String term) {
        if (term != null) {
            if (lowerCase) {
                term = term.toLowerCase();
            }
            
            if (freqDistLemmas.getCount(term) > 1) {
                return true;
            }
        }
        return false;
    }

    protected boolean isSemanticallyRelated(String lemma) throws AnalysisEngineProcessException {
        if (lemma != null) {
            if (lowerCase) {
                lemma = lemma.toLowerCase();
            }
            for (String item : freqDistLemmas.getKeys()) {

                // should not match with itself - this was already tested
                if (item.equals(lemma)) {
                    continue;
                }
                
                try {
                    if (measure.isDistanceMeasure()) {
                        if (measure.getSimilarity(lemma, item) < this.threshold) {
                            return true;
                        }
                    }
                    else {
                        if (measure.getSimilarity(lemma, item) > this.threshold) {
                            return true;
                        }
                    }
                }
                catch (SimilarityException e) {
                    throw new AnalysisEngineProcessException(e);
                }
                
            }
        }
        return false;
    }
}