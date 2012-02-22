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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.uimafit.factory.AnalysisEngineFactory.createAggregate;
import static org.uimafit.factory.AnalysisEngineFactory.createAggregateDescription;
import static org.uimafit.factory.AnalysisEngineFactory.createPrimitiveDescription;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.jcas.JCas;
import org.junit.Ignore;
import org.junit.Test;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.core.treetagger.TreeTaggerPosLemmaTT4J;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.type.RWSECandidate;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.core.RWSECandidateAnnotator;

public class LexCohesionAnnotatorTest
{

    @Ignore
    @Test
    public void testProcess()
        throws Exception
    {
        AnalysisEngineDescription segmenter = createPrimitiveDescription(
                BreakIteratorSegmenter.class
        );

        AnalysisEngineDescription tagger = createPrimitiveDescription(
                TreeTaggerPosLemmaTT4J.class,
                TreeTaggerPosLemmaTT4J.PARAM_LANGUAGE_CODE, "en"
        );

        AnalysisEngineDescription candidates = createPrimitiveDescription(
                RWSECandidateAnnotator.class,
                RWSECandidateAnnotator.PARAM_TYPE, Token.class.getName()
        );

        AnalysisEngineDescription cohesion= createPrimitiveDescription(
                LexCohesionDetector.class,
                LexCohesionDetector.PARAM_VOCABULARY, "src/test/resources/vocabulary/test_vocabulary_lexcohesion.txt",
                LexCohesionDetector.PARAM_LANGUAGE_CODE, "en",
                LexCohesionDetector.PARAM_MIN_LENGTH, 2,
                LexCohesionDetector.PARAM_THRESHOLD, 0.5f
        );

        AnalysisEngineDescription aggr = createAggregateDescription(
                segmenter,
                tagger,
                candidates,
                cohesion
        );
                
        AnalysisEngine engine = createAggregate(aggr);
        
        String text = "this this is is his tis";
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
            assertTrue(anomaly.getSuggestions(0).getReplacement().equals("this"));  
            System.out.println(anomaly);
            j++;
        }
        assertEquals(1, j);
    }
}
