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

import static de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining.util.SpellingRevisionUtils.containsLinebreak;

import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.io.jwpl.WikipediaRevisionPairReader;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining.type.ChangedToken;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining.type.RevisionSentencePair;

public class ChangeVisualizer
    extends JCasAnnotator_ImplBase
{

    /** Maximum allowed sentence length. **/
    public static final String PARAM_SENTENCE_LENGTH= "MaxSentenceLength";
    @ConfigurationParameter(name = PARAM_SENTENCE_LENGTH, mandatory = true, defaultValue="200")
    private int maxSentenceLength;

    public static final String PARAM_MIN_CHANGED_WORDS = "MinChangedWords";
    @ConfigurationParameter(name = PARAM_MIN_CHANGED_WORDS, mandatory=true, defaultValue="1")
    private int minChangedWords;
    
    public static final String PARAM_MAX_CHANGED_WORDS = "MaxChangedWords";
    @ConfigurationParameter(name = PARAM_MAX_CHANGED_WORDS, mandatory=true, defaultValue="5")
    private int maxChangedWords;

    @Override
    public void process(JCas jcas)
        throws AnalysisEngineProcessException
    {

        JCas revView1;
        JCas revView2;
        try {
            revView1 = jcas.getView(WikipediaRevisionPairReader.REVISION_1);
            revView2 = jcas.getView(WikipediaRevisionPairReader.REVISION_2);
        }
        catch (CASException e) {
            throw new AnalysisEngineProcessException(e);
        }
        
        for (RevisionSentencePair pair : JCasUtil.select(revView2, RevisionSentencePair.class)) {
            Sentence s1 = (Sentence) pair.getSentence1();
            Sentence s2 = (Sentence) pair.getSentence2();
            
            // do not consider very long sentences (usually parsing errors)
            if (s1.getCoveredText().length() > maxSentenceLength) {
                continue;
            }
            
            List<ChangedToken> changedList1 = JCasUtil.selectCovered(revView1, ChangedToken.class, s1);
            List<ChangedToken> changedList2 = JCasUtil.selectCovered(revView2, ChangedToken.class, s2);
    
            // only consider changes where at most nrOfChangedWords tokens where changed
            if (changedList1.size() < minChangedWords || 
                changedList2.size() < minChangedWords ||
                changedList1.size() > maxChangedWords || 
                changedList2.size() > maxChangedWords)
            {
                continue;
            }
            
            ChangedToken changedToken1 = changedList1.iterator().next();
            ChangedToken changedToken2 = changedList2.iterator().next();
            
            String token1 = changedToken1.getCoveredText();
            String token2 = changedToken2.getCoveredText();
    
            // same token is definitely not what we want
            if (token1.toLowerCase().equals(token2.toLowerCase())) {
                continue;
            }
    
            // sentence may not contains line breaks -> this indicates wrong sentence splitting
            if (containsLinebreak(s1.getCoveredText()) || containsLinebreak(s2.getCoveredText())) {
                continue;
            }
    
            // should not start with a number
            // (we are not looking for numbers)
            if (Character.isDigit(token1.charAt(0)) || Character.isDigit(token2.charAt(0))) {
                continue;
            }
    
//            // should not be all uppercase letters
//            if (token1.toUpperCase().equals(token1) ||
//                token2.toUpperCase().equals(token2))
//            {
//                continue;
//            }

            System.out.println("--------------------------------------");
            for (ChangedToken ct1 : changedList1) {
                System.out.print(ct1.getCoveredText() + ", ");
            }
            System.out.println();
            System.out.println();
            for (ChangedToken ct2 : changedList2) {
                System.out.print(ct2.getCoveredText() + ", ");
            }
            System.out.println();
            System.out.println();
            System.out.println(s1.getCoveredText());
            System.out.println(s2.getCoveredText());
            System.out.println();
       }
    }
}
