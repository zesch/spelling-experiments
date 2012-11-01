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
package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012;

import static org.uimafit.factory.AnalysisEngineFactory.createAggregateDescription;
import static org.uimafit.factory.AnalysisEngineFactory.createPrimitiveDescription;
import static org.uimafit.factory.CollectionReaderFactory.createDescription;
import static org.uimafit.factory.ExternalResourceFactory.createExternalResourceDescription;

import java.io.File;
import java.io.IOException;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceInitializationException;

import de.tudarmstadt.ukp.dkpro.core.api.resources.DKProContext;
import de.tudarmstadt.ukp.dkpro.core.frequency.resources.Web1TFrequencyCountResource;
import de.tudarmstadt.ukp.dkpro.core.io.tei.TEIReader;
import de.tudarmstadt.ukp.dkpro.core.opennlp.OpenNlpPosTagger;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.lab.Lab;
import de.tudarmstadt.ukp.dkpro.lab.task.Dimension;
import de.tudarmstadt.ukp.dkpro.lab.task.ParameterSpace;
import de.tudarmstadt.ukp.dkpro.lab.task.Task;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.BatchTask;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.BatchTask.ExecutionPolicy;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.featureextraction.ngram.NgramFeatureExtractor;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.featureextraction.ngram.PosNGramFeatureExtractor;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.io.HOO2012SourceReader;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.lab.report.HOO2012ResultsReport;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.lab.task.BaselineTask;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.lab.task.ExtractFeaturesTestTask;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.lab.task.ExtractFeaturesTrainingTask;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.lab.task.MetaInfoTask;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.lab.task.StatisticalTask;

public class HOO2012Experiments {

	private final static String TEST_DATA = "classpath:/hoo2012/train/Raw";
	private final static String GOLD_DATA =  "classpath:/hoo2012/train/Gold";

	
	private static final String LANG = "en";
	private final static String TEAM_ID = "UD";
		
	public static boolean WRITE_EDITS = true;

	private final static String MIN_NGRAM_LEVEL = "1";
	private final static String MAX_NGRAM_LEVEL = "3";
	
	private final static String NGRAM_MODEL = "en";
//	private final static String NGRAM_MODEL = "wiki_en";
		
	private final static String[] PREP_CONFUSION_SET = new String[] {
		"at",
		"by",
		"for",
		"from",
		"in",
		"of",
		"on",
		"out",
		"over",
		"to",
		"up",
		"with"
	};

	private final static String[] DET_CONFUSION_SET = new String[] {
		"a",
		"an",
		"the",
		"this"
	};
	
    public enum WekaClassifiers {
        smo,
        j48,
        naivebayes,
        randomforest
    }
    
//  public static final String[] classifiers = new String[] { WekaClassifiers.j48.name(), WekaClassifiers.naivebayes.name(), WekaClassifiers.randomforest.name(), WekaClassifiers.smo.name() };
  public static final String[] classifiers = new String[] { WekaClassifiers.smo.name() };

    public static final String OUTPUT_KEY = "outputDir";
	
	public static void main(String[] args) throws Exception {
		
		HOO2012Experiments exp = new HOO2012Experiments();
		
//		// baselines
//		exp.runBaselineExperiments(
//				ART.class.getName(),
//				"classpath:/candidates/articles.txt",
//				"the"
//		);
//		exp.runBaselineExperiments(
//				PP.class.getName(),
//				"classpath:/candidates/prepositions.txt",
//				"of"
//		);
//		
//		// old statistical approach from HOO 2011 adapted to only target articles and prepositions
//		// also adapted to limit possible candidates to a list of candidates
//		// ARTICLES only
//		exp.runStatisticalExperiments(
//				ART.class.getName(),
//				"classpath:/candidates/articles.txt"
//		);
//		// PREPOSITIONS only
//		exp.runStatisticalExperiments(
//				PP.class.getName(),
//				"classpath:/candidates/prepositions.txt"
//		);
		
		// supervised system
		exp.runSupervisedExperiments();
	}
	
	private void runBaselineExperiments(
			final String targetType,
			final String confusionSetFile,
			final String replacement
	)
		throws Exception
	{
		Task baselineTask = new BaselineTask(getReader(TEST_DATA), targetType, confusionSetFile, replacement);
	
		ParameterSpace pSpace = new ParameterSpace(
				Dimension.create("langCode",     LANG),
				Dimension.create("teamId",       TEAM_ID),
				Dimension.create("testDataPath", TEST_DATA),
				Dimension.create("goldDataPath", GOLD_DATA)
		);
	
	    BatchTask batch = new BatchTask();
	    batch.setParameterSpace(pSpace);
	    batch.addTask(baselineTask);
	    batch.addReport(HOO2012ResultsReport.class);
	
	    Lab.getInstance().run(batch);
	}

	private void runStatisticalExperiments(final String targetType, final String confusionSetFile)
		throws Exception
	{
		Task statisticalTask = new StatisticalTask(getReader(TEST_DATA), targetType, confusionSetFile, NGRAM_MODEL);
    
		ParameterSpace pSpace = new ParameterSpace(
				Dimension.create("langCode",     LANG),
				Dimension.create("teamId",       TEAM_ID),
				Dimension.create("testDataPath", TEST_DATA),
				Dimension.create("goldDataPath", GOLD_DATA),
				Dimension.create("threshold",    0.005f)
		);
    
	    BatchTask batch = new BatchTask();
	    batch.setParameterSpace(pSpace);
	    batch.addTask(statisticalTask);
	    batch.addReport(HOO2012ResultsReport.class);

	    Lab.getInstance().run(batch);
	}
	
