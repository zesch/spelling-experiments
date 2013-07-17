package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.featureextraction.spelling;

import static org.uimafit.factory.AnalysisEngineFactory.createAggregateDescription;
import static org.uimafit.factory.AnalysisEngineFactory.createPrimitive;
import static org.uimafit.factory.AnalysisEngineFactory.createPrimitiveDescription;

import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.jcas.JCas;
import org.cleartk.classifier.Feature;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;

public class PosContextFeatureExtractorTest
{
    @Test
    public void posContextFeatureExtractorTest()
        throws Exception
    {
        AnalysisEngineDescription desc = createAggregateDescription(
                createPrimitiveDescription(BreakIteratorSegmenter.class),
                createPrimitiveDescription(OpenNlpPosTagger.class)
        );
        AnalysisEngine engine = createPrimitive(desc);

        JCas jcas = engine.newJCas();
        jcas.setDocumentLanguage("en");
        jcas.setDocumentText("This is a test.");
        engine.process(jcas);
        
        POS pos = new POS(jcas, 5, 7);
        pos.addToIndexes();
        
        Assert.assertEquals("is", pos.getCoveredText());

        PosContextFeatureExtractor extractor = new PosContextFeatureExtractor();
        extractor.initialize(engine.getUimaContext());
        List<Feature> features = extractor.extract(jcas, pos);
        Assert.assertEquals(5, features.size());
        
        Iterator<Feature> iter = features.iterator();
        this.assertFeature("Preceding_0_2_1", "OOB1", iter.next());
        this.assertFeature("Preceding_0_2_0_TypePath(PosValue)", "DT", iter.next());
        this.assertFeature("Following_0_2_0_TypePath(PosValue)", "DT", iter.next());
        this.assertFeature("Following_0_2_1_TypePath(PosValue)", "NN", iter.next());
        this.assertFeature("Covered_0_TypePath(PosValue)", "VBZ", iter.next());
    }
    
    private void assertFeature(String expectedName, Object expectedValue, Feature actualFeature) {
        Assert.assertNotNull(actualFeature);
        Assert.assertEquals(expectedName, actualFeature.getName());
        Assert.assertEquals(expectedValue, actualFeature.getValue());
    }
}
