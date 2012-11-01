package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.featureextraction.lexcial;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.uima.UimaContext;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.classifier.Feature;
import org.cleartk.classifier.feature.extractor.CleartkExtractorException;
import org.cleartk.classifier.feature.extractor.simple.SimpleFeatureExtractor;
import org.uimafit.factory.initializable.Initializable;

public class IdentityExtractor
    implements SimpleFeatureExtractor, Initializable
{
    
    private List<String> confusionSet;
    
    public IdentityExtractor(Set<String> confusionSet) {
        this.confusionSet = new ArrayList<String>(confusionSet);
    }
    
    @Override
    public void initialize(UimaContext context)
        throws ResourceInitializationException
    {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public List<Feature> extract(JCas jcas, Annotation focusAnnotation)
        throws CleartkExtractorException
    {   
        List<Feature> features = new ArrayList<Feature>();
        features.add(new Feature("identity", confusionSet.indexOf(focusAnnotation.getCoveredText())));
        return features;
    }
}