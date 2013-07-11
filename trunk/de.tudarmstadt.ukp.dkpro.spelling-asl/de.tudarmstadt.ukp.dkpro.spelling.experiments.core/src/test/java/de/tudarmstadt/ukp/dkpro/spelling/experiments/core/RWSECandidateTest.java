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

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitive;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;
import static org.junit.Assert.assertEquals;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.api.frequency.TestFrequencyCountResource;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.type.RWSECandidate;

public class RWSECandidateTest
{

    @Test
    public void testProcess()
        throws Exception
    {
        AnalysisEngine annotator = createPrimitive(
                RWSECandidateAnnotator.class,
                RWSECandidateAnnotator.PARAM_TYPE, Token.class.getName()
        );

        String content  = "Thsik is a Named Entity";
        JCas jcas = annotator.newJCas();
        
        TokenBuilder<Token, Annotation> tb = new TokenBuilder<Token, Annotation>(Token.class, Annotation.class);
        tb.buildTokens(jcas, content);
        annotator.process(jcas);

        int i=0;
        for (RWSECandidate c : JCasUtil.select(jcas, RWSECandidate.class)) {
            System.out.println(c);
            i++;
        }
        assertEquals(5, i);
        
        NamedEntity ne = new NamedEntity(jcas);
        ne.setBegin(10);
        ne.setEnd(23);
        ne.addToIndexes();
        
        AnalysisEngineDescription filter = createPrimitiveDescription(
                        RWSECandidateFilter.class,
                        RWSECandidateFilter.PARAM_LOW_FREQ, 1000,
                        RWSECandidateFilter.FREQUENCY_PROVIDER_RESOURCE,
                            ExternalResourceFactory.createExternalResourceDependencies(TestFrequencyCountResource.class)

        );
        
        AnalysisEngine engine = createPrimitive(filter);
        
        engine.process(jcas);

        int j=0;
        for (RWSECandidate c : JCasUtil.select(jcas, RWSECandidate.class)) {
            System.out.println(c);
            j++;
        }
        assertEquals(1, j);

    }
}
