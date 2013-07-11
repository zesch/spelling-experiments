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
import org.apache.uima.fit.component.JCasConsumer_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.type.GoldSpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.core.DatasetItem;

public class ContextShrinker
    extends JCasConsumer_ImplBase
{
    
    public static final String PARAM_OUTPUT_FILE = "OutputFile";
    @ConfigurationParameter(name=PARAM_OUTPUT_FILE, mandatory=true)
    private File outputFile;
    
    private BufferedWriter writer;
    
    @Override
    public void initialize(UimaContext context)
            throws ResourceInitializationException
    {
        super.initialize(context);
        
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
        for (Sentence s : JCasUtil.select(jcas, Sentence.class)) {
            for (GoldSpellingAnomaly gsa : JCasUtil.selectCovered(jcas, GoldSpellingAnomaly.class, s)) {
                DatasetItem item = new DatasetItem(
                        gsa.getCoveredText(),
                        gsa.getSuggestions(0).getReplacement(),
                        0,
                        s.getCoveredText(),
                        Integer.parseInt(dmd.getCollectionId()),
                        Integer.parseInt(dmd.getDocumentId())
                );

                item = DatasetChecker.validateItem(item);
                try {
                    writeItem(writer, item);
                }
                catch (IOException e) {
                    throw new AnalysisEngineProcessException(e);
                }
            }
        }
    }

    private void writeItem(BufferedWriter writer, DatasetItem item) throws IOException {
        writer.write(item.toString());
        writer.newLine();
        writer.flush();
    }    
        
    @Override
    public void collectionProcessComplete()
        throws AnalysisEngineProcessException
    {
        try {
            writer.close();
        }
        catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
        super.collectionProcessComplete();
    }
}
