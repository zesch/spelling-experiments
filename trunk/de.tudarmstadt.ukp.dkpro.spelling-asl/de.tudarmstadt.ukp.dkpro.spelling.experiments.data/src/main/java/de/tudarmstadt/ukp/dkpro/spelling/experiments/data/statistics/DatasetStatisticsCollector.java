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
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.frequency.util.FrequencyDistribution;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.type.GoldSpellingAnomaly;

/**
 * Collects statistics about the dataset.
 * 
 * @author zesch
 *
 */
public class DatasetStatisticsCollector
    extends JCasAnnotator_ImplBase
{
    
    public static final String PARAM_INCLUDE_FD = "IncludeFD";
    @ConfigurationParameter(name = PARAM_INCLUDE_FD, mandatory = true, defaultValue="false")
    private boolean includeFD;

    
    private static final String LF = System.getProperty("line.separator");
    
    private LevenshteinDistance levenshteinComparator;
    private FrequencyDistribution<String> fd;

    private int nrOfItems;
    private int nrOfTokens;
    private int nrOfSentences;
    private int distanceSum;
    
    @Override
    public void initialize(UimaContext context)
        throws ResourceInitializationException
    {
        super.initialize(context);

        levenshteinComparator = new LevenshteinDistance();
        fd = new FrequencyDistribution<String>();
        
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
        
        fd.inc(anomaly.getCoveredText() + "-" + anomaly.getSuggestions(0).getReplacement());
        distanceSum += levenshteinComparator.computeLevenshteinDistance(
                anomaly.getCoveredText(),
                anomaly.getSuggestions(0).getReplacement()
        );
        
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
        
        if (includeFD) {
            sb.append(fd);
            sb.append(LF);
        }
        
        System.out.println(sb.toString());
    }
    
    // from http://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance
    class LevenshteinDistance {
        private int minimum(int a, int b, int c) {
                return Math.min(Math.min(a, b), c);
        }
 
        public int computeLevenshteinDistance(CharSequence str1, CharSequence str2) {
                int[][] distance = new int[str1.length() + 1][str2.length() + 1];
 
                for (int i = 0; i <= str1.length(); i++) {
                    distance[i][0] = i;
                }
                for (int j = 0; j <= str2.length(); j++) {
                    distance[0][j] = j;
                }
 
                for (int i = 1; i <= str1.length(); i++) {
                    for (int j = 1; j <= str2.length(); j++) {
                        distance[i][j] = minimum(
                                        distance[i - 1][j] + 1,
                                        distance[i][j - 1] + 1,
                                        distance[i - 1][j - 1]
                                                        + ((str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0
                                                                        : 1));
                    }
                }
 
                return distance[str1.length()][str2.length()];
        }
    }
}