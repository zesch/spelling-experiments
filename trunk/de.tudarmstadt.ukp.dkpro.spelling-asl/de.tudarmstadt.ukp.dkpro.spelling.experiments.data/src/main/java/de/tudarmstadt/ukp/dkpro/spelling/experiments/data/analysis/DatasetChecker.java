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
package de.tudarmstadt.ukp.dkpro.spelling.experiments.data.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.component.JCasConsumer_ImplBase;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.type.GoldSpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.core.DatasetItem;

public class DatasetChecker
    extends JCasConsumer_ImplBase
{
    
    public static final String PARAM_OUTPUT_FILE = "OutputFile";
    @ConfigurationParameter(name=PARAM_OUTPUT_FILE, mandatory=true)
    private File outputFile;

    private BufferedWriter writer;
    
    private int itemCounter;
    
    @Override
    public void initialize(UimaContext context)
            throws ResourceInitializationException
    {
        super.initialize(context);
        
        itemCounter = 0;
        
        try {
            writer = new BufferedWriter(new FileWriter(outputFile));
        }
        catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
    }
    
    @Override
    public void process(JCas jcas)
        throws AnalysisEngineProcessException
    {
        DocumentMetaData dmd = DocumentMetaData.get(jcas);

        for (GoldSpellingAnomaly gsa : JCasUtil.select(jcas, GoldSpellingAnomaly.class)) {
            itemCounter++;
            DatasetItem item = new DatasetItem(
                gsa.getCoveredText(),
                gsa.getSuggestions(0).getReplacement(),
                gsa.getBegin(),
                jcas.getDocumentText(),
                Integer.parseInt(dmd.getCollectionId()),
                Integer.parseInt(dmd.getDocumentId())
            );
            
            item = validateItem(item);
            writeItem(writer, item);
        }
    }
    
    public static DatasetItem validateItem(DatasetItem item) {
        int startOffset = item.getOffset();
        int endOffset = startOffset + item.getWrong().length();
        
        String wrongInContext = item.getContext().substring(startOffset, endOffset);
        
        if (!item.getWrong().equals(wrongInContext)) {
            System.out.println("Wrong offset in item. Trying to correct.");
            
            int newOffset = item.getContext().indexOf(item.getWrong()); 
            if (newOffset != -1) {
                System.out.println("Correcting wrong offset " + item.getOffset() + " to " + newOffset);
                item.setOffset(newOffset);
            }
            else {
                System.out.println(item.getWrong() + " - not found in context: " + item.getContext());
            }
        }
        
        return item;
    }
    
    private void writeItem(BufferedWriter writer, DatasetItem item) throws AnalysisEngineProcessException {
        try {
            writer.write(item.toString());
            writer.newLine();
            writer.flush();
        }
        catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }
    
    @Override
    public void collectionProcessComplete()
        throws AnalysisEngineProcessException
    {
        System.out.println(itemCounter + " items.");
        
        try {
            writer.close();
        }
        catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
        super.collectionProcessComplete();
    }
}
