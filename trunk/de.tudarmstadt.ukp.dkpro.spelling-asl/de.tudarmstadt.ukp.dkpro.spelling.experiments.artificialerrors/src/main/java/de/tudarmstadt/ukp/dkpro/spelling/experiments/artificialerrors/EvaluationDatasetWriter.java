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
package de.tudarmstadt.ukp.dkpro.spelling.experiments.artificialerrors;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.uimafit.component.JCasConsumer_ImplBase;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.core.DatasetItem;

public class EvaluationDatasetWriter
    extends JCasConsumer_ImplBase
{
    
    public static final String LF = System.getProperty("line.separator");
    
    public static final String PARAM_OUTPUT_FILE = "OutputFile";
    @ConfigurationParameter(name=PARAM_OUTPUT_FILE, mandatory=false, defaultValue="target/output.txt")
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

        for (Sentence s : JCasUtil.select(jcas, Sentence.class)) {
            StringBuilder sb = new StringBuilder();
            List<SpellingAnomaly> anomalies = JCasUtil.selectCovered(jcas, SpellingAnomaly.class, s);
            
            if (anomalies.size() > 0) {
                for (SpellingAnomaly anomaly : anomalies) {
                    Token token = JCasUtil.selectCovered(jcas, Token.class, anomaly).iterator().next();
                    
                    sb.append(
                            new DatasetItem(
                                    anomaly.getSuggestions(0).getReplacement(),
                                    token.getCoveredText(),
                                    token.getBegin() - s.getBegin(),
                                    getWrongSentence(s, token, anomaly.getSuggestions(0).getReplacement())
                            )
                    );
                    sb.append(LF);
                }
            }
            try {
                writer.write(sb.toString());
                getContext().getLogger().log(Level.INFO, sb.toString());
            }
            catch (IOException e) {
                throw new AnalysisEngineProcessException(e);
            }
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

    
    /**
     * @return Inserts the wrong string in the sentence s instead of token t.
     */
    private String getWrongSentence(Sentence s, Token t, String wrongToken) {
        int startOffset = t.getBegin() - s.getBegin();
        int endOffset = t.getEnd() - s.getBegin();
        
        String sentence = s.getCoveredText();

        if (endOffset > sentence.length()) {
            endOffset = sentence.length();
        }
        
        return sentence.substring(0, startOffset) + wrongToken + sentence.substring(endOffset, sentence.length());
    }
}
