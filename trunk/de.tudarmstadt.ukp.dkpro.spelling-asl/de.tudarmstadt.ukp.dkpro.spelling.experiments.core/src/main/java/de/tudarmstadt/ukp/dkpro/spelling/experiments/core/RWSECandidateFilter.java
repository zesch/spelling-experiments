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

import static org.uimafit.util.JCasUtil.select;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.descriptor.ExternalResource;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.frequency.provider.FrequencyCountProvider;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.PUNC;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.resources.ResourceUtils;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.type.RWSECandidate;

/**
 * Filters RWSE candidate annotations according to certain conditions.
 * The remaining candidates are possible RWSEs.
 * 
 * @author zesch
 *
 */
public class RWSECandidateFilter
	extends JCasAnnotator_ImplBase
{
    
    /** 
     * What should be considered a low frequency. Depends on corpus used for lookup.
     */
    public static final String PARAM_LOW_FREQ = "LowFrequency";
    @ConfigurationParameter(name = PARAM_LOW_FREQ, mandatory=true, defaultValue="0")
    private int lowFreq;

    /** A list of words that should not be used as target words. **/
    public static final String PARAM_STOPWORD_LIST = "StopwordList";
    @ConfigurationParameter(name = PARAM_STOPWORD_LIST, mandatory = false)
    private String stopwordString;
    Set<String> stopwords;
    
    /** The minimum length of a candidate in characters. **/
    public static final String PARAM_MIN_LENGTH = "MinLength";
    @ConfigurationParameter(name = PARAM_MIN_LENGTH, mandatory = true, defaultValue="2")
    private int minLength;

    public static final String FREQUENCY_PROVIDER_RESOURCE = "FrequencyProvider";
    @ExternalResource(key = FREQUENCY_PROVIDER_RESOURCE)
    private FrequencyCountProvider frequencyProvider;

    @Override
    public void initialize(UimaContext context)
        throws ResourceInitializationException
    {
        super.initialize(context);

        // FIXME if candidates other than tokens are allowed, we need to generalize which n-grams are used for lookup
        try {
            stopwords = new HashSet<String>();
            if (stopwordString != null) {
                InputStream is = null;
                try {
                    URL url = ResourceUtils.resolveLocation(stopwordString, this, getContext());
                    is = url.openStream();
                    String content = IOUtils.toString(is, "UTF-8");
                    stopwords.addAll(Arrays.asList(content.split("\n")));
                }
                finally{
                    IOUtils.closeQuietly(is);
                }
            }
        }
        catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
    }
    
	@Override
	public void process(JCas jcas)
		throws AnalysisEngineProcessException
	{
	    
		Collection<RWSECandidate> toRemove = new ArrayList<RWSECandidate>();
		for (RWSECandidate c : select(jcas, RWSECandidate.class)) {
		    String item = c.getCoveredText();
			
		    // check for stopwords
		    if (stopwords.contains(item)) {
		        toRemove.add(c);
		    }
		    
		    // check for non-word (i.e. low frequency in web1t)
		    try {
                if (lowFreq > 0 && frequencyProvider.getFrequency(item) < lowFreq) {
                    toRemove.add(c);
                }
            }
            catch (Exception e) {
                throw new AnalysisEngineProcessException(e);
            }

            // shorter than minLength? -> do not consider as RWSE
            if (item.length() < minLength) {
                toRemove.add(c);
            }
            
        }

        // should not be part of a named entity
        for (NamedEntity ne : JCasUtil.select(jcas, NamedEntity.class)){
            for (RWSECandidate c : JCasUtil.selectCovered(jcas, RWSECandidate.class, ne)) {
                toRemove.add(c);
            }
        }
        
        // no punctuation
        for (PUNC punc : JCasUtil.select(jcas, PUNC.class)) {
            for (RWSECandidate c : JCasUtil.selectCovered(jcas, RWSECandidate.class, punc)) {
                toRemove.add(c);
            }
        }
        
		for (RWSECandidate c : toRemove) {
			c.removeFromIndexes();
		}
	}
}
