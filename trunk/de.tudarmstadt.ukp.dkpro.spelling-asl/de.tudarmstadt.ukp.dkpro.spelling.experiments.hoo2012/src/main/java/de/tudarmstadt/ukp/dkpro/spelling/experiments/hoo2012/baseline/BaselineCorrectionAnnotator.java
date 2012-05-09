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
package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.baseline;

import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.Level;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.type.RWSECandidate;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.utils.SpellingUtils;

public class BaselineCorrectionAnnotator
    extends JCasAnnotator_ImplBase
{
    
	public static final String PARAM_REPLACEMENT = "Replacement";
    @ConfigurationParameter(name = PARAM_REPLACEMENT, mandatory=true)
    protected String replacement;
    
	@Override
    public void process(JCas jcas)
        throws AnalysisEngineProcessException
    {
       
        // sanity check
        if (JCasUtil.select(jcas, RWSECandidate.class).size() == 0) {
            getContext().getLogger().log(Level.WARNING, "No RWSECandidate annotations present. Probably the pipeline is not properly configured.");
            getContext().getLogger().log(Level.WARNING, jcas.getDocumentText());
            return;
        }
        
        for (RWSECandidate candidate : JCasUtil.select(jcas, RWSECandidate.class)) {
        	String possibleError;
        	List<Lemma> lemmas = JCasUtil.selectCovered(jcas, Lemma.class, candidate);
        	if (lemmas.size() > 0) {
        		possibleError = lemmas.get(0).getValue();
        	}
        	else {
        		possibleError = candidate.getCoveredText();
        	}
        	
        	if (!possibleError.equals(replacement)) {
                SpellingAnomaly anomaly = new SpellingAnomaly(jcas);
                anomaly.setBegin(candidate.getBegin());
                anomaly.setEnd(candidate.getEnd());
                anomaly.setSuggestions(
                		SpellingUtils.getSuggestedActionArray(jcas, replacement)
                );
                anomaly.addToIndexes();        		
        	}
        }
    }
}
