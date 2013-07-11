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
import static org.junit.Assert.assertEquals;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.core.SpellingErrorCombinator.CombinationStrategy;

public class SpellingErrorCombinatorTest
{

    @Test
    public void testCombinatorJoin()
        throws Exception
    {
        AnalysisEngine combinator = createPrimitive(
                SpellingErrorCombinator.class,
                SpellingErrorCombinator.PARAM_COMBINATION_STRATEGY, CombinationStrategy.join.name()
        );

        String content  = "Thisk is a tset.";

        JCas jcas = combinator.newJCas();
        jcas.setDocumentText(content);

        addSpellingAnomalyAnnotation(jcas,  0,  4, 0.5f, "This");
        addSpellingAnomalyAnnotation(jcas,  0,  4, 0.4f, "That");
        addSpellingAnomalyAnnotation(jcas, 11, 14, 1.0f, "test");
        
        combinator.process(jcas);

        int i=0;
        for (SpellingAnomaly e : JCasUtil.select(jcas, SpellingAnomaly.class)) {
            System.out.println(e);

            if (e.getBegin() == 0) {
                assertEquals(2, e.getSuggestions().size());
            }
            
            i++;
        }
        assertEquals(2, i);
    }

    @Test
    public void testCombinatorKeepMultiple()
        throws Exception
    {
        AnalysisEngine combinator = createPrimitive(
                SpellingErrorCombinator.class,
                SpellingErrorCombinator.PARAM_COMBINATION_STRATEGY, CombinationStrategy.onlyKeepMultiple.name()
        );

        String content  = "Thisk is a tset.";

        JCas jcas = combinator.newJCas();
        jcas.setDocumentText(content);

        addSpellingAnomalyAnnotation(jcas,  0,  4, 0.5f, "This");
        addSpellingAnomalyAnnotation(jcas,  0,  4, 0.4f, "That");
        addSpellingAnomalyAnnotation(jcas, 11, 14, 1.0f, "test");

        combinator.process(jcas);

        int i=0;
        for (SpellingAnomaly e : JCasUtil.select(jcas, SpellingAnomaly.class)) {
            System.out.println(e);

            if (e.getBegin() == 0) {
                assertEquals(2, e.getSuggestions().size());
            }
            
            i++;
        }
        assertEquals(1, i);
    }

    private void addSpellingAnomalyAnnotation(JCas jcas, int begin, int end, float certainty, String ... suggestions) {
        SpellingAnomaly a = new SpellingAnomaly(jcas, begin, end);
        FSArray array = new FSArray(jcas, suggestions.length);
        int i=0;
        for (String suggestion : suggestions) {
            SuggestedAction action = new SuggestedAction(jcas);
            action.setReplacement(suggestion);
            action.setCertainty(certainty);
            array.set(i, action);
            i++;
        }
        a.setSuggestions(array);
        a.addToIndexes();
    }
    
}
