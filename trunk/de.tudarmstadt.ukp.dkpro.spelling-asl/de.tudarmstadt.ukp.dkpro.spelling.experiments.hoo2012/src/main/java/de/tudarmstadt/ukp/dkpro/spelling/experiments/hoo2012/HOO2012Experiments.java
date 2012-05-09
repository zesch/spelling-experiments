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

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.ART;
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.PP;
import de.tudarmstadt.ukp.dkpro.core.api.resources.DKProContext;
import de.tudarmstadt.ukp.dkpro.core.frequency.resources.Web1TFrequencyCountResource;
import de.tudarmstadt.ukp.dkpro.core.io.tei.TEIReader;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.core.treetagger.TreeTaggerChunkerTT4J;
import de.tudarmstadt.ukp.dkpro.core.treetagger.TreeTaggerPosLemmaTT4J;
import de.tudarmstadt.ukp.dkpro.lab.Lab;
import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContext;
import de.tudarmstadt.ukp.dkpro.lab.storage.StorageService.AccessMode;
import de.tudarmstadt.ukp.dkpro.lab.task.Dimension;
import de.tudarmstadt.ukp.dkpro.lab.task.Discriminator;
import de.tudarmstadt.ukp.dkpro.lab.task.ParameterSpace;
import de.tudarmstadt.ukp.dkpro.lab.task.Task;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.BatchTask;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.BatchTask.ExecutionPolicy;
import de.tudarmstadt.ukp.dkpro.lab.uima.task.impl.UimaTaskBase;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.baseline.BaselineCorrectionAnnotator;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.classification.CorrectionAnnotator;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.classification.MetaCollector;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.classification.NaiveBayesClassifierFactory;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.classification.SMOClassifierFactory;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.classification.SMOClassifierWithoutRegressionFactory;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.hoo2011.FixedCandidateAnnotator;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.hoo2011.FixedCandidateTrigramProbabilityDetector;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.io.HOO2012Evaluator;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.io.HOO2012ResultsReport;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.io.HOO2012SourceReader;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.DefaultWekaDataWriterFactory;

public class HOO2012Experiments {

	private final static String INPUT_DATA = "classpath:/hoo2012/train/Raw";
	private final static String GOLD_DATA =  "classpath:/hoo2012/train/Gold";

	
	private static final String LANG = "en";
	private final static String TEAM_ID = "UD";
		
	protected static boolean WRITE_EDITS = true;

	private final static String MIN_NGRAM_LEVEL = "1";
	private final static String MAX_NGRAM_LEVEL = "3";
	
	private final static String NGRAM_MODEL = "en";
//	private final static String NGRAM_MODEL = "wiki_en";
		
	private final static String[] PREP_CONFUSION_SET = new String[] {
		"as",
		"at",
		"but",
		"by",
		"for",
		"from",
		"in",
		"of",
		"on",
		"out",
		"over",
		"since",
		"than",
		"to",
		"up",
		"with"
	};

	private final static String[] DET_CONFUSION_SET = new String[] {
		"a",
//		"an",
		"the",
		"this"
	};
	
    public static final String KEY_META = "metaModel";
    public static final String KEY_OUTPUT = "outputDir";
	public static final String KEY_TRAINING = "trainingModel";

	
	
