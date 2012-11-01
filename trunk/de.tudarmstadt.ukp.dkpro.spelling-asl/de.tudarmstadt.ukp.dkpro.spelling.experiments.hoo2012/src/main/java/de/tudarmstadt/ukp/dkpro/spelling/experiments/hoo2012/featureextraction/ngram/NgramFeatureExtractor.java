package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.featureextraction.ngram;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.classifier.Feature;
import org.cleartk.classifier.feature.extractor.CleartkExtractorException;
import org.cleartk.classifier.feature.extractor.simple.SimpleFeatureExtractor;
import org.uimafit.factory.initializable.Initializable;

import de.tudarmstadt.ukp.dkpro.core.api.frequency.util.FrequencyDistribution;
import de.tudarmstadt.ukp.dkpro.core.api.resources.ResourceUtils;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.util.NGramUtils;

public class NgramFeatureExtractor
    implements SimpleFeatureExtractor, Initializable
{

    public static final String PARAM_NGRAM_MIN_N = "NGramMinSize";
    public static final String PARAM_NGRAM_MAX_N = "NGramMaxSize";
    public static final String PARAM_NGRAM_FD_FILE = "NGramFDFile";
    public static final String PARAM_USE_TOP_K = "TopK";
    public static final String PARAM_STOPWORD_LIST = "StopwordList";
    public static final String PARAM_LOWER_CASE = "LowerCaseNGrams";
    public static final String PARAM_MIN_TOKEN_LENGTH_THRESHOLD = "MinTokenLengthThreshold";
    
    
    private int minN = 1;
    private int maxN = 3;
    private String fdFile;
    private int topK = 500;
    protected Set<String> topKSet;
    private String stopwordListLocation;
    private Set<String> stopwords;
    private boolean lowerCaseNGrams = true;
    private int minTokenLengthThreshold = 1;
    
    private String prefix;
    
    private FrequencyDistribution<String> trainingFD;
    
    @Override
    public List<Feature> extract(JCas jcas, Annotation focusAnnotation)
        throws CleartkExtractorException
    {
        List<Feature> features = new ArrayList<Feature>();
        
        FrequencyDistribution<String> documentNgrams = NGramUtils.getDocumentNgrams(jcas, lowerCaseNGrams, minN, maxN, stopwords);
        for (String topNgram : topKSet) {
            if (documentNgrams.getKeys().contains(topNgram)) {
    //            features.add(new Feature(prefix + "_" + topNgram, documentNgrams.getCount(topNgram)));
                features.add(new Feature(prefix + "_" + topNgram, 1));
            }
            else {
                features.add(new Feature(prefix + "_" + topNgram, 0));
            }
        }
        
    //    documentNgrams.clear();
    
        return features;
    }
    
    @Override
    public void initialize(UimaContext context)
        throws ResourceInitializationException
    {
        
        initializeParameters(context);
    
        stopwords = getStopwordList(stopwordListLocation);
    
        topKSet = getTopNgrams(stopwords);
        
        prefix = "ngrams_";
    
    }
    
    private void initializeParameters(UimaContext context) {
        if (context.getConfigParameterValue(PARAM_NGRAM_MIN_N) != null) {
            this.minN = (Integer) context.getConfigParameterValue(PARAM_NGRAM_MIN_N);
        }
    
        if (context.getConfigParameterValue(PARAM_NGRAM_MAX_N) != null) {
            this.maxN = (Integer) context.getConfigParameterValue(PARAM_NGRAM_MAX_N);
        }
    
        if (context.getConfigParameterValue(PARAM_USE_TOP_K) != null) {
            this.topK = (Integer) context.getConfigParameterValue(PARAM_USE_TOP_K);
        }
        
        if (context.getConfigParameterValue(PARAM_NGRAM_FD_FILE) != null) {
            this.fdFile = (String) context.getConfigParameterValue(PARAM_NGRAM_FD_FILE);
        }
    
        if (context.getConfigParameterValue(PARAM_LOWER_CASE) != null) {
            this.lowerCaseNGrams = (Boolean) context.getConfigParameterValue(PARAM_LOWER_CASE);
        }
    
        if (context.getConfigParameterValue(PARAM_STOPWORD_LIST) != null) {
            this.stopwordListLocation = (String) context.getConfigParameterValue(PARAM_STOPWORD_LIST);
        }
    
        if (context.getConfigParameterValue(PARAM_MIN_TOKEN_LENGTH_THRESHOLD) != null) {
            this.minTokenLengthThreshold = (Integer) context.getConfigParameterValue(PARAM_MIN_TOKEN_LENGTH_THRESHOLD);
        }
    }
    
    private Set<String> getTopNgrams(Set<String> stopwords)
            throws ResourceInitializationException
    {
        try {
            trainingFD = new FrequencyDistribution<String>();
            trainingFD.load(new File(fdFile));
        }
        catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
        catch (ClassNotFoundException e) {
            throw new ResourceInitializationException(e);
        }
        
        
        // FIXME - maybe something better should directly go into FrequencyDistribution
        // FIXME - this is a really bad hack
        Map<String,Long> map = new HashMap<String,Long>();
        
        for (String key : trainingFD.getKeys()) {
            map.put(key, trainingFD.getCount(key));
        }
        
        Map<String,Long> sorted_map = new TreeMap<String,Long>(new ValueComparator(map));
        sorted_map.putAll(map);
        
        Set<String> topNGrams = new HashSet<String>();
        int i=0;
        for (String key : sorted_map.keySet()) {
            if (i > topK) {
                break;
            }
            
            if (key.length() >= minTokenLengthThreshold) {
                topNGrams.add(key);
                i++;
            }
        }
        
        return topNGrams;
    }
    
    private Set<String> getStopwordList(String stopwordListLocation) throws ResourceInitializationException {
        Set<String> stopwords = new TreeSet<String>();
        if (stopwordListLocation != null) {
            InputStream is = null;
            try {
                URL url = ResourceUtils.resolveLocation(stopwordListLocation, this, null);
                is = url.openStream();
                String content = IOUtils.toString(is, "UTF-8");
                for (String line : Arrays.asList(content.split("\n"))) {
                    if (line.length() > 0) {
                        stopwords.add(StringUtils.chomp(line));
                    }
                }
            }
            catch (IOException e) {
                throw new ResourceInitializationException(e);
            }
            finally{
                IOUtils.closeQuietly(is);
            }
        }
        return stopwords;
    }
    
    class ValueComparator
        implements Comparator<String>
    {
    
        Map<String,Long> base;
    
        public ValueComparator(Map<String,Long> base)
        {
            this.base = base;
        }
    
        @Override
        public int compare(String a, String b)
        {
    
            if (base.get(a) < base.get(b)) {
                return 1;
            }
            else {
                return -1;
            }
        }
    }

}