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
package de.tudarmstadt.ukp.dkpro.semantics.spelling.utils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction;

public class SpellingUtils
{
    /**
     * Get a list for all possible variants of the given word containing an insertion, deletion,
     * replacement or transposition.
     *
     * @param word
     *            the word.
     * @return the set of variants.
     */
    // FIXME does not properly handle many languages. How to obtain a list of valid characters in a language? accented characters, umlauts, etc.
    // TODO this could be solved using the vocabulary that we already need to have -> valid characters need to appear in the vocabulary with a certain minimum probability
    public static Set<String> getEdits(String word)
    {
        Set<String> candidates = new HashSet<String>();

        for (int i = 0; i < word.length(); i++) {
            // deletes
            candidates.add(word.substring(0, i) + word.substring(i + 1));

            for (char c = 'a'; c <= 'z'; c++) {
                // replaces
                candidates.add(word.substring(0, i) + c + word.substring(i + 1));
                // inserts
                candidates.add(word.substring(0, i) + c + word.substring(i));
            }
        }

        // inserts at the end
        for (char c = 'a'; c <= 'z'; c++) {
            candidates.add(word + c);
        }

        // transposes
        for (int i = 0; i < word.length() - 1; i++) {
            candidates.add(word.substring(0, i) + word.substring(i + 1, i + 2)
                    + word.substring(i, i + 1) + word.substring(i + 2));
        }

        // remove empty candidate
        Iterator<String> candidateIter = candidates.iterator();
        while (candidateIter.hasNext()) {
            String candidate = candidateIter.next();
            if (candidate.length() == 0) {
                candidateIter.remove();
            }
            else if (Character.isWhitespace(candidate.charAt(0))) {
                candidateIter.remove();
            }
        }
        
        return candidates;
    }
    
    public static Set<String> getEditsInVocabulary(String term, int maxEditDistance, Set<String> vocabulary) {

        // generate spelling variants
        Set<String> variants = SpellingUtils.getEdits(term);
        variants.remove(term);
        
        // test whether spelling variants are in vocabulary
        Iterator<String> variantsIter = variants.iterator();
        while (variantsIter.hasNext()) {
            String candidate = variantsIter.next();
            if (!vocabulary.contains(candidate)) {
                variantsIter.remove();
            }
            // do not change case of first letter
            else if (!isCaseMatch(term, candidate)) {
                variantsIter.remove();
            }
        }

        if (maxEditDistance > 1) {
            Set<String> additionalVariants = new HashSet<String>();
            for (String variant : variants) {
                additionalVariants.addAll(getEditsInVocabulary(variant, maxEditDistance-1, vocabulary));
            }
            variants.addAll(additionalVariants);
        }
        
        return variants;
    }
    
    /**
     * @param s1
     * @param s2
     * @return True, if the first letter of both strings is either uppercase or lowercase.
     */
    public static boolean isCaseMatch(String s1, String s2) {
        if (s1.length() == 0 || s2.length() == 0) {
            return true;
        }
        
        s1 = s1.substring(0,1);
        s2 = s2.substring(0,1);
        
        if ((s1.toLowerCase().equals(s1) && s2.toLowerCase().equals(s2)) ||
            (s1.toUpperCase().equals(s1) && s2.toUpperCase().equals(s2)))
        {
            return true;
        }
        
        return false;
    }
    
    public static FSArray getSuggestedActionArray(JCas jcas, String replacement) {
        SuggestedAction action = new SuggestedAction(jcas);
        action.setReplacement(replacement);
        action.setCertainty(1.0f);
        
        FSArray array = new FSArray(jcas, 1);
        array.set(0, action);

        return array;
    }


}
