/*******************************************************************************
 * Copyright 2010
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
package de.tudarmstadt.ukp.dkpro.spelling.experiments.eacl2012;

import java.io.IOException;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.factory.CollectionReaderFactory;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.N;
import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.core.treetagger.TreeTaggerPosLemmaTT4J;
import de.tudarmstadt.ukp.dkpro.lab.Lab;
import de.tudarmstadt.ukp.dkpro.lab.engine.TaskContext;
import de.tudarmstadt.ukp.dkpro.lab.storage.StorageService.AccessMode;
import de.tudarmstadt.ukp.dkpro.lab.task.Dimension;
import de.tudarmstadt.ukp.dkpro.lab.task.Discriminator;
import de.tudarmstadt.ukp.dkpro.lab.task.ParameterSpace;
import de.tudarmstadt.ukp.dkpro.lab.task.impl.BatchTask;
import de.tudarmstadt.ukp.dkpro.lab.uima.task.impl.UimaTaskBase;
import de.tudarmstadt.ukp.dkpro.spelling.detector.knowledge.KnowledgeBasedDetector;
import de.tudarmstadt.ukp.dkpro.spelling.detector.knowledge.LexCohesionDetector;
import de.tudarmstadt.ukp.dkpro.spelling.detector.ngram.TrigramProbabilityDetector;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.core.RWSECandidateAnnotator;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.core.RWSECandidateFilter;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.core.SpellingErrorCombinator;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.core.SpellingErrorCombinator.CombinationStrategy;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.core.util.MeasureConfig;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.data.util.DataUtil;
import de.tudarmstadt.ukp.dkpro.spelling.io.SpellingErrorInContextEvaluator;
import de.tudarmstadt.ukp.dkpro.spelling.io.SpellingErrorInContextReader;
import de.tudarmstadt.ukp.dkpro.spelling.io.SpellingErrorInContextReader_LongFormat;
import de.tudarmstadt.ukp.similarity.dkpro.resource.vsm.VectorIndexSourceRelatednessResource;

public class CombinationExperiments extends EACL_ExperimentsBase
{
    
    public static class ExperimentTask extends UimaTaskBase
    {
        @Discriminator private String datasetPath;
        @Discriminator private String languageCode;
        @Discriminator private float alpha;
        @Discriminator private float threshold;
        @Discriminator private boolean lowerCase;
        @Discriminator private SupportedFrequencyProviders freqProvider;
        @Discriminator private int downscaleFactor;
        @Discriminator private String candidateType;
        @Discriminator private CombinationStrategy combinationStrategy;
        @Discriminator private MeasureConfig measure;

        @Override
        public CollectionReaderDescription getCollectionReaderDescription(TaskContext aContext)
            throws ResourceInitializationException, IOException
        {
                return CollectionReaderFactory.createDescription(
                        SpellingErrorInContextReader_LongFormat.class,
                        SpellingErrorInContextReader.PARAM_INPUT_FILE, datasetPath
                );
        }

        @Override
        public AnalysisEngineDescription getAnalysisEngineDescription(TaskContext aContext)
            throws ResourceInitializationException, IOException
        {
            return createEngine(
                    createEngine(BreakIteratorSegmenter.class),
                    createEngine(
                            TreeTaggerPosLemmaTT4J.class,
                            TreeTaggerPosLemmaTT4J.PARAM_LANGUAGE_CODE, languageCode
                    ),
                    createEngine(
                            RWSECandidateAnnotator.class,
                            RWSECandidateAnnotator.PARAM_TYPE, candidateType
                    ),
                    createEngine(
                            RWSECandidateFilter.class,
                            RWSECandidateFilter.PARAM_STOPWORD_LIST, blacklistMap.get(languageCode),
                            RWSECandidateFilter.PARAM_MIN_LENGTH, MIN_LENGTH,
                            RWSECandidateFilter.FREQUENCY_PROVIDER_RESOURCE, getFrequencyProviderResource(
                                    freqProvider,
                                    languageCode
                            )
                    ),
                    createEngine(
                            TrigramProbabilityDetector.class,
                            TrigramProbabilityDetector.PARAM_LANGUAGE_CODE, languageCode,
                            TrigramProbabilityDetector.PARAM_VOCABULARY, vocabularyMap.get(languageCode),
                            TrigramProbabilityDetector.PARAM_MIN_LENGTH, MIN_LENGTH,
                            TrigramProbabilityDetector.PARAM_ALPHA, alpha,
                            TrigramProbabilityDetector.FREQUENCY_PROVIDER_RESOURCE, getFrequencyProviderResource(
                                    freqProvider,
                                    languageCode,
                                    downscaleFactor
                            )
                    ),
                    createEngine(
                            LexCohesionDetector.class,
                            KnowledgeBasedDetector.PARAM_LANGUAGE_CODE, languageCode,
                            KnowledgeBasedDetector.PARAM_VOCABULARY, vocabularyMap.get(languageCode),
                            KnowledgeBasedDetector.PARAM_THRESHOLD, threshold,
                            KnowledgeBasedDetector.PARAM_MIN_LENGTH, MIN_LENGTH,
                            KnowledgeBasedDetector.PARAM_LOWER_CASE, lowerCase,
                            KnowledgeBasedDetector.SR_RESOURCE, getSRResource(measure)
                    ),
                    createEngine(
                            SpellingErrorCombinator.class,
                            SpellingErrorCombinator.PARAM_COMBINATION_STRATEGY, combinationStrategy.name()
                    ),
                    createEngine(
                            SpellingErrorInContextEvaluator.class,
                            SpellingErrorInContextEvaluator.PARAM_OUTPUT_FILE,
                            aContext.getStorageLocation("RESULT.txt", AccessMode.READWRITE).getPath(),
                            SpellingErrorInContextEvaluator.PARAM_LAB_OUTPUT_FILE,
                            aContext.getStorageLocation(LAB_RESULTS_FILE, AccessMode.READWRITE).getPath()
                    ) 
            );
        }
    }

    public static void main(String[] args) throws Exception
    {
        
        ParameterSpace enPspace = new ParameterSpace(
                Dimension.createBundle("dataset", datasetMap2datasetArray(
                        DataUtil.getAllDatasets("classpath*:/datasets/en", new String[]{"txt"}),
                        "en")
                ),
                Dimension.create("combinationStrategy", CombinationStrategy.join, CombinationStrategy.onlyKeepMultiple),
                Dimension.create("threshold",           0.05f),
                Dimension.create("lowerCase",           true),
                Dimension.create("freqProvider",        SupportedFrequencyProviders.google),
                Dimension.create("alpha",               StatisticalApproach.ALPHA),
                Dimension.create("downscaleFactor",     1),
                Dimension.create("candidateType",       N.class.getName()),
                Dimension.create("measure",             new MeasureConfig(
                        VectorIndexSourceRelatednessResource.class,
                        VectorIndexSourceRelatednessResource.PARAM_MODEL_LOCATION,
                        getWorkspacePath("esaIndexesVector" + "/en/wkt/")))
        );

        ParameterSpace dePspace = new ParameterSpace(
                Dimension.createBundle("dataset", datasetMap2datasetArray(
                        DataUtil.getAllDatasets("classpath*:/datasets/de", new String[]{"txt"}),
                        "de")
                ),
                Dimension.create("combinationStrategy", CombinationStrategy.join, CombinationStrategy.onlyKeepMultiple),
                Dimension.create("threshold",           0.01f),
                Dimension.create("lowerCase",           true),
                Dimension.create("freqProvider",        SupportedFrequencyProviders.google),
                Dimension.create("alpha",               StatisticalApproach.ALPHA),
                Dimension.create("downscaleFactor",     1),
                Dimension.create("candidateType",       N.class.getName()),
                Dimension.create("measure",             new MeasureConfig(
                        VectorIndexSourceRelatednessResource.class,
                        VectorIndexSourceRelatednessResource.PARAM_MODEL_LOCATION,
                        getWorkspacePath("esaIndexesVector" + "/de/wp/")))
        );

        BatchTask batchTaskEn = new BatchTask();
        batchTaskEn.setParameterSpace(enPspace);
        batchTaskEn.addTask(new ExperimentTask());
        batchTaskEn.addReport(RwseBatchResultReport.class);

        BatchTask batchTaskDe = new BatchTask();
        batchTaskDe.setParameterSpace(dePspace);
        batchTaskDe.addTask(new ExperimentTask());
        batchTaskDe.addReport(RwseBatchResultReport.class);

        Lab.getInstance().run(batchTaskEn);
        Lab.getInstance().run(batchTaskDe);
    }
}
