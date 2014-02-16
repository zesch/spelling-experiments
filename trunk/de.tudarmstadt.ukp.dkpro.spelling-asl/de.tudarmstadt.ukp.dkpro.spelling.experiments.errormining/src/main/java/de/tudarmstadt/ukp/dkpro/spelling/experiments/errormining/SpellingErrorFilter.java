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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.frequency.provider.FrequencyCountProvider;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.ADJ;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.NN;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.V;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.parameter.ComponentParameters;
import de.tudarmstadt.ukp.dkpro.core.api.resources.ResourceUtils;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.io.jwpl.WikipediaRevisionPairReader;
import de.tudarmstadt.ukp.dkpro.core.jazzy.SpellChecker;
import de.tudarmstadt.ukp.dkpro.lexsemresource.Entity;
import de.tudarmstadt.ukp.dkpro.lexsemresource.Entity.PoS;
import de.tudarmstadt.ukp.dkpro.lexsemresource.LexicalSemanticResource;
import de.tudarmstadt.ukp.dkpro.lexsemresource.LexicalSemanticResource.LexicalRelation;
import de.tudarmstadt.ukp.dkpro.lexsemresource.LexicalSemanticResource.SemanticRelation;
import de.tudarmstadt.ukp.dkpro.lexsemresource.core.ResourceFactory;
import de.tudarmstadt.ukp.dkpro.lexsemresource.exception.LexicalSemanticResourceException;
import de.tudarmstadt.ukp.dkpro.lexsemresource.exception.ResourceLoaderException;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.core.DatasetItem;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining.type.ChangedToken;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.errormining.type.RevisionSentencePair;
import dkpro.similarity.algorithms.lexical.string.LevenshteinSecondStringComparator;
import dkpro.similarity.algorithms.lexical.string.SecondStringComparator_ImplBase;

/**
 * Filters aligned sentences according to some heuristics.
 * 
 * @author zesch
 *
 */
