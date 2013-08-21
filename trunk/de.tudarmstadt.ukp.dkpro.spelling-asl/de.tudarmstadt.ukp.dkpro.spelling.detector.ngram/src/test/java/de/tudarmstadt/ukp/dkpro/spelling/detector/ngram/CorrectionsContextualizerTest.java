package de.tudarmstadt.ukp.dkpro.spelling.detector.ngram;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.fit.factory.ExternalResourceFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ExternalResourceDescription;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction;
import de.tudarmstadt.ukp.dkpro.core.api.frequency.TestFrequencyCountResource;
import de.tudarmstadt.ukp.dkpro.core.api.resources.DkproContext;
import de.tudarmstadt.ukp.dkpro.core.frequency.resources.Web1TFrequencyCountResource;
import de.tudarmstadt.ukp.dkpro.core.jazzy.SpellChecker;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;

public class CorrectionsContextualizerTest
{

    @Test
    public void jazzyContextualizerTest() 
        throws Exception
    {
        String testDocument = "The cat sta on the mat . Some errosr occur in user "
                + "discourse morre often . What do you tink ?";

    
        String context = DkproContext.getContext().getWorkspace("web1t").getAbsolutePath();
        String workspace = "en";
        ExternalResourceDescription web1tResource = ExternalResourceFactory.createExternalResourceDescription(
                Web1TFrequencyCountResource.class,
                Web1TFrequencyCountResource.PARAM_MIN_NGRAM_LEVEL, "1",
                Web1TFrequencyCountResource.PARAM_MAX_NGRAM_LEVEL, "3",
                Web1TFrequencyCountResource.PARAM_INDEX_PATH, new File(context, workspace).getAbsolutePath()
        );

        
        AnalysisEngineDescription desc = createEngineDescription(
                createEngineDescription(
                        BreakIteratorSegmenter.class
                )
                ,
                createEngineDescription(
                        SpellChecker.class,
                        SpellChecker.PARAM_MODEL_LOCATION, "src/test/resources/dict/testdict.txt"
//                        SpellChecker.PARAM_DICT_PATH, "classpath:/vocabulary/en_US_dict.txt"
                )
                ,
                createEngineDescription(
                        CorrectionsContextualizer.class,
                        CorrectionsContextualizer.FREQUENCY_PROVIDER_RESOURCE,
                            ExternalResourceFactory.createExternalResourceDescription(TestFrequencyCountResource.class)
//                            web1tResource
                )
        );
        
        AnalysisEngine engine = createEngine(desc); 
        JCas jcas = engine.newJCas();
        jcas.setDocumentLanguage("en");
        jcas.setDocumentText(testDocument);
        engine.process(jcas);

        int j = 0;
        for (SpellingAnomaly anomaly : JCasUtil.select(jcas, SpellingAnomaly.class)) {
            System.out.println(anomaly.getCoveredText());
            FSArray suggestedAction = anomaly.getSuggestions();
            for (int i=0; i<suggestedAction.size(); i++) {
                SuggestedAction action = (SuggestedAction) suggestedAction.get(i);
                System.out.println("  " + action.getReplacement() + " - " + action.getCertainty());
            }
            j++;
        }
        
        assertEquals(4, j);
        System.out.println("Found " + j + " errors");
    }
    
}