	public static void main(String[] args) throws Exception {
		
		HOO2012Experiments exp = new HOO2012Experiments();
		
		// baselines
		exp.runBaselineExperiments(
				ART.class.getName(),
				"classpath:/candidates/articles.txt",
				"the"
		);
		exp.runBaselineExperiments(
				PP.class.getName(),
				"classpath:/candidates/prepositions.txt",
				"of"
		);
		
		// old statistical approach from HOO 2011 adapted to only target articles and prepositions
		// also adapted to limit possible candidates to a list of candidates
		// ARTICLES only
		exp.runStatisticalExperiments(
				ART.class.getName(),
				"classpath:/candidates/articles.txt"
		);
		// PREPOSITIONS only
		exp.runStatisticalExperiments(
				PP.class.getName(),
				"classpath:/candidates/prepositions.txt"
		);
		
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
		Task baselineTask = new UimaTaskBase() {
	
			@Discriminator String langCode;
			@Discriminator String teamId;
			@Discriminator String testDataPath;
			@Discriminator String goldDataPath;
			
		    { setType("Baseline"); }
		
		    @Override
		    public CollectionReaderDescription getCollectionReaderDescription(TaskContext aContext)
		            throws ResourceInitializationException, IOException
		    {
		            return getReader(testDataPath);
		    }
		
		    @Override
		    public AnalysisEngineDescription getAnalysisEngineDescription(TaskContext aContext)
		            throws ResourceInitializationException, IOException
		    {
		        File outputDir = aContext.getStorageLocation(
		        		KEY_OUTPUT,
						AccessMode.READWRITE
		        );
		
		        return createAggregateDescription(
						createPrimitiveDescription(BreakIteratorSegmenter.class),
						createPrimitiveDescription(
		                        TreeTaggerPosLemmaTT4J.class,
		                        TreeTaggerPosLemmaTT4J.PARAM_LANGUAGE_CODE, langCode
		                ),
		                createPrimitiveDescription(
		                        FixedCandidateAnnotator.class,
		                        FixedCandidateAnnotator.PARAM_TYPE, targetType,
		                        FixedCandidateAnnotator.PARAM_CANDIDATE_FILE, confusionSetFile
		                ),
		                createPrimitiveDescription(
		                        BaselineCorrectionAnnotator.class,
		                        BaselineCorrectionAnnotator.PARAM_REPLACEMENT, replacement
		                ),
		                createPrimitiveDescription(
		        				HOO2012Evaluator.class,
		        				HOO2012Evaluator.PARAM_OUTPUT_PATH, outputDir + "/",
		        				HOO2012Evaluator.PARAM_EXTRACTION_PATH, outputDir + "/extraction/",
		        				HOO2012Evaluator.PARAM_GOLD_PATH, goldDataPath,
		        				HOO2012Evaluator.PARAM_TEAM_ID, teamId,
		        				HOO2012Evaluator.PARAM_RUN_ID, "0",
		        				HOO2012Evaluator.PARAM_WRITE_EDITS, WRITE_EDITS
		        		)
				);
		    }
		};
	
		ParameterSpace pSpace = new ParameterSpace(
				Dimension.create("langCode",     LANG),
				Dimension.create("teamId",       TEAM_ID),
				Dimension.create("testDataPath", INPUT_DATA),
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
		Task statisticalTask = new UimaTaskBase() {

			@Discriminator String langCode;
			@Discriminator String teamId;
			@Discriminator String testDataPath;
			@Discriminator String goldDataPath;
			@Discriminator float threshold;
							
		    { setType("StatisticalCorrection"); }
		
		    @Override
		    public CollectionReaderDescription getCollectionReaderDescription(TaskContext aContext)
		            throws ResourceInitializationException, IOException
		    {
		            return getReader(testDataPath);
		    }
		
		    @Override
		    public AnalysisEngineDescription getAnalysisEngineDescription(TaskContext aContext)
		            throws ResourceInitializationException, IOException
		    {
		        File outputDir = aContext.getStorageLocation(
		        		KEY_OUTPUT,
						AccessMode.READWRITE
		        );
		
		        return createAggregateDescription(
						createPrimitiveDescription(BreakIteratorSegmenter.class),
						createPrimitiveDescription(
		                        TreeTaggerPosLemmaTT4J.class,
		                        TreeTaggerPosLemmaTT4J.PARAM_LANGUAGE_CODE, langCode
		                ),
		                createPrimitiveDescription(
		                        FixedCandidateAnnotator.class,
		                        FixedCandidateAnnotator.PARAM_TYPE, targetType,
		                        FixedCandidateAnnotator.PARAM_CANDIDATE_FILE, confusionSetFile
		                ),
		                createPrimitiveDescription(
		                        FixedCandidateTrigramProbabilityDetector.class,
		                        FixedCandidateTrigramProbabilityDetector.PARAM_LANGUAGE_CODE, "en",
		                        FixedCandidateTrigramProbabilityDetector.PARAM_VOCABULARY, "classpath:/vocabulary/en_US_dict.txt",
		                        FixedCandidateTrigramProbabilityDetector.PARAM_MIN_LENGTH, 2,
		                        FixedCandidateTrigramProbabilityDetector.PARAM_ALPHA, threshold,
		                        FixedCandidateTrigramProbabilityDetector.PARAM_CANDIDATE_FILE, confusionSetFile,
		                        FixedCandidateTrigramProbabilityDetector.FREQUENCY_PROVIDER_RESOURCE, getNGramProvider(NGRAM_MODEL)
		                ),
		                createPrimitiveDescription(
		        				HOO2012Evaluator.class,
		        				HOO2012Evaluator.PARAM_OUTPUT_PATH, outputDir + "/",
		        				HOO2012Evaluator.PARAM_EXTRACTION_PATH, outputDir + "/extraction/",
		        				HOO2012Evaluator.PARAM_GOLD_PATH, goldDataPath,
		        				HOO2012Evaluator.PARAM_TEAM_ID, teamId,
		        				HOO2012Evaluator.PARAM_RUN_ID, "0",
		        				HOO2012Evaluator.PARAM_WRITE_EDITS, WRITE_EDITS
		        		)
		        );
		    }
		};
    
		ParameterSpace pSpace = new ParameterSpace(
				Dimension.create("langCode",     LANG),
				Dimension.create("teamId",       TEAM_ID),
				Dimension.create("testDataPath", INPUT_DATA),
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
		Task metaInfoTask = new UimaTaskBase() {

			@Discriminator String langCode;

			{ setType("MetaPreprocessing"); }
		
		    @Override
		    public CollectionReaderDescription getCollectionReaderDescription(TaskContext aContext)
		            throws ResourceInitializationException, IOException
		    {
		            return getBrownCorpusReader(langCode);
		    }
		
		    @Override
		    public AnalysisEngineDescription getAnalysisEngineDescription(TaskContext aContext)
		            throws ResourceInitializationException, IOException
		    {
		        File metaDir = aContext.getStorageLocation(
		        		KEY_META,
						AccessMode.READWRITE
		        );
		
		        return createAggregateDescription(
		        		createPrimitiveDescription(BreakIteratorSegmenter.class),
						createPrimitiveDescription(
		                        TreeTaggerPosLemmaTT4J.class,
		                        TreeTaggerPosLemmaTT4J.PARAM_LANGUAGE_CODE, langCode
		                ),
		        		createPrimitiveDescription(TreeTaggerChunkerTT4J.class),
		        		createPrimitiveDescription(
		        				MetaCollector.class,
		        				MetaCollector.PARAM_META_FILE, metaDir + "/metaRT.xml",
		        				MetaCollector.PARAM_RELATION, "RT",
		        				MetaCollector.PARAM_CONFUSION_SET, PREP_CONFUSION_SET,
		        				MetaCollector.FREQUENCY_PROVIDER_RESOURCE, getNGramProvider(NGRAM_MODEL)
						),
						createPrimitiveDescription(
								MetaCollector.class,
								MetaCollector.PARAM_META_FILE, metaDir + "/metaRD.xml",
								MetaCollector.PARAM_RELATION, "RD",
								MetaCollector.PARAM_CONFUSION_SET, DET_CONFUSION_SET,
								MetaCollector.FREQUENCY_PROVIDER_RESOURCE, getNGramProvider(NGRAM_MODEL)
						)
				);
		    }
		};
		
		Task trainingTask = new UimaTaskBase() {

			@Discriminator String langCode;
			
		    { setType("TrainingPreprocessing"); }
		
		    @Override
		    public CollectionReaderDescription getCollectionReaderDescription(TaskContext aContext)
		            throws ResourceInitializationException, IOException
		    {
		            return getBrownCorpusReader(langCode);
		    }
		
		    @Override
		    public AnalysisEngineDescription getAnalysisEngineDescription(TaskContext aContext)
		            throws ResourceInitializationException, IOException
		    {
		    	File metaDir = aContext.getStorageLocation(
		        		KEY_META,
						AccessMode.READWRITE
		        );
		    	
		    	File arffDir = aContext.getStorageLocation(
		        		KEY_TRAINING,
						AccessMode.READWRITE
		        );

		    	return createAggregateDescription(
						createPrimitiveDescription(BreakIteratorSegmenter.class),
						createPrimitiveDescription(
		                        TreeTaggerPosLemmaTT4J.class,
		                        TreeTaggerPosLemmaTT4J.PARAM_LANGUAGE_CODE, langCode
		                ),
						createPrimitiveDescription(TreeTaggerChunkerTT4J.class),
						createPrimitiveDescription(
								CorrectionAnnotator.class,
								CorrectionAnnotator.PARAM_CATEGORY_CLASS, "RT",
								CorrectionAnnotator.PARAM_CONFUSION_SET, PREP_CONFUSION_SET,
								CorrectionAnnotator.PARAM_DATA_WRITER_FACTORY_CLASS_NAME, DefaultWekaDataWriterFactory.class.getName(),
									DefaultWekaDataWriterFactory.PARAM_ARFF_FILE, arffDir + "/arffRT.arff",
									DefaultWekaDataWriterFactory.PARAM_META_FILE, metaDir + "/metaRT.xml",
								CorrectionAnnotator.FREQUENCY_PROVIDER_RESOURCE, getNGramProvider(NGRAM_MODEL)
						),
		
						createPrimitiveDescription(
								CorrectionAnnotator.class,
								CorrectionAnnotator.PARAM_CATEGORY_CLASS, "RD",
								CorrectionAnnotator.PARAM_CONFUSION_SET, DET_CONFUSION_SET,
								CorrectionAnnotator.PARAM_DATA_WRITER_FACTORY_CLASS_NAME, DefaultWekaDataWriterFactory.class.getName(),
									DefaultWekaDataWriterFactory.PARAM_ARFF_FILE, arffDir + "/arffRD.arff",
									DefaultWekaDataWriterFactory.PARAM_META_FILE, metaDir + "/metaRD.xml",
								CorrectionAnnotator.FREQUENCY_PROVIDER_RESOURCE, getNGramProvider(NGRAM_MODEL)
						)

		    	);
		    }
		};
		
		Task testSVMRegression = getTestTask(SMOClassifierFactory.class.getName());
		Task testSVM           = getTestTask(SMOClassifierWithoutRegressionFactory.class.getName());
		Task testNaiveBayes    = getTestTask(NaiveBayesClassifierFactory.class.getName());
    
		ParameterSpace pSpaceDefault = new ParameterSpace(
				Dimension.create("langCode",     LANG),
				Dimension.create("teamId",       TEAM_ID),
				Dimension.create("testDataPath", INPUT_DATA),
				Dimension.create("goldDataPath", GOLD_DATA)
		);

		ParameterSpace pSpaceRegression = new ParameterSpace(
				Dimension.create("langCode",     LANG),
				Dimension.create("teamId",       TEAM_ID),
				Dimension.create("testDataPath", INPUT_DATA),
				Dimension.create("goldDataPath", GOLD_DATA),
				Dimension.create("thresholdRT",  0.95f, 0.8f, 0.5f ),
				Dimension.create("thresholdRD",  0.8f,  0.7f, 0.3f )
		);

	    // for making training dependent on meta
	    trainingTask.addImportLatest(
	    		KEY_META,
	    		KEY_META,
	    		metaInfoTask.getType()
	    );

	    // for making test dependent on meta and training
	    testNaiveBayes.addImportLatest(KEY_META, KEY_META, metaInfoTask.getType());
	    testNaiveBayes.addImportLatest(KEY_TRAINING, KEY_TRAINING, trainingTask.getType());

	    testSVM.addImportLatest(KEY_META, KEY_META, metaInfoTask.getType());
	    testSVM.addImportLatest(KEY_TRAINING, KEY_TRAINING, trainingTask.getType());

	    testSVMRegression.addImportLatest(KEY_META, KEY_META, metaInfoTask.getType());
	    testSVMRegression.addImportLatest(KEY_TRAINING, KEY_TRAINING, trainingTask.getType());
    
	    BatchTask batchNB = new BatchTask();
	    batchNB.setParameterSpace(pSpaceDefault);
	    batchNB.setExecutionPolicy(ExecutionPolicy.USE_EXISTING);
	    batchNB.addTask(metaInfoTask);
	    batchNB.addTask(trainingTask);
	    batchNB.addTask(testNaiveBayes);

	    Lab.getInstance().run(batchNB);


	    BatchTask batchSVM = new BatchTask();
	    batchSVM.setParameterSpace(pSpaceDefault);
	    batchSVM.setExecutionPolicy(ExecutionPolicy.USE_EXISTING);
	    batchSVM.addTask(metaInfoTask);
	    batchSVM.addTask(trainingTask);
	    batchSVM.addTask(testSVM);

	    Lab.getInstance().run(batchSVM);

	    
	    BatchTask batchRegression = new BatchTask();
	    batchRegression.setParameterSpace(pSpaceRegression);
	    batchRegression.setExecutionPolicy(ExecutionPolicy.USE_EXISTING);
	    batchRegression.addTask(metaInfoTask);
	    batchRegression.addTask(trainingTask);
	    batchRegression.addTask(testSVMRegression);

	    Lab.getInstance().run(batchRegression);
	}

	private Task getTestTask(final String classifier) {
		return new UimaTaskBase() {

			@Discriminator String langCode;
			@Discriminator String teamId;
			@Discriminator String testDataPath;
			@Discriminator String goldDataPath;
			@Discriminator float thresholdRT;
			@Discriminator float thresholdRD;

			{ setType("Test" + classifier); }
		
		    @Override
		    public CollectionReaderDescription getCollectionReaderDescription(TaskContext aContext)
		            throws ResourceInitializationException, IOException
		    {
		            return getReader(testDataPath);
		    }
		
		    @Override
		    public AnalysisEngineDescription getAnalysisEngineDescription(TaskContext aContext)
		            throws ResourceInitializationException, IOException
		    {
		    	File metaDir = aContext.getStorageLocation(
		        		KEY_META,
						AccessMode.READWRITE
		        );
		    	
		    	File arffDir = aContext.getStorageLocation(
		        		KEY_TRAINING,
						AccessMode.READWRITE
		        );
		    	
		    	File outputDir = aContext.getStorageLocation(
		        		KEY_OUTPUT,
						AccessMode.READWRITE
		        );

		    	return createAggregateDescription(
						createPrimitiveDescription(BreakIteratorSegmenter.class),
						createPrimitiveDescription(
		                        TreeTaggerPosLemmaTT4J.class,
		                        TreeTaggerPosLemmaTT4J.PARAM_LANGUAGE_CODE, langCode
		                ),
						createPrimitiveDescription(TreeTaggerChunkerTT4J.class),
						createPrimitiveDescription(
								CorrectionAnnotator.class,
								CorrectionAnnotator.PARAM_CATEGORY_CLASS, "RT",
								CorrectionAnnotator.PARAM_CLASSIFIER_FACTORY_CLASS_NAME, classifier,
									SMOClassifierFactory.PARAM_META_FILE, metaDir + "/metaRT.xml",
									SMOClassifierFactory.PARAM_ARFF_FILE, arffDir + "/arffRT.arff",
									SMOClassifierFactory.PARAM_THRESHOLD, thresholdRT,
								CorrectionAnnotator.PARAM_CONFUSION_SET, PREP_CONFUSION_SET,
								CorrectionAnnotator.FREQUENCY_PROVIDER_RESOURCE, getNGramProvider(NGRAM_MODEL)
						),
						createPrimitiveDescription(
								CorrectionAnnotator.class,
								CorrectionAnnotator.PARAM_CATEGORY_CLASS, "RD",
								CorrectionAnnotator.PARAM_CLASSIFIER_FACTORY_CLASS_NAME, classifier,
									SMOClassifierFactory.PARAM_META_FILE, metaDir + "/metaRD.xml",
									SMOClassifierFactory.PARAM_ARFF_FILE, arffDir + "/arffRD.arff",
									SMOClassifierFactory.PARAM_THRESHOLD, thresholdRD,
								CorrectionAnnotator.PARAM_CONFUSION_SET, DET_CONFUSION_SET,
								CorrectionAnnotator.FREQUENCY_PROVIDER_RESOURCE, getNGramProvider(NGRAM_MODEL)
						),
						createPrimitiveDescription(
								HOO2012Evaluator.class,
								HOO2012Evaluator.PARAM_OUTPUT_PATH, outputDir + "/",
								HOO2012Evaluator.PARAM_EXTRACTION_PATH, outputDir + "/extraction/",
								HOO2012Evaluator.PARAM_GOLD_PATH, goldDataPath,
								HOO2012Evaluator.PARAM_TEAM_ID, teamId,
								HOO2012Evaluator.PARAM_RUN_ID, "0",
								HOO2012Evaluator.PARAM_WRITE_EDITS, WRITE_EDITS
						)		    	);
		    }
		};
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
	
	private ExternalResourceDescription getNGramProvider(String ngramModel) throws IOException {
		File context = DKProContext.getContext().getWorkspace("web1t");
		System.out.println(new File(context, ngramModel).getAbsolutePath());
		
		return createExternalResourceDescription(
			Web1TFrequencyCountResource.class,
			Web1TFrequencyCountResource.PARAM_MIN_NGRAM_LEVEL, MIN_NGRAM_LEVEL,
			Web1TFrequencyCountResource.PARAM_MAX_NGRAM_LEVEL, MAX_NGRAM_LEVEL,
			Web1TFrequencyCountResource.PARAM_INDEX_PATH, new File(context, ngramModel).getAbsolutePath());
	}

	private CollectionReaderDescription getBrownCorpusReader(String languageCode) throws ResourceInitializationException, IOException  {
		File brownPath = DKProContext.getContext().getWorkspace("brown_tei_small");
		
		return createDescription(
				TEIReader.class,
				TEIReader.PARAM_LANGUAGE, languageCode,
				TEIReader.PARAM_PATH, brownPath.getAbsolutePath(),
				TEIReader.PARAM_PATTERNS, new String[] {
				TEIReader.INCLUDE_PREFIX + "**/*.xml" }
		);
	}
}
