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
package de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining;

import static org.uimafit.util.JCasUtil.select;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.uimafit.component.JCasAnnotator_ImplBase;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;

public class SentenceFilter
	extends JCasAnnotator_ImplBase
{
    
	@Override
	public void process(JCas aJCas)
		throws AnalysisEngineProcessException
	{
	    
		Collection<Sentence> toRemove = new ArrayList<Sentence>();
		for (Sentence s : select(aJCas, Sentence.class)) {
		    String text = s.getCoveredText();
			
		    // too many ":" -> likely to be Wikipedia language link list
		    if (StringUtils.countMatches(text, ":") > 3) {
			    toRemove.add(s);
			}
		    
		    // remove very short sentences
		    if (text.length() < 5) {
		        toRemove.add(s);
		    }
		}
		
		// TODO should we also remove underlying tokens?
		// as we add context according to sentences - it might not be necessary
		for (Sentence s : toRemove) {
			s.removeFromIndexes();
		}
	}
}
