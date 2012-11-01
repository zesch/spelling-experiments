package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.util;

import static org.uimafit.util.JCasUtil.select;
import static org.uimafit.util.JCasUtil.selectCovered;
import static org.uimafit.util.JCasUtil.toText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.uima.jcas.JCas;

import de.tudarmstadt.ukp.dkpro.core.api.frequency.util.FrequencyDistribution;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.ngrams.util.NGramStringListIterable;

public class NGramUtils
{

    public static FrequencyDistribution<String> getDocumentNgrams(JCas jcas, boolean lowerCaseNGrams, int minN, int maxN) {
        Set<String> empty = Collections.emptySet();
        return getDocumentNgrams(jcas, lowerCaseNGrams, minN, maxN, empty);
    }
    
    public static FrequencyDistribution<String> getDocumentNgrams(JCas jcas, boolean lowerCaseNGrams, int minN, int maxN, Set<String> stopwords) {
        FrequencyDistribution<String> documentNgrams = new FrequencyDistribution<String>();
        for (Sentence s : select(jcas, Sentence.class)) {
            // TODO parameterize type
            for (List<String> ngram : new NGramStringListIterable(toText(selectCovered(Token.class, s)), minN, maxN)) {

                ngram = filterNgram(ngram, lowerCaseNGrams, stopwords);
                
                // filter might have reduced size to zero => don't add in this case
                if (ngram.size() > 0) {
                    documentNgrams.inc(StringUtils.join(ngram, "_"));
                }
            }
        }
        return documentNgrams;
    }
    
    public static List<String> filterNgram(List<String> ngram, boolean lowerCase, Set<String> stopwords) {
        List<String> filteredNgram = new ArrayList<String>();
        for (String token : ngram) {
            if (lowerCase) {
                token = token.toLowerCase();
            }
            if (!stopwords.contains(token)) {
                filteredNgram.add(token.toLowerCase());
            }
        }
        return filteredNgram;
    }
}
