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
package de.tudarmstadt.ukp.dkpro.spelling.io;

import static org.junit.Assert.assertEquals;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.utils.SpellingUtils;

public class SpellingErrorInContextEvaluatorTest
{
    
    static int[] offsets = new int[] {
        70,
        65,
        17,
        0
    };

    static String[] errors = new String[] {
        "chance",
        "tolerance",
        "goals",
        "wrong"
    };

    static String[] correctSuggestions = new String[] {
            "change",
            "tolerant",
            "jails",
            "wrong"
    };
    
    static String[] wrongSuggestions = new String[] {
        "wrong",
        "wrong",
        "wrong",
        "wrong"
    };

    private CollectionReaderDescription getReader() throws ResourceInitializationException {
        return CollectionReaderFactory.createReaderDescription(
                SpellingErrorInContextReader_LongFormat.class,
                SpellingErrorInContextReader.PARAM_INPUT_FILE, "classpath:/io/test_longFormat.txt"
        );
    }

    private AnalysisEngine getEngine() throws ResourceInitializationException {
        return AnalysisEngineFactory.createEngine(
                SpellingErrorInContextEvaluator.class
        );
    }
    
    @Test
    public void spellingEvaluatorTest_allCorrect() throws Exception {

        System.out.println("all correct");
        
        AnalysisEngine ae = getEngine();
        
        int i = 0;
        for (JCas jcas : new JCasIterable(getReader())) {
            addAnnotation(jcas, i, true);
            ae.process(jcas);
            i++;
        }
        // there are 3 documents in the test set
        assertEquals(3,i);

        ae.collectionProcessComplete();
    }
    
    @Test
    public void spellingEvaluatorTest_multipleAllCorrect() throws Exception {

        System.out.println("multiple all correct");
        
        AnalysisEngine ae = getEngine();
        
        int i = 0;
        for (JCas jcas : new JCasIterable(getReader())) {
            addAnnotation(jcas, i, true);
            addAnnotation(jcas, i, true);
            ae.process(jcas);
            i++;
        }
        // there are 3 documents in the test set
        assertEquals(3,i);

        ae.collectionProcessComplete();
    }

    @Test
    public void spellingEvaluatorTest_allWrong() throws Exception {

        System.out.println("all wrong");

        AnalysisEngine ae = getEngine();
        
        int i = 0;
        for (JCas jcas : new JCasIterable(getReader())) {
            addAnnotation(jcas, 3, false);
            ae.process(jcas);
            i++;
        }
        // there are 3 documents in the test set
        assertEquals(3,i);

        ae.collectionProcessComplete();
    }
    
    @Test
    public void spellingEvaluatorTest_multipleAllWrong() throws Exception {

        System.out.println("multiple all wrong");
        
        AnalysisEngine ae = getEngine();
        
        int i = 0;
        for (JCas jcas : new JCasIterable(getReader())) {
            addAnnotation(jcas, 3, false);
            addAnnotation(jcas, 3, false);
            ae.process(jcas);
            i++;
        }
        // there are 3 documents in the test set
        assertEquals(3,i);

        ae.collectionProcessComplete();
    }
    
    @Test
    public void spellingEvaluatorTest_correctionWrong() throws Exception {

        System.out.println("correction wrong");

        AnalysisEngine ae = getEngine();
        
        int i = 0;
        for (JCas jcas : new JCasIterable(getReader())) {
            addAnnotation(jcas, i, false);
            ae.process(jcas);
            i++;
        }
        // there are 3 documents in the test set
        assertEquals(3,i);

        ae.collectionProcessComplete();
    }
    
    
    @Test
    public void spellingEvaluatorTest_mixed() throws Exception {

        System.out.println("mixed");
        
        AnalysisEngine ae = getEngine();
        
        int i = 0;
        for (JCas jcas : new JCasIterable(getReader())) {
            if (i<2) {
                addAnnotation(jcas, i+1, false);
                addAnnotation(jcas, i, true);
            }
            
            addAnnotation(jcas, i, true);
            
            ae.process(jcas);
            i++;
        }
        // there are 3 documents in the test set
        assertEquals(3,i);

        ae.collectionProcessComplete();
    }
    
    private void addAnnotation(JCas jcas, int i, boolean isCorrect) {
        String suggestion = "";
        if (isCorrect) {
            suggestion = correctSuggestions[i];
        }
        else {
            suggestion = wrongSuggestions[i];
        }
        
        SpellingAnomaly anomaly = new SpellingAnomaly(jcas);
        anomaly.setBegin(offsets[i]);
        anomaly.setEnd(offsets[i] + errors[i].length());
        anomaly.setSuggestions(SpellingUtils.getSuggestedActionArray(jcas, suggestion));
        anomaly.addToIndexes();
    }
}