public class SpellingErrorFilter
    extends JCasAnnotator_ImplBase
{
    
    /** 
    * How many context sentences should be added left and right of the target sentence.
    * 0 - only the target sentence
    * 1 - one left and one right -> 3 sentences including the target sentence
    * 2 - two left and two right -> 5 sentences including the target sentence 
    * ...
    * 
    * This parameter indirectly controls the "alpha" parameter used by (Wilcox-O'Hearn et al., 2008) to control the error rate of the typist.
    */
    private static final int NR_OF_CONTEXT_SENTENCES = 3;

    /** 
     * The language code that should be used to initialize the phrase count provider.
     */
    public static final String PARAM_LANG = "LanguageCode";
    @ConfigurationParameter(name = PARAM_LANG, mandatory=true, defaultValue="en")
    private String languageCode;

    /** 
     * The language code that should be used to initialize the phrase count provider.
     */
    public static final String PARAM_TARGET_LOCATION = ComponentParameters.PARAM_TARGET_LOCATION;
    @ConfigurationParameter(name = PARAM_TARGET_LOCATION, mandatory=true, defaultValue="en")
    private File outputPath;

    /** 
     * What should be considered a low frequency. Depends on corpus used for lookup.
     */
    public static final String PARAM_LOW_FREQ = "LowFrequency";
    @ConfigurationParameter(name = PARAM_LOW_FREQ, mandatory=true)
    private int lowFreq;
    
    /** A list of words that should not be used as target words. **/
    public static final String PARAM_BLACKLIST = "BlackList";
    @ConfigurationParameter(name = PARAM_BLACKLIST, mandatory = false)
    private String blacklistString;
    Set<String> blackList;
    
    /** Maximum allowed levenshtein distance. **/
    public static final String PARAM_LEVENSHTEIN_DISTANCE = "LevenshteinDistance";
    @ConfigurationParameter(name = PARAM_LEVENSHTEIN_DISTANCE, mandatory = true, defaultValue="3")
    private int levenshteinDistance;
    private SecondStringComparator_ImplBase levenshteinComparator;

    /** Maximum allowed sentence length. **/
    public static final String PARAM_SENTENCE_LENGTH= "MaxSentenceLength";
    @ConfigurationParameter(name = PARAM_SENTENCE_LENGTH, mandatory = true, defaultValue="200")
    private int maxSentenceLength;
    
    public static final String FREQUENCY_COUNT_RESOURCE= "FrequencyProvider";
    @ExternalResource(key = FREQUENCY_COUNT_RESOURCE)
    private FrequencyCountProvider frequencyProvider;
    
//    /** 
//     * How much context should be added.
//     * As context is always added in form of sentences, it cannot be guaranteed that the resulting context will of the specified size.
//     * It is tried to add context sentences until the context is larger than the provided value.
//     * 
//     * A context of half the given size will be added left and right of the sentence.
//     * 
//     * This parameter indirectly controls the "alpha" parameter used by (Wilcox-O'Hearn et al., 2008) to control the error rate of the typist.
//     */
//    public static final String PARAM_ERROR_CONTEXT_SIZE = "ErrorContextSize";
//    @ConfigurationParameter(name = PARAM_ERROR_CONTEXT_SIZE, mandatory=true, defaultValue="200")
//    private int errorContextSize;
//
//    /** 
//     * How many context sentences should be added left and right of the target sentence.
//     * 0 - only the target sentence
//     * 1 - one left and one right -> 3 sentences including the target sentence
//     * 2 - two left and two right -> 5 sentences including the target sentence 
//     * ...
//     * 
//     * This parameter indirectly controls the "alpha" parameter used by (Wilcox-O'Hearn et al., 2008) to control the error rate of the typist.
//     */
//    public static final String PARAM_NR_OF_CONTEXT_SENTENCES = "NrOfContextSentences";
//    @ConfigurationParameter(name = PARAM_ERROR_CONTEXT_SIZE, mandatory=true, defaultValue="1")
//    private int nrOfContextSentences;


    Random random;
    
    @SuppressWarnings("serial")
    public static final Map<String,String> dictionaryMap = new HashMap<String,String>() {{
        put("en", "classpath:/vocabulary/en_US_dict.txt");
        put("de", "classpath:/vocabulary/de_dict.txt");
    }};
    private AnalysisEngine spellChecker; 

    @SuppressWarnings("serial")
    public static final Map<String,String> lsrMap = new HashMap<String,String>() {{
        put("en", "wordnet");
        put("de", "germanet");
    }};
    private LexicalSemanticResource lsr;
    
    private final String semanticErrorOutputName = "dataset_semantic.txt";
    private final String syntacticErrorOutputName = "dataset_syntactic.txt";
    private BufferedWriter semanticErrorWriter;
    private BufferedWriter syntacticErrorWriter;
    
    private Set<String> alreadySeenErrors;
    
    private JCas revView1;
    private JCas revView2;
    
    @Override
    public void initialize(UimaContext context)
        throws ResourceInitializationException
    {
        super.initialize(context);

        try {
            levenshteinComparator = new LevenshteinSecondStringComparator();

            blackList = new HashSet<String>();
            if (blacklistString != null) {
                InputStream is = null;
                try {
                    URL url  = ResourceUtils.resolveLocation(blacklistString, this, getContext());
                    is = url.openStream();
                    String content = IOUtils.toString(is, "UTF-8");
                    blackList.addAll(Arrays.asList(content.split("\n")));
                }
                finally{
                    IOUtils.closeQuietly(is);
                }
            }
            
            if (!dictionaryMap.containsKey(languageCode)) {
                throw new IOException("Do not know which spell checker dictionary to use for language: " + languageCode);
            }
            spellChecker = AnalysisEngineFactory.createEngine(
                    SpellChecker.class,
                    SpellChecker.PARAM_MODEL_LOCATION, dictionaryMap.get(languageCode)
            );
        
            if (!lsrMap.containsKey(languageCode)) {
                throw new IOException("Do not know which lexical semantic resource to use for language: " + languageCode);
            }
            lsr = ResourceFactory.getInstance().get(lsrMap.get(languageCode), languageCode);
            
            File semanticOutfile = new File(outputPath, semanticErrorOutputName);
            File syntacticOutfile = new File(outputPath, syntacticErrorOutputName);
            outputPath.mkdirs();
            
            semanticErrorWriter = new BufferedWriter(new FileWriter(semanticOutfile));
            syntacticErrorWriter = new BufferedWriter(new FileWriter(syntacticOutfile));
            
            alreadySeenErrors = new HashSet<String>();
            
            random = new Random();
        }
        catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
        catch (ResourceLoaderException e) {
            throw new ResourceInitializationException(e);
        }
    }
    
    
    @Override
    public void process(JCas jcas)
        throws AnalysisEngineProcessException
    {
        // RevisionSentencePair annotations are added to the second view
        try {
            revView1 = jcas.getView(WikipediaRevisionPairReader.REVISION_1);
            revView2 = jcas.getView(WikipediaRevisionPairReader.REVISION_2);
        }
        catch (CASException e) {
            throw new AnalysisEngineProcessException(e);
        }
        
        DocumentMetaData dmd = DocumentMetaData.get(revView1);
        
        // heuristics in this loop should be loosely ordered by their computation time
        // "heavy" computing should come last
        for (RevisionSentencePair pair : JCasUtil.select(revView2, RevisionSentencePair.class)) {
            Sentence s1 = (Sentence) pair.getSentence1();
            Sentence s2 = (Sentence) pair.getSentence2();
            
            // do not consider very long sentences (usually parsing errors)
            if (s1.getCoveredText().length() > maxSentenceLength) {
                continue;
            }
            
            List<ChangedToken> changedList1 = JCasUtil.selectCovered(revView1, ChangedToken.class, s1);
            List<ChangedToken> changedList2 = JCasUtil.selectCovered(revView2, ChangedToken.class, s2);
    
            // only consider changes where there was a single token change
            if (changedList1.size() != 1 || changedList2.size() != 1) {
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
    
            // check that the changed token is in the same position in the sentence
            // this avoids situations where "a b c" is changed into "a c b"
            // which often also leads to low distance / high similarity
            if (changedToken1.getPosition() != changedToken2.getPosition()) {
                continue;
            }
            
            // should not start with a number
            // (we are not looking for numbers)
            if (Character.isDigit(token1.charAt(0)) || Character.isDigit(token2.charAt(0))) {
                continue;
            }
    
            // should not be all uppercase letters
            if (token1.toUpperCase().equals(token1) ||
                token2.toUpperCase().equals(token2))
            {
                continue;
            }
            
            // should not change case
            if (!haveFirstLettersSameCase(token1.charAt(0), token2.charAt(0))) {
                continue;
            }
            
            // certain words should not be target words
            if (blackList.contains(token1.toLowerCase()) || blackList.contains(token2.toLowerCase())) {
                continue;
            }
            
    //deactivated as this check also removes a lot of interesting cases             
    //        // quick check for substitutions of whole words
    //        // if letter at the beginning an letter at the end is different
    //        if (text1.charAt(0) != text2.charAt(0) &&
    //            text1.charAt(text1.length()-1) != text2.charAt(text2.length()-1))
    //        {
    //            continue;
    //        }
            
            long freq1;
            long freq2;
            double distance;
            double ratio;
    
            try {
                freq1 = frequencyProvider.getFrequency(token1);
                freq2 = frequencyProvider.getFrequency(token2);
                ratio = (double) freq1 / freq2;
                distance = levenshteinComparator.getSimilarity(token1, token2);
            }
            catch (Exception e) {
                throw new AnalysisEngineProcessException(e);
            }
            
            // zero frequencies - definitely not a word
            if (freq1 == 0 || freq2 == 0) {
                continue;
            }
            
            // low frequencies - probably not a word
            if (freq1 < lowFreq || freq2 < lowFreq) {
                continue;
            }
    
            // distance should not be greater than a threshold
            if (distance > levenshteinDistance) {
                continue;
            }
            
            // low ratio and low levenshtein indicate normal spelling mistake
            // FIXME no magic numbers
            if (ratio < 0.0001 && distance < 4) {
                continue;
            }
            
            // if both token share the same lemma - probably not an error
            List<Lemma> lemmas1 = JCasUtil.selectCovered(revView1, Lemma.class, changedToken1);
            List<Lemma> lemmas2 = JCasUtil.selectCovered(revView2, Lemma.class, changedToken2);
    
            if (lemmas1.size() == 0 || lemmas2.size() == 0) {
                throw new AnalysisEngineProcessException(new Throwable("could not get lemma for token"));
            }
    
            Lemma lemma1 = lemmas1.get(0);
            Lemma lemma2 = lemmas2.get(0);
            
            if (lemma1.getValue().equals(lemma2.getValue())) {
                System.out.println("SAME LEMMA");
                System.out.println(token1 + " - " + token2);
                System.out.println();
                continue;
            }
    
            
            List<POS> tags1 = JCasUtil.selectCovered(revView1, POS.class, changedToken1);
            List<POS> tags2 = JCasUtil.selectCovered(revView2, POS.class, changedToken2);
    
            if (tags1.size() == 0 || tags2.size() == 0) {
                throw new AnalysisEngineProcessException(new Throwable("could not get lemma for token"));
            }
    
            POS tag1 = tags1.get(0);
            POS tag2 = tags2.get(0);
            
            //#################################
            // check for POS class
            if (!hasAllowableTag(tag2)) {
                System.out.println("Unallowed Tag");
                System.out.println(token1 + " - " + token2);
                System.out.println();
                continue;
            }
            //#################################
            
            // Is currently covered by the more general allowed tags rule 
            // changes in Named Entities are most likely to be due to semantic errors
            if (isNamedEntity(tag1) && isNamedEntity(tag2)) {
                System.out.println("NAMED ENTITY");
                System.out.println(token1 + " - " + token2);
                System.out.println();
                continue;
            }
            
            // whether the probably wrong token1 would be detected by a spell checker
            boolean detectable;
            try {
                detectable = isDetectableSpellingError(
                        changedToken1,
                        changedToken2,
                        s1,
                        JCasUtil.selectCovered(revView1, Token.class, s1)
                );
            }
            catch (ResourceInitializationException e) {
                throw new AnalysisEngineProcessException(e);
            }
            catch (UIMAException e) {
                throw new AnalysisEngineProcessException(e);
            }
    
            if (detectable) {
                System.out.println("DETECTED SPELLING ERROR");
                System.out.println(token1 + " - " + token2);
                System.out.println();
                continue;
            }
    
            try {
                if (isInCloseRelation(lemma1.getValue(), lemma2.getValue())) {
                    System.out.println("CLOSE RELATION");
                    System.out.println(token1 + " - " + token2);
                    System.out.println();
                    continue;
                }
            }
            catch (LexicalSemanticResourceException e) {
                throw new AnalysisEngineProcessException(e);
            }

            System.out.println(token1 + " - " + token2);
            System.out.println(freq1 + " - " + freq2);
            System.out.println(s1.getCoveredText());
            System.out.println(s2.getCoveredText());
            System.out.println();

            String leftContext  = getContextSentences(revView1, s1, NR_OF_CONTEXT_SENTENCES, true);
            String rightContext = getContextSentences(revView1, s1, NR_OF_CONTEXT_SENTENCES, false);
            
            // write remaining errors in file using correct formatting
            DatasetItem item = new DatasetItem(
                token1,
                token2,
                leftContext.length() + changedToken1.getBegin() - s1.getBegin(),
                leftContext + s1.getCoveredText() + rightContext,
                new Integer(dmd.getCollectionId()).intValue(),
                new Integer(dmd.getDocumentId()).intValue()
            );
    
            // both token should have the same POS, otherwise it is more likely a grammatical error
            if (tag1.getClass().getName().equals(tag2.getClass().getName())) {
                if (!alreadySeenErrors.contains(item.getWrong() + "-" + item.getCorrect())) {
                    alreadySeenErrors.add(item.getWrong() + "-" + item.getCorrect());                    
                    writeItem(semanticErrorWriter, item);
                }
            }
            else {
                if (!alreadySeenErrors.contains(item.getWrong() + "-" + item.getCorrect())) {
                    alreadySeenErrors.add(item.getWrong() + "-" + item.getCorrect());                    
                    writeItem(syntacticErrorWriter, item);
                }
            }
        }
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
    
//    private List<Sentence> getContext(JCas jcas, Sentence s, int maxContextSize, boolean leftContext) {
//        List<Sentence> context = new ArrayList<Sentence>();
//
//        // FIXME for very large requested contexts - fetching only 10 sentences here might not be sufficient
//        if (leftContext) {
//            context.addAll(
//                    JCasUtil.selectPreceding(jcas, Sentence.class, s, 10)
//            );
//        }
//        else {
//            context.addAll(
//                    JCasUtil.selectFollowing(jcas, Sentence.class, s, 10)
//            );
//        }
//        
//        // delete unnecessary context
//        int contextSize = 0;
//        Iterator<Sentence> contextIter = context.iterator();
//        while (contextIter.hasNext()) {
//            Sentence contextSentence = contextIter.next();
//            if (contextSize < maxContextSize) {
//                contextSize += JCasUtil.selectCovered(jcas, Token.class, contextSentence).size();
//            }
//            else {
//                contextIter.remove();
//            }
//        }
//        
//        return context;
//    }
    
    private String getContextSentences(JCas jcas, Sentence s, int nrOfContextSentences, boolean leftContext) {
        List<Sentence> context = new ArrayList<Sentence>();

        if (leftContext) {
            context.addAll(
                    JCasUtil.selectPreceding(jcas, Sentence.class, s, nrOfContextSentences)
            );
        }
        else {
            context.addAll(
                    JCasUtil.selectFollowing(jcas, Sentence.class, s, nrOfContextSentences)
            );
        }
        
        StringBuilder sb = new StringBuilder();
        for (Sentence sentence : context) {
            sb.append(sentence.getCoveredText());
            sb.append(" ");
        }
        return sb.toString();
    }

    /**
     * @param t The probably wrong token.
     * @param s The sentence with the token.
     * @return Whether the token t in sentence s would be detected by a spelling corrector. 
     * @throws UIMAException 
     */
    private boolean isDetectableSpellingError(ChangedToken wrongToken, ChangedToken correctToken, Sentence sentence, List<Token> tokens) throws UIMAException {
        
        JCas aJCas = spellChecker.newJCas();

        TokenBuilder<Token, Sentence> tb = TokenBuilder.create(Token.class, Sentence.class);
        tb.buildTokens(aJCas, sentence.getCoveredText());
        
        // detect and annotate spelling errors
        spellChecker.process(aJCas);

        // check whether a detected spelling error has the same offsets as wrongToken
        for (SpellingAnomaly error : JCasUtil.select(aJCas, SpellingAnomaly.class)) {
            String suggestion = error.getSuggestions(0).getReplacement();
            if (suggestion != null) {
                if (error.getCoveredText().equals(wrongToken.getCoveredText()) &&
                    suggestion.equals(correctToken.getCoveredText()))
                {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private boolean isInCloseRelation(String lemma1, String lemma2) throws LexicalSemanticResourceException {
        for (Entity e1 : lsr.getEntity(lemma1)) {
            for (Entity e2 : lsr.getEntity(lemma2)) {
                if (isRelated(e1,e2)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isRelated(Entity e1, Entity e2) throws LexicalSemanticResourceException {
        Set<String> lexemes2 = e2.getLexemes();
        
        String lexeme = e1.getFirstLexeme();
        PoS pos = e1.getPos();
        String sense = e1.getSense(lexeme); 

        for (LexicalRelation relation : LexicalRelation.values()) {
            Set<String> relatedLexemes = lsr.getRelatedLexemes(lexeme, pos, sense, relation);
            relatedLexemes.retainAll(lexemes2);
            
            if (relatedLexemes.size() > 0) {
                return true;
            }
        }
        
        for (SemanticRelation relation : SemanticRelation.values()) {
            Set<Entity> relatedEntities = lsr.getRelatedEntities(e1, relation);
            
            if (relatedEntities.contains(e2)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean haveFirstLettersSameCase(char char1, char char2) {
        if (Character.isUpperCase(char1) && Character.isLowerCase(char2) ||
            Character.isLowerCase(char1) && Character.isUpperCase(char2))
        {
            return false;
        }
        
        return true;
    }

    private boolean isNamedEntity(POS pos) {
        if (pos.getClass().getName().equals("de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.NP")) {
            return true;
        }
        
        return false;
    }

    private boolean hasAllowableTag(POS pos) {
        if (pos instanceof NN ||
            pos instanceof V ||
            pos instanceof ADJ)
        {
            return true;
        }
        
        return false;
    }

    @Override
    public void collectionProcessComplete()
        throws AnalysisEngineProcessException
    {
        try {
            syntacticErrorWriter.close();
            semanticErrorWriter.close();
        }
        catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
        super.collectionProcessComplete();
    }
}
