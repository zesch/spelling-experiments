package de.tudarmstadt.ukp.dkpro.spelling.detector.ngram;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.jcas.tcas.Annotation;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class NGramDetectorUtils
{
    public static int getCandidatePosition(Annotation candidate, List<Token> tokens)
    {
        int position = -1;
        
        for (int i=0; i<tokens.size(); i++) {
            if (tokens.get(i).getBegin() == candidate.getBegin() &&
                tokens.get(i).getEnd()   == candidate.getEnd())
            {
                position = i;
            }
        }

        return position;
    }

    public static List<String> getChangedWords(String edit, List<String> words, int offset) {
        List<String> changedWords = new ArrayList<String>(words);
        changedWords.set(offset, edit);
            
        return changedWords;
    }

    public static List<String> limitToContextWindow(List<String> words, int offset, int windowSize) {
        int minOffset = offset - windowSize;
        if (minOffset < 0) {
            minOffset = 0;
        }
        
        int maxOffset = offset + windowSize;
        if (maxOffset >= words.size()) {
            maxOffset = words.size()-1;
        }
        
        List<String> changedWords = words.subList(minOffset, maxOffset+1);
            
        return changedWords;
    }

    public static String getTrigram(String s1, String s2, String s3) {
        StringBuilder sb = new StringBuilder();
        sb.append(s1);
        sb.append(" ");
        sb.append(s2);
        sb.append(" ");
        sb.append(s3);
        return sb.toString();
    }    
}