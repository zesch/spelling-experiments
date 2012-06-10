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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.uimafit.factory.AnalysisEngineFactory.createAggregate;
import static org.uimafit.factory.AnalysisEngineFactory.createAggregateDescription;
import static org.uimafit.factory.AnalysisEngineFactory.createPrimitiveDescription;
import static org.uimafit.factory.ExternalResourceFactory.bindResource;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.jcas.JCas;
import org.junit.Test;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.frequency.TestFrequencyCountResource;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.core.treetagger.TreeTaggerPosLemmaTT4J;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.type.RWSECandidate;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.core.RWSECandidateAnnotator;

public class TrigramProbabilityDetectorTest
{

    @Test
    public void testProcess()
        throws Exception
    {
        AnalysisEngineDescription segmenter = createPrimitiveDescription(
                BreakIteratorSegmenter.class
        );

        AnalysisEngineDescription tagger = createPrimitiveDescription(
                OpenNlpPosTagger.class
        );

        AnalysisEngineDescription candidates = createPrimitiveDescription(
                RWSECandidateAnnotator.class,
                RWSECandidateAnnotator.PARAM_TYPE, Token.class.getName()
        );

        AnalysisEngineDescription detector = createPrimitiveDescription(
                TrigramProbabilityDetector.class,
                TrigramProbabilityDetector.PARAM_LANGUAGE_CODE, "en",
                TrigramProbabilityDetector.PARAM_VOCABULARY, "src/test/resources/vocabulary/test_vocabulary_trigram.txt",
                TrigramProbabilityDetector.PARAM_MIN_LENGTH, 2,
                TrigramProbabilityDetector.PARAM_ALPHA, 0.7f
        );

        bindResource(
                detector,
                LMBasedDetector.FREQUENCY_PROVIDER_RESOURCE,
                TestFrequencyCountResource.class
        );

        AnalysisEngineDescription aggr = createAggregateDescription(
                segmenter,
                tagger,
                candidates,
                detector
        );
        
        AnalysisEngine engine = createAggregate(aggr);
        
        String text = "aaa bbb ccc ddd ee fff";
        JCas jcas = engine.newJCas();
        jcas.setDocumentLanguage("en");
        jcas.setDocumentText(text);
        
        engine.process(jcas);

        int i=0;
        for (RWSECandidate c : JCasUtil.select(jcas, RWSECandidate.class)) {
            System.out.println(c);
            i++;
        }
        assertEquals(6, i);

        int j=0;
        for (SpellingAnomaly anomaly : JCasUtil.select(jcas, SpellingAnomaly.class)) {
            assertTrue(anomaly.getSuggestions(0).getReplacement().equals("eee"));  
            System.out.println(anomaly);
            j++;
        }
        assertEquals(1, j);
    }
}
