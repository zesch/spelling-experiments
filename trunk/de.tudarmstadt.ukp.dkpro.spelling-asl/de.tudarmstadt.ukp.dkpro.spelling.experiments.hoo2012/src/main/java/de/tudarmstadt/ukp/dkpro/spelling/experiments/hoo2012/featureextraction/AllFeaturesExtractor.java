/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.featureextraction;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.classifier.CleartkAnnotator;
import org.cleartk.classifier.Instance;
import org.cleartk.classifier.feature.extractor.simple.SimpleFeatureExtractor;
import org.cleartk.classifier.weka.util.CleartkInstanceConverter;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.factory.initializable.Initializable;
import org.uimafit.util.JCasUtil;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.functions.SMO;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;
import weka.core.Utils;
import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.utils.SpellingUtils;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.featureextraction.ngram.NgramFeatureExtractor;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.featureextraction.spelling.FirstCharFeatureExtractor;

public class AllFeaturesExtractor
    extends CleartkAnnotator<String>
{
	
    public static final String PARAM_CONFUSION_SET = "ConfusionSet";
    @ConfigurationParameter(name=PARAM_CONFUSION_SET, mandatory=true)
    private String[] confusionSetArray;
    
	public static final String PARAM_ERROR_CLASS = "CategoryClass";
	@ConfigurationParameter(name = PARAM_ERROR_CLASS, mandatory = true)
	private String errorClass;

    public static final String PARAM_TRAINING_ARFF = "TrainingArff";
    @ConfigurationParameter(name = PARAM_TRAINING_ARFF, mandatory = false)
    private File trainingArff;

    public static final String PARAM_CLASSIFIER = "Classifier";
    @ConfigurationParameter(name = PARAM_CLASSIFIER, mandatory = false)
    private String classifier;

    // if we use the isTraining parameter from the CleartkAnnotator, we also need to set a valid classifer
	// thus, we introduce another parameter here
    public static final String PARAM_IS_TEST= "IsTest";
    @ConfigurationParameter(name = PARAM_IS_TEST, mandatory = true, defaultValue="false")
    private boolean isTest;

    private Set<String> confusionSet;
	
    protected List<SimpleFeatureExtractor> featureExtractors;
    
	@Override
    public void initialize(UimaContext context)
        throws ResourceInitializationException
    {
        super.initialize(context);
         
        confusionSet = new HashSet<String>();
        confusionSet.addAll(Arrays.asList(confusionSetArray));
        
        featureExtractors = new ArrayList<SimpleFeatureExtractor>();
        featureExtractors.add(new NgramFeatureExtractor());
        featureExtractors.add(new FirstCharFeatureExtractor());
//        featureExtractors.add(new PosContextFeatureExtractor());
//        featureExtractors.add(new IdentityExtractor(confusionSet));

        for (SimpleFeatureExtractor featExt : featureExtractors) {
            if (featExt instanceof Initializable) {
                ((Initializable) featExt).initialize(context);
            }
        }
    }

    @Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		Collection<Token> tokens = JCasUtil.select(jcas, Token.class);

        if (isTest) {

            Instances trainData = null;
            Classifier cl = null;
            try {
                trainData = getInstances(trainingArff);
                
                cl = getClassifier();

//                SpreadSubsample spread = new SpreadSubsample();
//                spread.setDistributionSpread(1.0);
//                    
//                FilteredClassifier fc = new FilteredClassifier();
//                fc.setFilter(spread);
//                fc.setClassifier(cl);
                    
                cl.buildClassifier(trainData);
            }
            catch (Exception e) {
                throw new AnalysisEngineProcessException(e);
            }
            
            for (Token token : tokens) {
                String tokenString = token.getCoveredText();
                if (tokenString.length() > 0 && confusionSet.contains(tokenString)) {
                    Instance<String> instance = new Instance<String>();
                    for (SimpleFeatureExtractor featExt : featureExtractors) {
                        instance.addAll(featExt.extract(jcas, token));
                    }
                    
                    instance.setOutcome(tokenString);
                    
                    List<String> classValues = new ArrayList<String>();
                    for (Enumeration e = trainData.classAttribute().enumerateValues(); e.hasMoreElements() ;) {
                        classValues.add(e.nextElement().toString());
                    }

                    // build classifier from training arff and classify
                    try {
                        weka.core.Instance wekaInstance = CleartkInstanceConverter.toWekaInstance(instance, classValues);
                        System.out.println(wekaInstance);
                        double prediction = cl.classifyInstance(wekaInstance);

                        // prediction is the index in the class labels, not the class label itself!
                        String outcome = trainData.classAttribute().value(new Double(prediction).intValue());
                        
                        if (!tokenString.equals(outcome)) {
                            SpellingAnomaly ann = new SpellingAnomaly(jcas, token.getBegin(), token.getEnd());
                            ann.setCategory(errorClass);
                            ann.setSuggestions(SpellingUtils.getSuggestedActionArray(jcas, outcome));
                            ann.addToIndexes();
                        }
                    }
                    catch (Exception e) {
                        throw new AnalysisEngineProcessException(e);
                    }
                }
            }
        }
        else {
            for (Token token : tokens) {
                String tokenString = token.getCoveredText();
                if (tokenString.length() > 0 && confusionSet.contains(tokenString)) {
                    Instance<String> instance = new Instance<String>();
                    for (SimpleFeatureExtractor featExt : featureExtractors) {
                        instance.addAll(featExt.extract(jcas, token));
                    }
                    
                    instance.setOutcome(tokenString);
                    
                    // we also need to add a negative example
                    // choose it randomly from the confusion set without the actual token
                    // TODO implement negative examples
                        

                    this.dataWriter.write(instance);
                }
            }
        }
	}

    private Instances getInstances(File instancesFile)
        throws FileNotFoundException, IOException
    {
        Instances trainData = null;
        Reader reader;
        if (instancesFile.getAbsolutePath().endsWith(".gz")) {
            reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(instancesFile))));
        }
        else {
            reader = new BufferedReader(new FileReader(instancesFile));
        }

        try {
            trainData = new Instances(reader);
            trainData.setClassIndex(trainData.numAttributes() - 1);
        }
        finally {
            reader.close();
        }

        return trainData;
    }
    
    private Classifier getClassifier()
            throws Exception
    {
        Classifier cl = null;
        // Build and evaluate classifier
        // The options given correspond to the default settings in the WEKA GUI
        if (classifier.equals("smo"))
        {
            SMO smo = new SMO();
            smo.setOptions(Utils.splitOptions("-C 1.0 -L 0.001 -P 1.0E-12 -N 0 -V -1 -W 1 -K \"weka.classifiers.functions.supportVector.PolyKernel -C 250007 -E 1.0\""));
            cl = smo;
        } else if (classifier.equals("j48"))
        {
            J48 j48 = new J48();
            j48.setOptions(new String[] {"-C", "0.25", "-M", "2"});
            cl = j48;
        } else if (classifier.equals("naivebayes"))
        {
            cl = new NaiveBayes();
        } else if (classifier.equals("randomforest"))
        {
            RandomForest rf = new RandomForest();
            rf.setOptions(Utils.splitOptions("-I 10 -K 0 -S 1"));
            cl = rf;
        }
        return cl;
    }
}