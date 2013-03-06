package de.tudarmstadt.ukp.dkpro.spelling.detector.ngram;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

public class NGramDetectorUtilsTest
{

    @Test
    public void getChangedWordsTest() {
        List<String> changedWords = NGramDetectorUtils.getChangedWords("an", getExample(), 2);
        assertEquals(StringUtils.join(changedWords, " "), "This is an example .");
    }

    @Test
    public void limitToContextWindowTest() {
        List<String> changedWords = NGramDetectorUtils.limitToContextWindow(getExample(), 2, 2);
        assertEquals(StringUtils.join(changedWords, " "), "This is an example .");

        List<String> changedWords2 = NGramDetectorUtils.limitToContextWindow(getExample(), 0, 2);
        assertEquals(StringUtils.join(changedWords2, " "), "This is an");

        List<String> changedWords3 = NGramDetectorUtils.limitToContextWindow(getExample(), 4, 2);
        assertEquals(StringUtils.join(changedWords3, " "), "an example .");
    }

    private List<String> getExample() {
        List<String> words = new ArrayList<String>();
        words.add("This");
        words.add("is");
        words.add("an");
        words.add("example");
        words.add(".");
        
        return words;
    }
}
