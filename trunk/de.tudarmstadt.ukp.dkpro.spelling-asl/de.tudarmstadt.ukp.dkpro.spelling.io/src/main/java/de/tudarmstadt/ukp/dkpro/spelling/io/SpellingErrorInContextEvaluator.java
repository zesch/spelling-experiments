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
package de.tudarmstadt.ukp.dkpro.spelling.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.type.GoldSpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.spelling.io.SpellingErrorPerformanceCounter.Mode;

/**
 * Evaluates the performance of a spelling correction component.
 * 
 * @author zesch
 *
 */
public class SpellingErrorInContextEvaluator extends JCasAnnotator_ImplBase {

    public static final String PARAM_OUTPUT_FILE = "OutputFile";
    @ConfigurationParameter(name=PARAM_OUTPUT_FILE, mandatory=false)
    protected File outputFile;

    public static final String PARAM_LAB_OUTPUT_FILE = "LabOutputFile";
    @ConfigurationParameter(name=PARAM_LAB_OUTPUT_FILE, mandatory=false)
    protected File labOutputFile;

    private BufferedWriter writer;
    private BufferedWriter labWriter;
    
    private SpellingErrorPerformanceCounter performanceCounter;

    @Override
    public void initialize(UimaContext context)
        throws ResourceInitializationException
    {
        super.initialize(context);
        
        performanceCounter = new SpellingErrorPerformanceCounter();
        
        try {
            if (outputFile == null) {
                outputFile = new File("target/output.txt");
            }
            if (outputFile.exists()) {
                outputFile.delete();
            }
            FileUtils.touch(outputFile);
            writer = new BufferedWriter(new FileWriter(outputFile));
            
            if (labOutputFile != null) {
                if (labOutputFile.exists()) {
                    labOutputFile.delete();
                }
                FileUtils.touch(labOutputFile);
                labWriter = new BufferedWriter(new FileWriter(labOutputFile));
            }
            else {
                labWriter = null;
            }
        }
        catch (IOException e) {
            throw new ResourceInitializationException(e);
        }
    }
    
    @Override
    public void process(JCas jcas)
        throws AnalysisEngineProcessException
    {
        // collect performance statistics

        DocumentMetaData dmd = DocumentMetaData.get(jcas);
        String fileName = dmd.getDocumentTitle();
        
        writeOutput("file: " + fileName);
        
        performanceCounter.registerFile(fileName);

        List<SpellingAnomaly> golds = new ArrayList<SpellingAnomaly>();
        List<SpellingAnomaly> detections = new ArrayList<SpellingAnomaly>();

        // retrieve 
        for (SpellingAnomaly anomaly : JCasUtil.select(jcas, SpellingAnomaly.class)) {
            if (anomaly instanceof GoldSpellingAnomaly) {
                golds.add(anomaly);
            }
            else {
                detections.add(anomaly);
            }
        }

        // remove duplicates
        golds = removeDuplicates(golds);
        detections = removeDuplicates(detections);        

        // use the pairs to obtain the tp, fp, and fn scores.
        for (AnomalyPair pair : getAnomalyPairs(detections, golds)) {

            writeOutput(pair.toString());
            
            // gold error with no matching detection
            if (!pair.isSetAnomaly()) {
                writeOutput("FN");

                performanceCounter.increaseFileFN(fileName, Mode.detection);
                performanceCounter.increaseFileFN(fileName, Mode.correction);
            }

            // detection with no matching gold
            else if (!pair.isSetGoldAnomaly()) {
                writeOutput("FP");

                performanceCounter.increaseFileFP(fileName, Mode.detection);
                performanceCounter.increaseFileFP(fileName, Mode.correction);
            }
            
            // correctly detected
            else {
                performanceCounter.increaseFileTP(fileName, Mode.detection);

                writeOutput("TP");
                
                // also correct suggestion?
                SpellingAnomaly detection = pair.getAnomaly();
                SpellingAnomaly gold = pair.getGoldAnomaly();
                
                this.getContext().getLogger().log(Level.FINE, gold.getSuggestions(0).getReplacement() + " / " + detection.getSuggestions(0).getReplacement());
                
                if (gold.getSuggestions(0).getReplacement().equals(detection.getSuggestions(0).getReplacement())) {
                    performanceCounter.increaseFileTP(fileName, Mode.correction);
                }
                else {
                    performanceCounter.increaseFileFP(fileName, Mode.correction);
                }
                
            }
        }
    }

