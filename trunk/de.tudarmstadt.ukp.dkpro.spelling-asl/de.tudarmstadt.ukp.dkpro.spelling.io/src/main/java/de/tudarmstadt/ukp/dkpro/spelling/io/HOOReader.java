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

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jaxen.XPath;
import org.jaxen.dom4j.Dom4jXPath;
import org.uimafit.descriptor.ConfigurationParameter;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction;
import de.tudarmstadt.ukp.dkpro.core.api.io.JCasResourceCollectionReader_ImplBase;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.type.GoldSpellingAnomaly;

/**
 * 
 * Reads the plain text files and annotates gold standard edits from the
 * corresponding xml file.
 * 
 * Plain text files are named with 4 digits and the suffix ".txt" (e.g.
 * "0001.txt"), while the corresponding gold edit files have the same id with a
 * final "G" and a ".xml" suffix (e.g. "0001G.xml").
 * 
 * @author Irina Smidt, Torsten Zesch
 * 
 */
public class HOOReader
    extends JCasResourceCollectionReader_ImplBase
{
    private static final String ENCODING = "UTF-8";

    public static final String PARAM_EDITS_PATH = "EditInputPath";
    @ConfigurationParameter(name = PARAM_EDITS_PATH, mandatory = true)
    private File editsPath;

    public static final String PARAM_WRITE_EDITS = "WriteEdits";
    @ConfigurationParameter(name = PARAM_WRITE_EDITS, mandatory = true, defaultValue = "true")
    private boolean writeEdits;

    private File[] editFiles;

    @Override
    public void initialize(UimaContext aContext)
        throws ResourceInitializationException
    {
        super.initialize(aContext);

        // get all the edit files
        editFiles = editsPath.listFiles(new FileFilter()
        {
            @Override
            public boolean accept(File pathname)
            {
                if (pathname.isFile() && pathname.getName().endsWith(".xml")) {
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void getNext(JCas jcas)
        throws IOException, CollectionException
    {
        Resource res = nextFile();
        initCas(jcas, res);

        // set the document text
        InputStream is = null;
        try {
            is = new BufferedInputStream(res.getInputStream());
            jcas.setDocumentText(IOUtils.toString(is, ENCODING));
        }
        finally {
            closeQuietly(is);
        }

        // get the matching edits file
        DocumentMetaData metaData = DocumentMetaData.get(jcas);
        String title = metaData.getDocumentTitle();
        String id = title.substring(0, 4);

        File goldFile = getMatchingGoldFile(id);

        if (goldFile == null) {
            throw new IOException("Could not get matching gold xml file for text file: " + title);
        }

        System.out.println("------------------ " + res.getPath() + "------------------");
        System.out.println("Matching Gold file: " + goldFile.getPath());

        try {
            SAXReader reader = new SAXReader();
            Document document;

            document = reader.read(new BufferedInputStream(new FileInputStream(goldFile)));
            Element root = document.getRootElement();

            String editXPath = "//edit";
            String correctionXPath = ".//correction";
            final XPath editXP = new Dom4jXPath(editXPath);
            final XPath correctionXP = new Dom4jXPath(correctionXPath);

            for (Object editElement : editXP.selectNodes(root)) {
                if (editElement instanceof Element) {
                    Element editNode = (Element) editElement;
                    String type = editNode.attributeValue("type");
                    int start = Integer.parseInt(editNode.attributeValue("start"));
                    int end = Integer.parseInt(editNode.attributeValue("end"));

                    // one edit: go through all corrections of this edit and
                    // save them to a list
                    List<String> correctionsList = new ArrayList<String>();
                    for (Object correctionElement : correctionXP.selectNodes(editElement)) {
                        if (correctionElement instanceof Element) {
                            Element correctionNode = (Element) correctionElement;
                            correctionsList.add(correctionNode.getText());
                        }
                    }

                    // as the number of corrections is not known in advance, the
                    // corrections were saved into an ArrayList,
                    // but they have to be converted into a StringArray
                    FSArray corrections = new FSArray(jcas, correctionsList.size());
                    for (int i = 0; i < correctionsList.size(); i++) {
                        SuggestedAction action = new SuggestedAction(jcas);
                        action.setReplacement(correctionsList.get(i));
                        action.setCertainty(1.0f);
                        
                        corrections.set(i, action);
                    }

                    if (writeEdits) {
                        GoldSpellingAnomaly editAnno = new GoldSpellingAnomaly(jcas, start, end);
                        editAnno.setCategory(type);
                        editAnno.setSuggestions(corrections);
                        editAnno.addToIndexes();
                    }
                }
            }
        }
        catch (Exception e) {
            throw new CollectionException(e);
        }
    }

    private File getMatchingGoldFile(String id)
    {
        if (editFiles == null) {
            return null;
        }
        
        for (File file : editFiles) {
            if (file.getName().substring(0, 4).equals(id)) {
                return file;
            }
        }

        return null;
    }
}
