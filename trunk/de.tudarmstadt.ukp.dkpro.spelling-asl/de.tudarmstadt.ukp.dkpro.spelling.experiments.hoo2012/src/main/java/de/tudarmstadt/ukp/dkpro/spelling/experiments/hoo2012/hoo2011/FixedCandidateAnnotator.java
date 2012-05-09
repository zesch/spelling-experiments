/*******************************************************************************
 * Copyright 2012
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
package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.hoo2011;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.descriptor.ConfigurationParameter;

import de.tudarmstadt.ukp.dkpro.core.api.featurepath.FeaturePathException;
import de.tudarmstadt.ukp.dkpro.core.api.featurepath.FeaturePathFactory;
import de.tudarmstadt.ukp.dkpro.core.api.resources.ResourceUtils;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.type.RWSECandidate;

/**
 * Adds RWSECandidate annotations for each annotation of a given type.
 * 
 * @author zesch
 *
 */
public class FixedCandidateAnnotator
	extends JCasAnnotator_ImplBase
{
    
	/** 
     * A file containing a list of candidates (each candiates on a single line).
     * Only the candidates are considered as possible corrections.
     * Can e.g. be used for article or preposition correction.
     */
    public static final String PARAM_CANDIDATE_FILE = "CandidateFile";
    @ConfigurationParameter(name = PARAM_CANDIDATE_FILE, mandatory=true)
    protected String candidateFileString;    
    
    /**
     * The fully qualified name of the type that should be used for annotation.
     */
    public static final String PARAM_TYPE = "Type";
    @ConfigurationParameter(name=PARAM_TYPE, mandatory=true)
    private String type;

    protected Set<String> candidateSet;
    
    @Override
	public void initialize(UimaContext context)
			throws ResourceInitializationException
	{
		super.initialize(context);
		
	        candidateSet = new HashSet<String>();
	        
	        try {
	            InputStream is = null;
	            try {
	                URL url = ResourceUtils.resolveLocation(candidateFileString, this, getContext());
	                is = url.openStream();
	                String content = IOUtils.toString(is, "UTF-8");
	                for (String item : content.split("\n")) {
	                    if (!item.startsWith("#")) {
	                        candidateSet.add( item );
	                    }
	                }
	            }
	            finally{
	                IOUtils.closeQuietly(is);
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
	    
        try {
            for (Entry<AnnotationFS, String> entry : FeaturePathFactory.select(jcas.getCas(), type)) {
                AnnotationFS a = entry.getKey();
                if (candidateSet.contains(a.getCoveredText())) {
                    RWSECandidate candidate = new RWSECandidate(jcas);
                    candidate.setBegin(a.getBegin());
                    candidate.setEnd(a.getEnd());
                    candidate.addToIndexes();
                }
            }
        }
        catch (FeaturePathException e) {
            throw new AnalysisEngineProcessException(e);
        }
	    
	}
}
