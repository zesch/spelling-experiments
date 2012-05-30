package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012;

import static org.uimafit.factory.AnalysisEngineFactory.createPrimitiveDescription;
import static org.uimafit.factory.CollectionReaderFactory.createCollectionReader;
import static org.uimafit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import junit.framework.Assert;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReader;
import org.junit.Ignore;
import org.junit.Test;
import org.uimafit.pipeline.SimplePipeline;

import de.tudarmstadt.ukp.dkpro.core.tokit.BreakIteratorSegmenter;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.io.HOO2012Evaluator;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.io.HOO2012SourceReader;

public class EvaluatorTest {

	// Ignoring test at it fails on server.
	@Ignore
	@Test
	public void evaluatorTest() throws Exception {
		CollectionReader reader = createCollectionReader(HOO2012SourceReader.class,
				createTypeSystemDescription(),
				HOO2012SourceReader.PARAM_PATH, "classpath:/hooInput/Raw",
				HOO2012SourceReader.PARAM_PATTERNS, new String[] {
				HOO2012SourceReader.INCLUDE_PREFIX + "*.xml" }
		);
		
		AnalysisEngineDescription eval = createPrimitiveDescription(HOO2012Evaluator.class,
				HOO2012Evaluator.PARAM_OUTPUT_PATH, "target/test/evaluator/out",
				HOO2012Evaluator.PARAM_EXTRACTION_PATH, "target/test/evaluator/extract",
				HOO2012Evaluator.PARAM_GOLD_PATH, "src/test/resources/hooInput/Gold",
				HOO2012Evaluator.PARAM_TEAM_ID, "UD",
				HOO2012Evaluator.PARAM_RUN_ID, "0",
				HOO2012Evaluator.PARAM_WRITE_EDITS, true
		);
		
		SimplePipeline.runPipeline(
				reader,
				createPrimitiveDescription(BreakIteratorSegmenter.class),
				createPrimitiveDescription(TestAnnotator.class),
				eval
		);
		
		Assert.assertEquals(
				readFile("target/test/evaluator/extract/0/0002UD0.xml"),
				readFile("src/test/resources/hooInput/Gold/0002GE.xml")
		);
	}
	
	protected String readFile(String filePath) throws IOException {
		String s = "";
		BufferedReader r = new BufferedReader(new FileReader(new File(filePath)));
		String line;
		while((line = r.readLine()) != null) {
			s += line+"\n";
		}
		
		return s;
	}
}
