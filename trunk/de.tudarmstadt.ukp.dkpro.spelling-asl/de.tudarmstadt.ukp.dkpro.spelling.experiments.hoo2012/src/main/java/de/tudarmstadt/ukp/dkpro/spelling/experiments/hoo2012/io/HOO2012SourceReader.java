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
package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.io;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.tudarmstadt.ukp.dkpro.core.api.io.ResourceCollectionReaderBase;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.type.HOOParagraph;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.type.Part;

public class HOO2012SourceReader extends ResourceCollectionReaderBase {

	@Override
	public void getNext(CAS aCAS) throws IOException, CollectionException {
		Resource res = nextFile();
		
		initCas(aCAS, res);
		try {
			extractFromXml(aCAS.getJCas(), res.getInputStream());
		} catch (CASException e) {
			throw new CollectionException(e);
		} catch (ParserConfigurationException e) {
			throw new IOException(e);
		} catch (SAXException e) {
			throw new IOException(e);
		}
	}

	public void extractFromXml(JCas aCas, InputStream inputStream) throws ParserConfigurationException, SAXException, IOException {
		// Open document
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(inputStream);
		doc.normalizeDocument();
		
		// Gets parts
		NodeList partList = doc.getElementsByTagName("PART");
		
		int[] parts = new int[partList.getLength()];
		
		// Extract text
		String text = "";
		for (int i = 0; i < partList.getLength(); i++) {
			NodeList pList = ((Element) partList.item(i)).getElementsByTagName("P");
			
			for (int j = 0; j < pList.getLength(); j++) {
				try {
					String line = pList.item(j).getChildNodes().item(0).getNodeValue();
					text += line + "\n";
				} catch (NullPointerException e) {
					// Do nothing if <P> tag is empty
				}
				
			}
			
			parts[i] = text.length();
		}

		// Set document text
		aCas.setDocumentLanguage("en");
		aCas.setDocumentText(text);
		
		// Add part annotation
		int last = 0;
		int offsetPara = 0;
		for (int i = 0; i < parts.length; i++) {
			Part p = new Part(aCas, last, parts[i]);
			p.setPid(((Element)partList.item(i)).getAttribute("id"));
			p.setCount(i);
			p.addToIndexes();
			
			last = parts[i];
			
			// Add paragraph annotation
			String[] lines = p.getCoveredText().split("\n");
			for (int j = 0; j < lines.length; j++) {
				String line = lines[j];
				int end = offsetPara + line.length();
				
				HOOParagraph para = new HOOParagraph(aCas, offsetPara, end);
				para.setCount(j);
				para.addToIndexes();
				
				offsetPara = end + 1;
			}
		}
	}
}
