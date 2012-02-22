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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.type.GoldSpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.utils.SpellingUtils;

/**
 * Reads gold standard spelling errors in context.
 *
 * <ul>
 * <li><code>InputFile</code> - the file containing the spelling error corpus</li>
 * <li><code>Language</code> (optional) - language of the input documents</li>
 * <li><code>Encoding</code> (optional) - character encoding of the input files</li>
 * </ul>
 *
 * @author zesch
 * 
 */
public class SpellingErrorInContextReader_LongFormat extends SpellingErrorInContextReader {

    private List<String> nextItem = null;
    
    @Override
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
        super.initialize(aContext);
    }

    public boolean hasNext()
        throws IOException 
    {
        if (nextItem == null) {
            nextItem = getNextItem();
        }
        
        return nextItem != null;
    }

    @Override
    public void getNext(JCas jcas) throws IOException, CollectionException {

        // set language if it was explicitly specified as a configuration parameter
        if (language != null) {
            jcas.setDocumentLanguage(language);
        }

        if (nextItem.size() != 7) {
            throw new IOException("Wrong file format.");
        }

        if (!nextItem.get(6).equals("")) {
            System.out.println(nextItem.get(6));
            throw new IOException("Wrong file format.");
            
        }
        
        String pageId  = nextItem.get(0);
        String revId   = nextItem.get(1);
        String error   = nextItem.get(2);
        String correct = nextItem.get(3);
        int offset     = new Integer(nextItem.get(4));
        String context = nextItem.get(5);
        
        jcas.setDocumentText(context);

        GoldSpellingAnomaly anomaly = new GoldSpellingAnomaly(jcas);
        anomaly.setBegin(offset);
        anomaly.setEnd(offset + error.length());
        anomaly.setSuggestions(SpellingUtils.getSuggestedActionArray(jcas, correct));
        anomaly.addToIndexes();

        DocumentMetaData docMetaData = DocumentMetaData.create(jcas);
        docMetaData.setDocumentTitle(new Integer(currentIndex).toString());
        docMetaData.setDocumentUri(inputFileString);
        docMetaData.setDocumentId(revId);
        docMetaData.setCollectionId(pageId);

        currentIndex++;
        
        // set to null, so that next call of hasNext() tries to fill the item again
        nextItem = null;
    }

    private List<String> getNextItem() throws IOException {
        List<String> item = new ArrayList<String>();
        
        // try to read next seven lines
        String line;
        for (int i=0; i<7; i++) {
            line = bufferedReader.readLine();
            if (line == null) {
                return null;
            }
            
            item.add(line);
        }
        
        return item;
    }
}
