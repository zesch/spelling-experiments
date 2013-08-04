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

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createAggregate;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createAggregateDescription;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitiveDescription;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.core.treetagger.TreeTaggerPosLemmaTT4J;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.type.RWSECandidate;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.core.RWSECandidateAnnotator;
import de.tudarmstadt.ukp.similarity.dkpro.resource.test.TestSimilarityResource;
public class LexCohesionAnnotatorTest
{

    @Test
    public void testProcess()
        throws Exception
    {
        AnalysisEngineDescription segmenter = createPrimitiveDescription(
                BreakIteratorSegmenter.class
        );

        AnalysisEngineDescription tagger = createPrimitiveDescription(
                TreeTaggerPosLemmaTT4J.class,
                TreeTaggerPosLemmaTT4J.PARAM_LANGUAGE, "en"
        );

        AnalysisEngineDescription candidates = createPrimitiveDescription(
                RWSECandidateAnnotator.class,
                RWSECandidateAnnotator.PARAM_TYPE, Token.class.getName()
        );

        AnalysisEngineDescription cohesion= createPrimitiveDescription(
                LexCohesionDetector.class,
                LexCohesionDetector.PARAM_VOCABULARY, "classpath:/vocabulary/en_US_dict.txt",
                LexCohesionDetector.PARAM_LANGUAGE_CODE, "en",
                LexCohesionDetector.PARAM_MIN_LENGTH, 2,
                LexCohesionDetector.PARAM_THRESHOLD, 0.35f,
                LexCohesionDetector.SR_RESOURCE, ExternalResourceFactory.createExternalResourceDescription(
                        TestSimilarityResource.class
                )
        );
        
        AnalysisEngineDescription aggr = createAggregateDescription(
                segmenter,
                tagger,
                candidates,
                cohesion
        );
                
        AnalysisEngine engine = createAggregate(aggr);
        
        String text = "This is not a construcdion.";
        JCas jcas = engine.newJCas();
        jcas.setDocumentText(text);
        
        engine.process(jcas);

        int i=0;
        for (RWSECandidate c : JCasUtil.select(jcas, RWSECandidate.class)) {
            System.out.println(c);
            i++;
        }
        assertEquals(1, i);

        int j=0;
        for (SpellingAnomaly anomaly : JCasUtil.select(jcas, SpellingAnomaly.class)) {
            assertTrue(anomaly.getSuggestions(0).getReplacement().equals("construction"));  
            System.out.println(anomaly);
            j++;
        }
        assertEquals(1, j);
    }
}
