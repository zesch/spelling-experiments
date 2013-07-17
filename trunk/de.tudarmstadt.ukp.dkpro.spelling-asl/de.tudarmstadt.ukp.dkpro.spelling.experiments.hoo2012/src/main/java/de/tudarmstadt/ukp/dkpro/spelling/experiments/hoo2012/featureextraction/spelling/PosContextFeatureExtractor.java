package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.featureextraction.spelling;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.classifier.Feature;
import org.cleartk.classifier.feature.extractor.CleartkExtractor;
import org.cleartk.classifier.feature.extractor.CleartkExtractor.Covered;
import org.cleartk.classifier.feature.extractor.CleartkExtractor.Following;
import org.cleartk.classifier.feature.extractor.CleartkExtractor.Preceding;
import org.cleartk.classifier.feature.extractor.CleartkExtractorException;
import org.cleartk.classifier.feature.extractor.simple.SimpleFeatureExtractor;
import org.cleartk.classifier.feature.extractor.simple.TypePathExtractor;
import org.uimafit.factory.initializable.Initializable;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;


public class PosContextFeatureExtractor
    implements SimpleFeatureExtractor, Initializable
{
    
    private CleartkExtractor extractor;
    
    private FeatureMap<String> featureMap;
        
    @Override
    public void initialize(UimaContext context)
        throws ResourceInitializationException
    {
        featureMap = new FeatureMap<String>();
        
        extractor = new CleartkExtractor(
                POS.class,
                new TypePathExtractor(POS.class,  "PosValue"),
                new Preceding(2),
                new Following(2),
                new Covered()
        );
    }

    @Override
    public List<Feature> extract(JCas jcas, Annotation focusAnnotation)
        throws CleartkExtractorException
    {
        // convert from nominal features to numeric    
        List<Feature> orginalFeatures = extractor.extract(jcas, focusAnnotation);
        for (Feature feature : orginalFeatures) {
            featureMap.register((String) feature.getValue());
        }
        
        List<Feature> convertedFeatures = new ArrayList<Feature>();
        for (Feature feature : orginalFeatures) {
            convertedFeatures.add(
                    new Feature(
                            feature.getName(),
                            featureMap.getId((String) feature.getValue())
                    )
            );
        }        
        
        return convertedFeatures;
    }
}