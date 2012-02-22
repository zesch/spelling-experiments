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
package de.tudarmstadt.ukp.dkpro.spelling.experiments.data.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.uimafit.component.JCasCollectionReader_ImplBase;
import org.uimafit.descriptor.ConfigurationParameter;

import de.tudarmstadt.ukp.dkpro.spelling.experiments.core.DatasetItem;

public class Csv2DatasetReader
    extends JCasCollectionReader_ImplBase
{

    private static final String DEFAULT_LANGUAGE = "en";

    public static final String PARAM_INPUT_FILE = "InputFile";
    @ConfigurationParameter(name = PARAM_INPUT_FILE, mandatory = true)
    private File inputFile;

    public static final String PARAM_LANGUAGE = "Language";
    @ConfigurationParameter(name = PARAM_LANGUAGE, mandatory = false, defaultValue = DEFAULT_LANGUAGE)
    private String language;

    public static final String PARAM_ENCODING = "Encoding";
    @ConfigurationParameter(name = PARAM_ENCODING, mandatory = false, defaultValue = "UTF-8")
    private String encoding;

    public static final String PARAM_SEPARATOR = "Separator";
    @ConfigurationParameter(name = PARAM_SEPARATOR, mandatory = true)
    private String separator;

    private int currentIndex;

    private BufferedReader reader;
    private BufferedWriter writer;

    private String nextLine;
    
    @Override
    public void initialize(UimaContext aContext)
        throws ResourceInitializationException
    {

        try {
            reader = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(inputFile),
                            encoding
                    )
            );
            writer = new BufferedWriter(new FileWriter(inputFile + "_converted.txt"));
        }
        catch (Exception e) {
            throw new ResourceInitializationException(e);
        }

        currentIndex = 0;
    }

    public boolean hasNext()
        throws IOException 
    {
        nextLine = reader.readLine();
        if (nextLine == null) {
            return false;
        }
        else {
            return true;
        }
    }

    @Override
    public void getNext(JCas jcas) throws IOException, CollectionException {

        // set language if it was explicitly specified as a configuration parameter
        if (language != null) {
            jcas.setDocumentLanguage(language);
        }

        String[] nextItem = nextLine.split(separator);
        
        if (nextItem.length != 5) {
            throw new IOException("Wrong file format.");
        }

        String pageId  = nextItem[0];
        String revId   = nextItem[1];
        String correct = nextItem[2];
        String error   = nextItem[3];
        String context = nextItem[4];
    
        DatasetItem item = new DatasetItem(
                error,
                correct,
                0,
                context,
                Integer.parseInt(pageId),
                Integer.parseInt(revId)
            );
            
            item = DatasetChecker.validateItem(item);
            writeItem(writer, item);
    }    
    
    private void writeItem(BufferedWriter writer, DatasetItem item) throws IOException {
        writer.write(item.toString());
        writer.newLine();
        writer.flush();
    }    
    
    @Override
    public void close()
        throws IOException
    {
    }

    public Progress[] getProgress()
    {
        return new Progress[] { new ProgressImpl(currentIndex, currentIndex, Progress.ENTITIES) };
    }
}