    @Override
    public void collectionProcessComplete()
        throws AnalysisEngineProcessException
    {
        super.collectionProcessComplete();
        
        writeOutput(performanceCounter.getFilePerformanceOverview(Mode.detection));
        
        writeOutput(performanceCounter.getFilePerformanceOverview(Mode.correction));

        writeOutput(performanceCounter.getMicroPerformanceOverview(Mode.detection));
        writeOutput(performanceCounter.getMacroPerformanceOverview(Mode.detection));
        
        writeOutput(performanceCounter.getMicroPerformanceOverview(Mode.correction));
        writeOutput(performanceCounter.getMacroPerformanceOverview(Mode.correction));
        
        // write an additional properties file for easy aggregation in the lab
        if (labWriter != null) {
            Properties props = new Properties();
            props.setProperty("P-detection", String.valueOf(performanceCounter.getMicroPrecision(Mode.detection)));
            props.setProperty("R-detection", String.valueOf(performanceCounter.getMicroRecall(Mode.detection)));
            props.setProperty("F-detection", String.valueOf(performanceCounter.getMicroFMeasure(Mode.detection)));
            props.setProperty("P-correction", String.valueOf(performanceCounter.getMicroPrecision(Mode.correction)));
            props.setProperty("R-correction", String.valueOf(performanceCounter.getMicroRecall(Mode.correction)));
            props.setProperty("F-correction", String.valueOf(performanceCounter.getMicroFMeasure(Mode.correction)));
            try {
                props.store(labWriter, null);
            }
            catch (IOException e) {
                throw new AnalysisEngineProcessException(e);
            }
        }
    }
    
    private <T extends SpellingAnomaly> List<T> removeDuplicates(Collection<T> collection) {
        List<T> filteredList = new ArrayList<T>();
        
        Set<String> fingerprints = new HashSet<String>();
        
        for (T t : collection) {
            String fingerprint = getFingerprint(t);
            if (!fingerprints.contains(fingerprint)) {
                fingerprints.add(fingerprint);
                filteredList.add(t);
            }
        }
        
        return filteredList;
    }
    
    private <T extends SpellingAnomaly> String getFingerprint(T t) {
        return t.getBegin() + t.getEnd() + t.getSuggestions(0).getReplacement();
    }
    
    /**
     * Form pairs between detected and gold standard errors.
     * If no counterpart can be found, incomplete pairs are formed.
     *  
     * @param detections The detected spelling errors
     * @param golds The gold standard spelling errors
     * @return The list of pairs between detected and gold standard errors.
     */
    private List<AnomalyPair> getAnomalyPairs(Collection<SpellingAnomaly> detections,
            Collection<SpellingAnomaly> golds)
    {
        List<AnomalyPair> pairs = new ArrayList<AnomalyPair>();
        
        for (SpellingAnomaly gold : golds) {
            for (SpellingAnomaly detection : detections) {
                if (isPair(detection, gold)) {
                    pairs.add(new AnomalyPair(detection, gold));
                }
            }
        }
        
        // remove already paired detections and golds from the lists 
        List<SpellingAnomaly> detectionsList = new ArrayList<SpellingAnomaly>(detections);
        List<SpellingAnomaly> goldsList = new ArrayList<SpellingAnomaly>(golds);
        for (AnomalyPair pair : pairs) {
            detectionsList.remove(pair.getAnomaly());
            goldsList.remove(pair.getGoldAnomaly());
        }

        // add remaining incomplete pairs
        for (SpellingAnomaly detection : detectionsList) {
            pairs.add(new AnomalyPair(detection, null));
        }
        for (SpellingAnomaly gold : goldsList) {
            pairs.add(new AnomalyPair(null, gold));
        }
        
        return pairs;
    }

    /**
     * A pair is formed, if the offset of the two annotations match.
     * 
     * @param detection The detected spelling error
     * @param gold The gold standard spelling error
     * @return Whether the detected spelling error and the gold spelling error should be paired.
     */
    private boolean isPair(SpellingAnomaly detection, SpellingAnomaly gold) {
        return (gold.getBegin() == detection.getBegin() &&
                gold.getEnd()   == detection.getEnd());
    }
    
    /**
     * Helper class for holding internally formed pairs between detected and gold standard errors.
     * 
     * @author zesch
     *
     */
    private class AnomalyPair
    {
        private final SpellingAnomaly anomaly;
        private final SpellingAnomaly goldAnomaly;
        
        public AnomalyPair(SpellingAnomaly anomaly, SpellingAnomaly goldAnomaly) 
        {
            this.anomaly = anomaly;
            this.goldAnomaly = goldAnomaly;
        }
        
        public boolean isSetAnomaly()
        {
            return anomaly != null;
        }
        
        public boolean isSetGoldAnomaly()
        {
            return goldAnomaly != null;
        }

        public SpellingAnomaly getAnomaly()
        {
            return anomaly;
        }
        
        public SpellingAnomaly getGoldAnomaly()
        {
            return goldAnomaly;
        }

        @Override
        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Gold: ");
            if (goldAnomaly != null) {
                sb.append(goldAnomaly.getCoveredText()); sb.append(" - "); sb.append(goldAnomaly.getSuggestions(0).getReplacement());
            }
            else {
                sb.append("# null #");
            }
            sb.append(System.getProperty("line.separator"));
            sb.append("Anomaly: ");
            if (anomaly != null) {
                sb.append(anomaly.getCoveredText()); sb.append(" - "); sb.append(anomaly.getSuggestions(0).getReplacement());
            }
            else {
                sb.append("# null #");
            }
            
            return sb.toString();
        }
    }

    private void writeOutput(String message) throws AnalysisEngineProcessException {
        
        this.getContext().getLogger().log(Level.INFO, message);
        
        try {
            writer.write(message);
            writer.newLine();
            writer.flush();
        }
        catch (IOException e) {
            throw new AnalysisEngineProcessException(e);
        }
    }

    @Override
    public void destroy()
    {
        super.destroy();

        try {
            writer.flush();
            writer.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
