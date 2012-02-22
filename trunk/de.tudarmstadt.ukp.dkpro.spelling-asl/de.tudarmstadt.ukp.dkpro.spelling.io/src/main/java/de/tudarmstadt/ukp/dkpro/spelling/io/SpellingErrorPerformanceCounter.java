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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;


public class SpellingErrorPerformanceCounter {

    public enum Mode {
        detection,
        correction
    }
    
    private static final String LF = System.getProperty("line.separator");

    private final Map<String,FilePerformance> context2PerformanceMap;

    public SpellingErrorPerformanceCounter()
    {
        this.context2PerformanceMap = new HashMap<String,FilePerformance>();
    }

    public void registerFile(String filename)
        throws AnalysisEngineProcessException
    {
        if (context2PerformanceMap.containsKey(filename)) {
            throw new AnalysisEngineProcessException(
                    new Throwable("Filename '" + filename + "' already registered.")
            );
        }
        context2PerformanceMap.put(
                filename,
                new FilePerformance()
        );
    }

    public Set<String> getRegisteredFiles() {
        return context2PerformanceMap.keySet();
    }

    public double getMicroPrecision(Mode mode) {
        int tpCount = 0;
        int fpCount = 0;

        for (String fileName : context2PerformanceMap.keySet()) {
            tpCount += context2PerformanceMap.get(fileName).getTP(mode);;
            fpCount += context2PerformanceMap.get(fileName).getFP(mode);;
        }
        if (tpCount + fpCount > 0) {
            return (double) tpCount / (tpCount + fpCount);
        }
        else {
            return 0.0;
        }
    }

    public double getMicroRecall(Mode mode) {
        int tpCount = 0;
        int fnCount = 0;

        for (String fileName : context2PerformanceMap.keySet()) {
            tpCount += context2PerformanceMap.get(fileName).getTP(mode);;
            fnCount += context2PerformanceMap.get(fileName).getFN(mode);;
        }
        if (tpCount + fnCount > 0) {
            return (double) tpCount / (tpCount + fnCount);
        }
        else {
            return 0.0;
        }
    }

    public double getMicroFMeasure(Mode mode) {
        double precision = getMicroPrecision(mode);
        double recall    = getMicroRecall(mode);
        
        if (precision + recall > 0) {
            return (2 * precision * recall / (precision + recall));
        }
        else {
            return 0.0;
        }
    }

    public double getMacroPrecision(Mode mode) {
        double precision = 0.0;
        for (String fileName : context2PerformanceMap.keySet()) {
            precision += getFilePrecision(fileName, mode);
        }
        return precision / context2PerformanceMap.keySet().size();
    }
    
    public double getMacroRecall(Mode mode) {
        double recall = 0.0;
        for (String fileName : context2PerformanceMap.keySet()) {
            recall += getFileRecall(fileName, mode);
        }
        return recall / getRegisteredFiles().size();
    }
    
    public double getMacroFMeasure(Mode mode) {
        double fmeasure = 0.0;
        for (String fileName : context2PerformanceMap.keySet()) {
            fmeasure += getFileFMeasure(fileName, mode);
        }
        return fmeasure / context2PerformanceMap.keySet().size();
    }

    public double getFilePrecision(String fileName, Mode mode) {

        int tp = context2PerformanceMap.get(fileName).getTP(mode);
        int fp = context2PerformanceMap.get(fileName).getFP(mode);
        if (tp + fp > 0) {
            return (double) tp  / (tp + fp);
        }
        else {
            return 0.0;
        }
    }
    
    public double getFileRecall(String fileName, Mode mode) {

        int tp = context2PerformanceMap.get(fileName).getTP(mode);
        int fn = context2PerformanceMap.get(fileName).getFN(mode);

        if (tp + fn > 0) {
            return (double) tp  / (tp + fn);
        }
        else {
            return 0.0;
        }
    }
    
    public double getFileFMeasure(String fileName, Mode mode) {

        double precision = getFilePrecision(fileName, mode);
        double recall = getFileRecall(fileName, mode);

        if (precision + recall > 0) {
            return (2 * precision * recall / (precision + recall));
        }
        else {
            return 0.0;
        }
    }

