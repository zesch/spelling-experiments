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

import java.util.Map.Entry;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.descriptor.ConfigurationParameter;

import de.tudarmstadt.ukp.dkpro.core.api.featurepath.FeaturePathException;
import de.tudarmstadt.ukp.dkpro.core.api.featurepath.FeaturePathFactory;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.type.RWSECandidate;

/**
 * Adds RWSECandidate annotations for each annotation of a given type.
 * 
 * @author zesch
 *
 */
public class RWSECandidateAnnotator
	extends JCasAnnotator_ImplBase
{
    
    /**
     * The fully qualified name of the type that should be used for annotation.
     */
    public static final String PARAM_TYPE = "Type";
    @ConfigurationParameter(name=PARAM_TYPE, mandatory=true)
    private String type;

    @Override
	public void process(JCas jcas)
		throws AnalysisEngineProcessException
	{
	    
        try {
            for (Entry<AnnotationFS, String> entry : FeaturePathFactory.select(jcas.getCas(), type)) {
                AnnotationFS a = entry.getKey();
                RWSECandidate candidate = new RWSECandidate(jcas);
                candidate.setBegin(a.getBegin());
                candidate.setEnd(a.getEnd());
                candidate.addToIndexes();
            }
        }
        catch (FeaturePathException e) {
            throw new AnalysisEngineProcessException(e);
        }
	    
	}
}
