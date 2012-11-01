package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.featureextraction.spelling;

import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.classifier.Feature;
import org.cleartk.classifier.feature.extractor.CleartkExtractorException;
import org.cleartk.classifier.feature.extractor.simple.SimpleFeatureExtractor;
import org.uimafit.factory.initializable.Initializable;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;

public class FirstCharFeatureExtractor
    implements SimpleFeatureExtractor, Initializable
{

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
        String lowerT = "";
        try {
            Token t = JCasUtil.selectSingleRelative(jcas, Token.class, focusAnnotation, 1);
            lowerT = t.getCoveredText().toLowerCase();
        }
        catch (IndexOutOfBoundsException e) {
            // catch exception and use empty string for matching below
        }
        
        List<Feature> features = new ArrayList<Feature>();
        features.add(new Feature("vowel_+1",     (lowerT.matches("^[aeiou].*")) ? 1 : 0));
        features.add(new Feature("consonant_+1", (lowerT.matches("^[bcdfghjklmnpqrstvwxyz].*$")) ? 1 : 0));
        features.add(new Feature("alphanum_+1",  (lowerT.matches("^[^a-z].*")) ? 1 : 0));
        return features;
    }
}