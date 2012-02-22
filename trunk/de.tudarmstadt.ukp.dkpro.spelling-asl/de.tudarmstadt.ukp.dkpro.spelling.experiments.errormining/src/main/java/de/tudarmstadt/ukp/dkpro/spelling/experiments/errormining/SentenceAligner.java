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

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.io.jwpl.WikipediaRevisionPairReader;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining.type.ChangedToken;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining.type.RevisionSentencePair;
import de.tudarmstadt.ukp.relatedness.api.RelatednessException;
import de.tudarmstadt.ukp.relatedness.api.TermRelatednessMeasure;
import de.tudarmstadt.ukp.relatedness.secondstring.JaroSecondStringComparator;

/**
 * Aligns sentences from two revisions if their distance
 * according to a user provided similarity method
 * is lower than a user defined threshold.
 * 
 * @author zesch
 *
 */
public class SentenceAligner
    extends JCasAnnotator_ImplBase
{

    /** 
     * The threshold for two sentences to be considered a sentence pair.
     * Interpretation depends on the specific measure as declared by PARAM_MEASURE. 
     */
    public static final String PARAM_THRESHOLD = "Threshold";
    @ConfigurationParameter(name = PARAM_THRESHOLD, mandatory=true)
    private float threshold;

    /** 
     * The number of characters that two sentences might differ to be still considered a pair.
     */
    public static final String PARAM_MAX_DIFF = "MaxDiff";
    @ConfigurationParameter(name = PARAM_MAX_DIFF, mandatory=false, defaultValue="5")
    private int maxDiff;

    /** 
     * The minimum length of a token (in characters) to be considered as a changed token.
     * At least one in a pair must be of that length.
     */
    public static final String PARAM_MIN_LENGTH = "MinLength";
    @ConfigurationParameter(name = PARAM_MIN_LENGTH, mandatory=false, defaultValue="3")
    private int minLength;

    
// disabled possibility to change measure for now. All simple string based measures worked almost equally well.    
//    public final static String SR_RESOURCE = "SemanticRelatednessResource";
//    @ExternalResource(key = SR_RESOURCE)
    protected TermRelatednessMeasure measure;
    
    @Override
    public void initialize(UimaContext context)
        throws ResourceInitializationException
    {
        super.initialize(context);
        
        measure = new JaroSecondStringComparator();
    }
    
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

        List<Sentence> sentenceList1 = new ArrayList<Sentence>(JCasUtil.select(revView1, Sentence.class));
        List<Sentence> sentenceList2 = new ArrayList<Sentence>(JCasUtil.select(revView2, Sentence.class));
        
        int maxSize = Math.max(sentenceList1.size(), sentenceList2.size());
        
        int segmentSize = 10;
        int iterations = (maxSize / segmentSize) + 1;
        
        for (int i=0; i<iterations; i++) {
            List<Sentence> sublist1 = getSubList(sentenceList1, i, segmentSize);
            List<Sentence> sublist2 = getSubList(sentenceList2, i, segmentSize);
            
            String subString1 = list2String(sublist1);
            String subString2 = list2String(sublist2);
            
            if (!subString1.equals(subString2)) {
                // iterate over all sentence pairs
                // get rid of equal pairs
                // only add pairs that have a relatedness which suits the threshold
                for (Sentence s1 : sublist1) {
                    for (Sentence s2 : sublist2) {
                        String text1 = s1.getCoveredText();
                        String text2 = s2.getCoveredText();
                        
                        // shortcut - no change in equal texts :)
                        if (text1.equals(text2)) {
                            continue;
                        }
                        
                        // shortcut - do not allow very large changes
                        if (Math.abs(text1.length() - text2.length()) > maxDiff) {
                            continue;
                        }
                            
                        try {
                            double relatedness = measure.getRelatedness(s1.getCoveredText(), s2.getCoveredText());

                            if (isSmallEdit(relatedness)) {
                                if (addChangedTokens(revView1, revView2, s1, s2)) {
                                    addPairAnnotation(revView2, s1, s2);
                                }
                            }
                        }
                        catch (RelatednessException e) {
                            throw new AnalysisEngineProcessException(e);
                        }
                    }
                }
            }
        }
        
    }
    
    private List<Sentence> getSubList(List<Sentence> list, int iteration, int segmentSize) {
        if (iteration*segmentSize > list.size()) {
            return new ArrayList<Sentence>();
        }
        else if ((iteration+1)*segmentSize < list.size()) {
            return list.subList(iteration*segmentSize, (iteration+1)*segmentSize);
        }
        else {
            return list.subList(iteration*segmentSize, list.size());
        }
    }
    
    private String list2String(List<Sentence> list) {
        StringBuilder sb = new StringBuilder();
        for (String item : JCasUtil.toText(list)) {
            sb.append(item);
        }
        return sb.toString();
    }
    
    private boolean isSmallEdit(double score) {
        if (measure.isDistanceMeasure()) {
            return score < threshold;
        }
        else {
            return score > threshold;
        }
    }
    
    private void addPairAnnotation(JCas jcas, Sentence s1, Sentence s2) {
        RevisionSentencePair annotation = new RevisionSentencePair(jcas);
        annotation.setSentence1(s1);
        annotation.setSentence2(s2);
        annotation.addToIndexes();
    }
    
    private boolean addChangedTokens(JCas view1, JCas view2, Sentence s1, Sentence s2) {
        List<Token> tokenList1 = JCasUtil.selectCovered(view1, Token.class, s1);
        List<Token> tokenList2 = JCasUtil.selectCovered(view2, Token.class, s2);
     
        if (tokenList1.size() != tokenList2.size()) {
            return false;
        }
        
        boolean foundChanges = false;
        for (int i=0; i<tokenList1.size(); i++) {
            Token t1 = tokenList1.get(i);
            Token t2 = tokenList2.get(i);
        
            String text1 = t1.getCoveredText();
            String text2 = t2.getCoveredText();
            
            // shortcut - only allow changes in tokens of a certain length
            if (text1.length() < minLength && text2.length() < minLength) {
                continue;
            }
            
            if (!text1.equals(text2)) {
                ChangedToken ct1 = new ChangedToken(view1, t1.getBegin(), t1.getEnd());
                ct1.setPosition(i);
                ct1.addToIndexes();
                
                ChangedToken ct2 = new ChangedToken(view2, t2.getBegin(), t2.getEnd());
                ct2.setPosition(i);
                ct2.addToIndexes();

                foundChanges = true;
            }
        }
        
        return foundChanges;       
    }
}
