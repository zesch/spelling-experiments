package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.meta;


import java.io.File;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.descriptor.ConfigurationParameter;

import de.tudarmstadt.ukp.dkpro.core.api.frequency.util.FrequencyDistribution;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.featureextraction.ngram.PosNGramFeatureExtractor;

public class PosNGramMetaCollector
    extends FreqDistBasedMetaCollector
{
    public static final String PARAM_POS_NGRAM_FD_FILE = "PosNGramFdFile";
    @ConfigurationParameter(name = PARAM_POS_NGRAM_FD_FILE, mandatory = true)
    private File ngramFdFile;
    
    public static final String NGRAM_FD_KEY = "posNgrams.ser";
    
    @ConfigurationParameter(name = PosNGramFeatureExtractor.PARAM_POS_NGRAM_MIN_N, mandatory = true)
    private int minN;
    
    @ConfigurationParameter(name = PosNGramFeatureExtractor.PARAM_POS_NGRAM_MAX_N, mandatory = true)
    private int maxN;

    @Override
    public void initialize(UimaContext context)
        throws ResourceInitializationException
    {
        super.initialize(context);
        fdFile = ngramFdFile;
    }
    
    @Override
    public void process(JCas jcas)
        throws AnalysisEngineProcessException
    {
        FrequencyDistribution<String> documentNGrams = PosNGramFeatureExtractor.getDocumentPOSNgrams(jcas, minN, maxN); 
        for (String ngram : documentNGrams.getKeys()) {
            fd.addSample(ngram, documentNGrams.getCount(ngram));
        }
    }
}