    public void increaseFileTP(String fileName, Mode mode) {
        FilePerformance filePerformance = context2PerformanceMap.get(fileName);
        filePerformance.setTP( filePerformance.getTP(mode) + 1, mode );
    }
    public void increaseFileFP(String fileName, Mode mode) {
        FilePerformance filePerformance = context2PerformanceMap.get(fileName);
        filePerformance.setFP( filePerformance.getFP(mode) + 1, mode );
    }
    public void increaseFileFN(String fileName, Mode mode) {
        FilePerformance filePerformance = context2PerformanceMap.get(fileName);
        filePerformance.setFN( filePerformance.getFN(mode) + 1, mode );
    }

    public String getMicroPerformanceOverview(Mode mode) {
        DecimalFormat df = new DecimalFormat("0.000");

        StringBuilder sb = new StringBuilder();
        sb.append("Micro Performance Overview"); sb.append(LF);
        sb.append("Mode: "); sb.append(mode); sb.append(LF);
        sb.append("n:    "); sb.append(context2PerformanceMap.size()); sb.append(LF);
        sb.append("P:    "); sb.append(df.format( getMicroPrecision(mode) )); sb.append(LF);
        sb.append("R:    "); sb.append(df.format( getMicroRecall(mode) )); sb.append(LF);
        sb.append("F:    "); sb.append(df.format( getMicroFMeasure(mode) )); sb.append(LF);
        sb.append(LF);

        return sb.toString();
    }
    
    public String getMacroPerformanceOverview(Mode mode) {
        DecimalFormat df = new DecimalFormat("0.000");

        StringBuilder sb = new StringBuilder();
        sb.append("Macro Performance Overview"); sb.append(LF);
        sb.append("Mode: "); sb.append(mode); sb.append(LF);
        sb.append("n:    "); sb.append(context2PerformanceMap.size()); sb.append(LF);
        sb.append("P:    "); sb.append(df.format( getMacroPrecision(mode) )); sb.append(LF);
        sb.append("R:    "); sb.append(df.format( getMacroRecall(mode) )); sb.append(LF);
        sb.append("F:    "); sb.append(df.format( getMacroFMeasure(mode) )); sb.append(LF);
        sb.append(LF);

        return sb.toString();
    }
    
    public String getFilePerformanceOverview(Mode mode) {
        DecimalFormat df = new DecimalFormat("0.000");

        StringBuilder sb = new StringBuilder();
        sb.append("File Performance Overview"); sb.append(LF);
        sb.append("Mode: "); sb.append(mode); sb.append(LF);
        sb.append("File"); sb.append("\t");
        sb.append("P"); sb.append("\t");
        sb.append("R"); sb.append("\t");
        sb.append("F"); sb.append("\t");
        sb.append(LF);

        List<String> files = new ArrayList<String>(getRegisteredFiles());
        Collections.sort(files);
        for (String fileName : files) {
            sb.append(fileName); sb.append("\t");
            sb.append(df.format( getFilePrecision(fileName, mode) )); sb.append("\t");
            sb.append(df.format( getFileRecall(fileName, mode)    )); sb.append("\t");
            sb.append(df.format( getFileFMeasure(fileName, mode)  )); sb.append("\t");
            sb.append(LF);
        }

        return sb.toString();
    }

    protected class FilePerformance {

        private int tpDetection;
        private int fpDetection;
        private int fnDetection;
        
        private int tpCorrection;
        private int fpCorrection;
        private int fnCorrection;

        private int getTP(Mode mode) {
            switch (mode) {
                case detection:
                    return tpDetection;
                case correction:
                    return tpCorrection;
                default:
                    return 0;
            }
        }

        private int getFP(Mode mode) {
            switch (mode) {
                case detection:
                    return fpDetection;
                case correction:
                    return fpCorrection;
                default:
                    return 0;
            }
        }

        private int getFN(Mode mode) {
            switch (mode) {
                case detection:
                    return fnDetection;
                case correction:
                    return fnCorrection;
                default:
                    return 0;
            }
        }

        private void setTP(int value, Mode mode) {
            switch (mode) {
                case detection:
                    tpDetection = value; break;
                case correction:
                    tpCorrection = value; break;
                default:
                    break;
            }
        }

        private void setFP(int value, Mode mode) {
            switch (mode) {
                case detection:
                    fpDetection = value; break;
                case correction:
                    fpCorrection = value; break;
                default:
                    break;
            }
        }

        private void setFN(int value, Mode mode) {
            switch (mode) {
                case detection:
                    fnDetection = value; break;
                case correction:
                    fnCorrection = value; break;
                default:
                    break;
            }
        }
    }
}
