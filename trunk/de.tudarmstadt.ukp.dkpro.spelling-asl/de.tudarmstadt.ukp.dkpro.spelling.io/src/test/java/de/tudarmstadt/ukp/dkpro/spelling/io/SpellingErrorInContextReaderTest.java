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

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.junit.Ignore;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;

public class SpellingErrorInContextReaderTest
{
    @Ignore
    @Test
    public void shortFormatTest() throws Exception {
        runTest("classpath:/io/test_shortFormat.txt", false);
    }

    @Test
    public void longFormatTest() throws Exception {
        runTest("classpath:/io/test_longFormat.txt", true);
    }

    @Test
    public void longFormatGZipTest() throws Exception {
        runTest("classpath:/io/test_longFormat.txt.gz", true);
    }

    private void runTest(String inputFile, boolean isLongFormat) throws UIMAException, IOException {
        String[] error = new String[] {
                "chance",
                "tolerance",
                "goals"
            };

        String[] correct = new String[] {
            "change",
            "tolerant",
            "jails"
        };

        String[] expectedContext = new String[] {
            "People may have started farming because the weather and soil began to chance.",
            "They also chose plants that are more resistance to disease, more tolerance to drought, and those that are easier to harvest.",
            "By the 1780s the goals of England were so full that convicts were often chained up in rotting old ships."
        };

        
        CollectionReader reader;
        if (isLongFormat) {
            reader = CollectionReaderFactory.createCollectionReader(
                    SpellingErrorInContextReader_LongFormat.class,
                    SpellingErrorInContextReader.PARAM_INPUT_FILE, inputFile
            );
        }
        else {
            reader = CollectionReaderFactory.createCollectionReader(
                    SpellingErrorInContextReader_ShortFormat.class,
                    SpellingErrorInContextReader.PARAM_INPUT_FILE, inputFile
            );
        }

        int i = 0;
        for (JCas jcas : new JCasIterable(reader)) {
            assertEquals(expectedContext[i], jcas.getDocumentText());
            

            SpellingAnomaly anomaly = JCasUtil.selectSingle(jcas, SpellingAnomaly.class);
            assertEquals(correct[i], anomaly.getSuggestions(0).getReplacement());
            assertEquals(error[i], anomaly.getCoveredText());
            
            i++;
        }

        // there are 3 documents in the test set
        assertEquals(3,i);
   
    }
}
