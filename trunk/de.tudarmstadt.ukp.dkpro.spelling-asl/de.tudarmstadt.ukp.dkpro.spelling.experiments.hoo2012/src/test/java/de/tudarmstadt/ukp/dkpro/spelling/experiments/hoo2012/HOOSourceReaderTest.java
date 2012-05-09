package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012;

import static org.uimafit.factory.CollectionReaderFactory.createCollectionReader;
import static org.uimafit.util.JCasUtil.select;
import static org.uimafit.util.JCasUtil.selectByIndex;
import junit.framework.Assert;

import org.apache.uima.collection.CollectionReader;
import org.apache.uima.jcas.JCas;
import org.junit.Test;
import org.uimafit.pipeline.JCasIterable;

import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.io.HOO2012SourceReader;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.type.HOOParagraph;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.type.Part;


public class HOOSourceReaderTest {

	@Test
	public void sourceReaderTest() throws Exception {
		CollectionReader reader = createCollectionReader(
				HOO2012SourceReader.class,
				HOO2012SourceReader.PARAM_PATH, "classpath:/input",
				HOO2012SourceReader.PARAM_PATTERNS, new String[] {
				HOO2012SourceReader.INCLUDE_PREFIX + "*.xml" });
		
		JCas jcas = new JCasIterable(reader).next();
		
		Assert.assertEquals("P1.\nP2.\nP3.\nP4.\n", jcas.getDocumentText());
		
		Assert.assertEquals(2, select(jcas, Part.class).size());
		Assert.assertEquals("1", selectByIndex(jcas, Part.class, 0).getPid());
		Assert.assertEquals("2", selectByIndex(jcas, Part.class, 1).getPid());
		
		Assert.assertEquals("P1.\nP2.\n", selectByIndex(jcas, Part.class, 0).getCoveredText());
		Assert.assertEquals("P3.\nP4.\n", selectByIndex(jcas, Part.class, 1).getCoveredText());
		
		Assert.assertEquals(4, select(jcas, HOOParagraph.class).size());
//		Assert.assertEquals(0, selectByIndex(jcas, Paragraph.class, 0).getCount());
//		Assert.assertEquals(1, selectByIndex(jcas, Paragraph.class, 1).getCount());
//		Assert.assertEquals(2, selectByIndex(jcas, Paragraph.class, 2).getCount());
//		Assert.assertEquals(3, selectByIndex(jcas, Paragraph.class, 3).getCount());
		
		Assert.assertEquals("P1.", selectByIndex(jcas, HOOParagraph.class, 0).getCoveredText());
		Assert.assertEquals("P2.", selectByIndex(jcas, HOOParagraph.class, 1).getCoveredText());
		Assert.assertEquals("P3.", selectByIndex(jcas, HOOParagraph.class, 2).getCoveredText());
		Assert.assertEquals("P4.", selectByIndex(jcas, HOOParagraph.class, 3).getCoveredText());
	}

}
