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

import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

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
public class SpellingErrorInContextReader_ShortFormat extends SpellingErrorInContextReader {

    private String nextItem;
    
    public boolean hasNext()
        throws IOException 
    {
        return (nextItem = bufferedReader.readLine()) != null;
    }

    @Override
    public void getNext(JCas jcas) throws IOException, CollectionException {

        // set language if it was explicitly specified as a configuration parameter
        if (language != null) {
            jcas.setDocumentLanguage(language);
        }

        String[] parts = nextItem.split("\t");
        if (parts.length != 4) {
            throw new IOException("Wrong file format on line " + currentIndex + ". " + nextItem);
        }

        String error   = parts[0];
        int offset     = new Integer(parts[1]);
        String correct = parts[2];
        String context = parts[3];
        
        jcas.setDocumentText(context);

        GoldSpellingAnomaly anomaly = new GoldSpellingAnomaly(jcas);
        anomaly.setBegin(offset);
        anomaly.setEnd(offset + error.length());
        anomaly.setSuggestions(SpellingUtils.getSuggestedActionArray(jcas, correct));
        anomaly.addToIndexes();

        DocumentMetaData docMetaData = DocumentMetaData.create(jcas);
        docMetaData.setDocumentTitle(new Integer(currentIndex).toString());
        docMetaData.setDocumentUri(inputFileString);
        docMetaData.setDocumentId(new Integer(currentIndex).toString());
        docMetaData.setCollectionId(inputFileString);

        currentIndex++;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public Progress[] getProgress() {
        return new Progress[] { new ProgressImpl(currentIndex, currentIndex, Progress.ENTITIES) };
    }
}
