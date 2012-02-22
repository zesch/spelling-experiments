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
import static org.uimafit.factory.CollectionReaderFactory.createCollectionReader;

import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.junit.Test;
import org.uimafit.pipeline.JCasIterable;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.io.ResourceCollectionReaderBase;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.type.GoldSpellingAnomaly;

/**
 * Class to test the HOOReader.
 * 
 * @author Irina Smidt
 * 
 */
public class HOOReaderTest {

	@Test
	public void hooReaderTest() throws Exception {
		CollectionReader reader = createCollectionReader(
		        HOOReader.class,
				HOOReader.PARAM_LANGUAGE, "en",
				HOOReader.PARAM_PATH,       "src/test/resources/hoo/text/",
				HOOReader.PARAM_EDITS_PATH, "src/test/resources/hoo/edit/",
				HOOReader.PARAM_PATTERNS,
				    new String[] {
		                ResourceCollectionReaderBase.INCLUDE_PREFIX + "*.txt"
		            }
		);

		int i = 0;
		for (JCas jcas : new JCasIterable(reader)) {
			if (i == 0) {
				assertEquals(39, JCasUtil.select(jcas, GoldSpellingAnomaly.class).size());
			} else if (i == 1) {
				assertEquals(56, JCasUtil.select(jcas, GoldSpellingAnomaly.class).size());
			}
			i++;
		}
		
		assertEquals(i, 2);
	}

	@Test(expected=RuntimeException.class)
	public void hooReaderTest_noEdits() throws Exception {

        CollectionReader reader = createCollectionReader(
                HOOReader.class,
                HOOReader.PARAM_LANGUAGE, "en",
                HOOReader.PARAM_PATH,       "src/test/resources/hoo/text/",
                HOOReader.PARAM_EDITS_PATH, "src/test/resources/hoo/edits/",
                HOOReader.PARAM_PATTERNS,
                    new String[] {
                        ResourceCollectionReaderBase.INCLUDE_PREFIX + "*.txt"
                    }
        );

		int i = 0;
		for (JCas jcas : new JCasIterable(reader)) {
			if (i == 0) {
				assertEquals(0, JCasUtil.select(jcas, GoldSpellingAnomaly.class).size());
			} else if (i == 1) {
				assertEquals(0, JCasUtil.select(jcas, GoldSpellingAnomaly.class).size());
			}
			i++;
		}
		
		assertEquals(i, 2);
	}

}
