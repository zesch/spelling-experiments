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
import org.apache.uima.util.Level;
import org.uimafit.component.JCasConsumer_ImplBase;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.type.GoldSpellingAnomaly;

public class Dataset2CsvConverter
    extends JCasConsumer_ImplBase
{
    
    public static final String LF = System.getProperty("line.separator");
    public static final String SEP = "#";
    
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
        StringBuilder sb = new StringBuilder();
        for (GoldSpellingAnomaly gsa : JCasUtil.select(jcas, GoldSpellingAnomaly.class)) {
            sb.append(dmd.getCollectionId()); sb.append(SEP);
            sb.append(dmd.getDocumentId()); sb.append(SEP);
            sb.append(gsa.getSuggestions(0).getReplacement()); sb.append(SEP);
            sb.append(gsa.getCoveredText()); sb.append(SEP);
            sb.append(jcas.getDocumentText()); sb.append(LF);
        }
        try {
            writer.write(sb.toString());
            getContext().getLogger().log(Level.INFO, sb.toString());
        }
        catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
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