	private void runSupervisedExperiments()
		throws Exception
	{
	    Object[] params = new Object[] {
	          NgramFeatureExtractor.PARAM_NGRAM_MIN_N, 1,
	          NgramFeatureExtractor.PARAM_NGRAM_MAX_N, 3,
	          PosNGramFeatureExtractor.PARAM_POS_NGRAM_MIN_N, 1,
	          PosNGramFeatureExtractor.PARAM_POS_NGRAM_MAX_N, 3
	    };
	    
		Task metaInfoTask = new MetaInfoTask(
		        getBrownCorpusReader("brown_tei_small", "en"),  params);
		Task trainingTaskRT = new ExtractFeaturesTrainingTask(
		        getBrownCorpusReader("brown_tei_small", "en"), "RT", PREP_CONFUSION_SET, NGRAM_MODEL);
        Task trainingTaskRD = new ExtractFeaturesTrainingTask(
                getBrownCorpusReader("brown_tei_small", "en"), "RD", DET_CONFUSION_SET, NGRAM_MODEL);
		Task testTaskRT     = new ExtractFeaturesTestTask(
		        getReader(TEST_DATA), "RT", PREP_CONFUSION_SET, NGRAM_MODEL);
        Task testTaskRD     = new ExtractFeaturesTestTask(
                getReader(TEST_DATA), "RD", DET_CONFUSION_SET, NGRAM_MODEL);
    
		ParameterSpace pSpaceDefault = new ParameterSpace(
				Dimension.create("langCode",     LANG),
				Dimension.create("teamId",       TEAM_ID),
				Dimension.create("testDataPath", TEST_DATA),
				Dimension.create("goldDataPath", GOLD_DATA),
				Dimension.create("classifier",   classifiers),
				Dimension.create("stopwordList", "classpath:/stopwords/empty.txt")
		);

	    // for making training dependent on meta
	    trainingTaskRT.addImportLatest(MetaInfoTask.META_KEY, MetaInfoTask.META_KEY, metaInfoTask.getType());
        trainingTaskRD.addImportLatest(MetaInfoTask.META_KEY, MetaInfoTask.META_KEY, metaInfoTask.getType());

	    // for making test dependent on meta and training
	    testTaskRT.addImportLatest(MetaInfoTask.META_KEY, MetaInfoTask.META_KEY, metaInfoTask.getType());
        testTaskRD.addImportLatest(MetaInfoTask.META_KEY, MetaInfoTask.META_KEY, metaInfoTask.getType());
	    testTaskRT.addImportLatest(ExtractFeaturesTrainingTask.TRAINING_KEY, ExtractFeaturesTrainingTask.TRAINING_KEY, trainingTaskRT.getType());
        testTaskRD.addImportLatest(ExtractFeaturesTrainingTask.TRAINING_KEY, ExtractFeaturesTrainingTask.TRAINING_KEY, trainingTaskRD.getType());
    
	    BatchTask batch = new BatchTask();
	    batch.setParameterSpace(pSpaceDefault);
	    batch.setExecutionPolicy(ExecutionPolicy.USE_EXISTING);
	    batch.addTask(metaInfoTask);
	    batch.addTask(trainingTaskRT);
	    batch.addTask(trainingTaskRD);
        batch.addTask(testTaskRT);
        batch.addTask(testTaskRD);

	    Lab.getInstance().run(batch);
	}

	private CollectionReaderDescription getReader(String dataset)
		throws ResourceInitializationException
	{
		return createDescription(
				HOO2012SourceReader.class,
				HOO2012SourceReader.PARAM_PATH, dataset,
				HOO2012SourceReader.PARAM_PATTERNS, new String[] {
						HOO2012SourceReader.INCLUDE_PREFIX + "*.xml"
					}
		);
	}
	
	public static ExternalResourceDescription getNGramProvider(String ngramModel) throws IOException {
		File context = DKProContext.getContext().getWorkspace("web1t");
		System.out.println(new File(context, ngramModel).getAbsolutePath());
		
		return createExternalResourceDescription(
			Web1TFrequencyCountResource.class,
			Web1TFrequencyCountResource.PARAM_MIN_NGRAM_LEVEL, MIN_NGRAM_LEVEL,
			Web1TFrequencyCountResource.PARAM_MAX_NGRAM_LEVEL, MAX_NGRAM_LEVEL,
			Web1TFrequencyCountResource.PARAM_INDEX_PATH, new File(context, ngramModel).getAbsolutePath());
	}

	private CollectionReaderDescription getBrownCorpusReader(String corpusLocation, String languageCode) throws ResourceInitializationException, IOException  {
		File brownPath = DKProContext.getContext().getWorkspace(corpusLocation);
		
		return createDescription(
				TEIReader.class,
				TEIReader.PARAM_LANGUAGE, languageCode,
				TEIReader.PARAM_PATH, brownPath.getAbsolutePath(),
				TEIReader.PARAM_PATTERNS, new String[] {
				TEIReader.INCLUDE_PREFIX + "**/*.xml" }
		);
	}
	
    public static AnalysisEngineDescription getPreprocessing(String languageCode)
            throws ResourceInitializationException
        {
        
        return createAggregateDescription(
            createPrimitiveDescription(BreakIteratorSegmenter.class),
            createPrimitiveDescription(
                    OpenNlpPosTagger.class,
                    OpenNlpPosTagger.PARAM_LANGUAGE, languageCode
            )
        );
    }
}