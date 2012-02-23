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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.util.CasUtil;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.featurepath.FeaturePathException;
import de.tudarmstadt.ukp.dkpro.core.api.featurepath.FeaturePathInfo;
import de.tudarmstadt.ukp.dkpro.core.api.resources.ResourceUtils;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.utils.SpellingUtils;

// FIXME current spelling anomaly annotation format only allows for one error per context.

// TODO update documentation
/**
 * Adds errors by changing a word into another known word with low edit distance.
 * <ul>
 * <li> the list of known words (the vocabulary)
 * <li> how many errors are introduced. (Wilcox-O'Hearn et al., 2008)
 *      used one error approximately every 200 words.
 * <li> which kind of words can be target words for replacement.
 *      For example, any word could be a target word (Mays, Damerau & Mercer, 1991),
 *      or only words that can be found in WordNet (Wilcox-O'Hearn, Hirst & Budanitsky, 2008).
 * </ul>
 * 
 * @author zesch
 *
 */
public class SpellingErrorAdder
    extends JCasAnnotator_ImplBase
{

    /** 
     * A file containing the list of known words (= the vocabulary).
     * As this list will be used to test the validity of generated spelling variants,
     * it should contain word forms, not lemmas.
     * 
     * Such lists can be easily created from a corpus.
     * Google N-grams are another (noisy) source.
     */
    public static final String PARAM_VOCABULARY = "VocabularyFile";
    @ConfigurationParameter(name = PARAM_VOCABULARY, mandatory=true)
    private String vocabularyFile;

    private Set<String> vocabulary;
    
    /** 
     * How many errors are allowed in one sentence.
     */
    public static final String PARAM_MAX_ERRORS_PER_SENTENCE = "MaxErrorsPerSentence";
    @ConfigurationParameter(name = PARAM_MAX_ERRORS_PER_SENTENCE, mandatory=true, defaultValue="1")
    private int maxErrorsPerSentence;

    /** 
     * How much context in form of sentences left and right of the target sentence should be added.
     * 
     * To avoid a position bias, context sentences are randomly added left or right of the sentence containing the error.
     * 
     * This parameter indirectly controls the "alpha" parameter used by (Wilcox-O'Hearn et al., 2008) to control the error rate of the typist.
     */
    public static final String PARAM_ERROR_CONTEXT_SIZE = "ErrorContextSize";
    @ConfigurationParameter(name = PARAM_ERROR_CONTEXT_SIZE, mandatory=true, defaultValue="0")
    private int errorContextSize;

    /** 
     * The minimum size of an error or an correction in characters.
     */
    public static final String PARAM_MINIMUM_SIZE = "MinimumSize";
    @ConfigurationParameter(name = PARAM_MINIMUM_SIZE, mandatory=true, defaultValue="2")
    private int minimumSize;

    /** 
     * The maximum edit distance.
     */
    public static final String PARAM_MAX_EDIT_DISTANCE = "MaxEditDistance";
    @ConfigurationParameter(name = PARAM_MAX_EDIT_DISTANCE, mandatory=true, defaultValue="1")
    private int maxEditDistance;

    /** 
     * The minimum size of a sentence (counted in tokens) to be considered for adding an error.
     */
    public static final String PARAM_MIN_SENTENCE_LENGTH = "MinSentenceLength";
    @ConfigurationParameter(name = PARAM_MIN_SENTENCE_LENGTH, mandatory=true, defaultValue="10")
    private int minSentenceLength;

    /** 
     * The maximum number of error items to create.
     * A value <= 0 means that all items in the corpus are used.
     */
    public static final String PARAM_MAX_ITEMS = "MaxItems";
    @ConfigurationParameter(name = PARAM_MAX_ITEMS, mandatory=true, defaultValue="0")
    private int maxItems;

    /**
     * The annotation type to be used as target words as a FeaturePath.
     * Might be e.g. Token if all words are considered, or Noun if only nouns should be considered.
     * 
     */
    public static final String PARAM_TARGET_ANNOTATION_TYPE = "TargetAnnotationType";
    @ConfigurationParameter(name = PARAM_TARGET_ANNOTATION_TYPE, mandatory = false)
    private String targetAnnotationTypeString;
    
    private Random randomGenerator;

    private int nrOfItemsAdded;
    
    @Override
    public void initialize(UimaContext context)
        throws ResourceInitializationException
    {
        super.initialize(context);
        
        this.vocabulary = new HashSet<String>();
        
        String content;
        try {
            InputStream is = null;
            try {
                URL url = ResourceUtils.resolveLocation(vocabularyFile, this, getContext());
                is = url.openStream();
                content = IOUtils.toString(is, "UTF-8");
                for (String item : content.split("\n")) {
                    if (!item.startsWith("#")) {
                        vocabulary.add( item );
                    }
                }
            }
            finally{
                IOUtils.closeQuietly(is);
            }
        }
        catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
        
        this.randomGenerator = new Random();
        
        this.nrOfItemsAdded = 0;
    }
    
    @Override
    public void process(JCas jcas)
        throws AnalysisEngineProcessException
    {

        if (nrOfItemsAdded >= maxItems) {
            return;
        }
        
        // initialize the FeaturePathInfo with the corresponding part
        FeaturePathInfo fp = new FeaturePathInfo();

        // separate typename and featurepath
        String[] segments = targetAnnotationTypeString.split("/", 2);

        String typeName = segments[0];
        Type t = jcas.getCas().getTypeSystem().getType(typeName);
        if (t == null) {
            throw new AnalysisEngineProcessException(new IllegalStateException("Type " +
                    typeName + " not found in type system"));
        }

        try {
            if (segments.length > 1) {
                fp.initialize(segments[1]);
            }
            else {
                fp.initialize("");
            }
        } catch (FeaturePathException e) {
            throw new AnalysisEngineProcessException(e);
        }

        
        for (Sentence s : JCasUtil.select(jcas, Sentence.class)) {

            // only consider sentences of a certain length
            List<Token> tokens = JCasUtil.selectCovered(jcas, Token.class, s);
            if (tokens.size() < minSentenceLength) {
                continue;
            }
            
            List<AnnotationFS> annotations = new ArrayList<AnnotationFS>(
                    CasUtil.selectCovered(jcas.getCas(), t, s)
            );

            addErrors(jcas, fp, annotations, maxErrorsPerSentence);
        }
    }

    private void addErrors(JCas jcas, FeaturePathInfo fp, List<AnnotationFS> annotations, int nrOfErrors) {

        // shuffle to avoid always adding error in first position
        Collections.shuffle(annotations); 

        int addedErrors = 0;
        for (AnnotationFS a : annotations) {
            String term = fp.getValue(a);
            if (addedErrors < nrOfErrors) {
                if (addCandidate(jcas, a, term)) {
                    nrOfItemsAdded++;
                    addedErrors++;
                }
            }
        }
    }
    
    private boolean addCandidate(JCas jcas, AnnotationFS a, String term) {
        // do not consider this term if it is too short
        if (term.length() < minimumSize) {
            return false;
        }
        
        // generate spelling variants
        Set<String> candidates = SpellingUtils.getEditsInVocabulary(
                term,
                maxEditDistance,
                vocabulary
        );
        
        // select one of the spelling variants in vocabulary
        String selectedCandidate = getRandomCandidate(candidates);
        
        // in case of not being able to select a candidate, try next
        if (selectedCandidate == null) {
            return false;
        }
        
        // do not use this, if the term itself was generated as a candidate
        if (selectedCandidate.equals(term)) {
            return false;
        }
        
        // do not consider this candidate if it is too short
        if (selectedCandidate.length() < minimumSize) {
            return false;
        }
        
        // FIXME a possible improvement would be to only select such variants that do not change the broad POS class to minimize grammatical errors.
        // However, this would require to re-postag the changed sentence. 

        // add a SpellingAnomaly annotation
        // actually, when used this way, the semantics of this annotation is somehow inversed
        // suggestion now contains the generated error instead of the suggested correct term
        // however, as we are only using this in a very specialized pipeline "highjacking" the annotation seem justified
        SpellingAnomaly anomaly = new SpellingAnomaly(jcas);
        anomaly.setBegin(a.getBegin());
        anomaly.setEnd(a.getEnd());
        anomaly.setSuggestions(SpellingUtils.getSuggestedActionArray(jcas, selectedCandidate));
        anomaly.addToIndexes();

        return true;
    }
    
    private String getRandomCandidate(Set<String> candidates) {
        if (candidates.size() > 0) {
            int randomPosition = randomGenerator.nextInt(candidates.size());
            List<String> candidateList = new ArrayList<String>( candidates );
            return candidateList.get(randomPosition);
        }
        else {
            return null;
        }
    }
}
