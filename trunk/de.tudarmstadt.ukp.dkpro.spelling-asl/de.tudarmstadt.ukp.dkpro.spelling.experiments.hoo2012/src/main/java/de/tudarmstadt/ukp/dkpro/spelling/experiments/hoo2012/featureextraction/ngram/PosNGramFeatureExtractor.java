package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.featureextraction.ngram;

import static org.uimafit.util.JCasUtil.select;
import static org.uimafit.util.JCasUtil.selectCovered;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.ngrams.util.NGramStringListIterable;

public class PosNGramFeatureExtractor
    implements SimpleFeatureExtractor, Initializable
{

    public static final String PARAM_POS_NGRAM_MIN_N = "PosNGramMinSize";
    public static final String PARAM_POS_NGRAM_MAX_N = "PosNGramMaxSize";
    public static final String PARAM_POS_NGRAM_FD_FILE = "PosNGramFDFile";
    public static final String PARAM_USE_TOP_K_POS = "TopKPOS";
    
    private int minN = 1;
    private int maxN = 3;
    private String fdFile;
    private int topK = 500;
    protected Set<String> topKSet;
        
    private FrequencyDistribution<String> trainingFD;
    
    @Override
    public List<Feature> extract(JCas jcas, Annotation focusAnnotation)
        throws CleartkExtractorException
    {
        List<Feature> features = new ArrayList<Feature>();
        
        FrequencyDistribution<String> documentNgrams = getDocumentPOSNgrams(jcas, minN, maxN);
        for (String topNgram : topKSet) {
            if (documentNgrams.getKeys().contains(topNgram)) {
                features.add(new Feature(topNgram, documentNgrams.getCount(topNgram)));
//                features.add(new Feature(topNgram, 1));
            }
            else {
                features.add(new Feature(topNgram, 0));
            }
        }
        
//        documentNgrams.clear();
 
        return features;
    }
    
    // TODO create a uimafit/cleartk class that does the config parameter magic like e.g. JCasAnnotator_ImplBase
    @Override
    public void initialize(UimaContext context)
        throws ResourceInitializationException
    {
        
        initializeParameters(context);
 
        topKSet = getTopNgrams();
    }
    
    private void initializeParameters(UimaContext context) {
        if (context.getConfigParameterValue(PARAM_POS_NGRAM_MIN_N) != null) {
            this.minN = (Integer) context.getConfigParameterValue(PARAM_POS_NGRAM_MIN_N);
        }

        if (context.getConfigParameterValue(PARAM_POS_NGRAM_MAX_N) != null) {
            this.maxN = (Integer) context.getConfigParameterValue(PARAM_POS_NGRAM_MAX_N);
        }

        if (context.getConfigParameterValue(PARAM_USE_TOP_K_POS) != null) {
            this.topK = (Integer) context.getConfigParameterValue(PARAM_USE_TOP_K_POS);
        }
        
        if (context.getConfigParameterValue(PARAM_POS_NGRAM_FD_FILE) != null) {
            this.fdFile = (String) context.getConfigParameterValue(PARAM_POS_NGRAM_FD_FILE);
        }
    }
    
    private Set<String> getTopNgrams()
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
            
            topNGrams.add(key);
            i++;
        }
        
        return topNGrams;
    }
    
    public static FrequencyDistribution<String> getDocumentPOSNgrams(JCas jcas, int minN, int maxN) {
        FrequencyDistribution<String> documentNgrams = new FrequencyDistribution<String>();
        for (Sentence s : select(jcas, Sentence.class)) {
            
            List<String> posStrings = new ArrayList<String>();
            for (POS pos : selectCovered(POS.class, s)) {
                posStrings.add(pos.getClass().getSimpleName());
//                posStrings.add(pos.getPosValue());
            }
            
            for (List<String> ngram : new NGramStringListIterable(posStrings, minN, maxN)) {
                documentNgrams.inc(StringUtils.join(ngram, "_"));
            }
        }
        return documentNgrams;
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