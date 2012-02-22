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
package de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining;

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

import de.tudarmstadt.ukp.dkpro.core.io.jwpl.type.WikipediaRevision;


public class SimpleResultWriter
    extends JCasConsumer_ImplBase
{
    
    public static final String LF = System.getProperty("line.separator");
    
    public static final String PARAM_OUTPUT_FILE = "OutputFile";
    @ConfigurationParameter(name=PARAM_OUTPUT_FILE, mandatory=false)
    private File outputFile;
    
    private BufferedWriter writer;
    
    @Override
    public void initialize(UimaContext context)
            throws ResourceInitializationException
    {
        super.initialize(context);
        
        if (outputFile == null) {
            outputFile = new File("target/output.txt");
        }
        
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

        StringBuilder sb = new StringBuilder();
        for (WikipediaRevision revision : JCasUtil.select(jcas, WikipediaRevision.class)) {
            sb.append(revision.getRevisionId()); sb.append(LF);
            sb.append(revision.getPageId()); sb.append(LF);
//            sb.append(revision.getUserId()); sb.append(LF);
            sb.append(revision.getComment()); sb.append(LF);
        }

        try {
            writer.write(sb.toString());
//            getContext().getLogger().log(Level.INFO, sb.toString());
            System.out.println(sb.toString());
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
        } catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
        super.collectionProcessComplete();
    }

}
