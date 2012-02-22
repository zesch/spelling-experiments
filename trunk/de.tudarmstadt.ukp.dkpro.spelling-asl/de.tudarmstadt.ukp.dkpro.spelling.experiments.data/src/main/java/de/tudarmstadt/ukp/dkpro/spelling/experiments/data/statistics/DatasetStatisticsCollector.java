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
package de.tudarmstadt.ukp.dkpro.spelling.experiments.data.statistics;

import java.util.Collection;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.type.GoldSpellingAnomaly;
import de.tudarmstadt.ukp.relatedness.api.RelatednessException;
import de.tudarmstadt.ukp.relatedness.api.TextRelatednessMeasure;
import de.tudarmstadt.ukp.relatedness.secondstring.LevenshteinSecondStringComparator;

/**
 * Collects statistics about the dataset.
 * 
 * @author zesch
 *
 */
public class DatasetStatisticsCollector
    extends JCasAnnotator_ImplBase
{
    
    private static final String LF = System.getProperty("line.separator");
    
    private TextRelatednessMeasure levenshteinComparator;

    private int nrOfItems;
    private int nrOfTokens;
    private int nrOfSentences;
    private int distanceSum;
    
    @Override
    public void initialize(UimaContext context)
        throws ResourceInitializationException
    {
        super.initialize(context);

        levenshteinComparator = new LevenshteinSecondStringComparator();
        
        nrOfItems = 0;
        nrOfTokens = 0;
        nrOfSentences = 0;
        distanceSum = 0;
    }
    
    
    @Override
    public void process(JCas jcas)
        throws AnalysisEngineProcessException
    {
        GoldSpellingAnomaly anomaly = JCasUtil.selectSingle(jcas, GoldSpellingAnomaly.class);
        
        try {
            distanceSum += levenshteinComparator.getRelatedness(
                    anomaly.getCoveredText(),
                    anomaly.getSuggestions(0).getReplacement()
            );
        }
        catch (RelatednessException e) {
            throw new AnalysisEngineProcessException(e);
        }
        
        Collection<Token> tokens = JCasUtil.select(jcas, Token.class);
        Collection<Sentence> sentences = JCasUtil.select(jcas, Sentence.class);
 
        nrOfTokens += tokens.size();
        nrOfSentences += sentences.size();
        
        nrOfItems++;
    }


    @Override
    public void collectionProcessComplete()
        throws AnalysisEngineProcessException
    {
        double avgNrOfTokens = (double) nrOfTokens / nrOfItems;
        double avgNrOfSentences = (double) nrOfSentences / nrOfItems;
        double avgEditDistance = (double) Math.abs(distanceSum) / nrOfItems;
        
        StringBuilder sb = new StringBuilder();
        sb.append("#items:              " + nrOfItems); sb.append(LF);
        sb.append("avg. #tokens:        " + avgNrOfTokens); sb.append(LF);
        sb.append("avg. #sentences:     " + avgNrOfSentences); sb.append(LF);
        sb.append("avg. edit distance:  " + avgEditDistance); sb.append(LF);
        sb.append(LF);
        
        System.out.println(sb.toString());
    }
}
