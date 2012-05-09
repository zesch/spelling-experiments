package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.cleartk.classifier.Feature;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute.ClassAttribute;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute.IntegerAttribute;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute.NominalAttribute;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.feature.ClassFeature;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.feature.IntegerFeature;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.feature.NominalFeature;

public class ARFFWriterTest {

	@Test
	public void testWriter() throws Exception {
		File target = new File("target/test/arff/test.arff");
		File test = new File("src/test/resources/arff/test.arff");
		
		Set<String> a1 = new HashSet<String>(Arrays.asList(new String[]{"a", "b", "c"}));
		Set<String> out = new HashSet<String>(Arrays.asList(new String[]{"t", "f"}));
		
		ARFFWriter writer = new ARFFWriter(target, new Meta("test", Arrays.asList(new IWekaAttribute[]{
				new NominalAttribute("a1", a1, "XXX"),
				new IntegerAttribute("a2"),
				new ClassAttribute("out", out)
		})));
		
		writer.write(new ArrayList<Feature>(Arrays.asList(new Feature[]{
				new NominalFeature("a1", "a"),
				new IntegerFeature("a2", 10)
		})), new ClassFeature("out", "t"));
		
		writer.write(new ArrayList<Feature>(Arrays.asList(new Feature[]{
				new NominalFeature("a1", "z"),
				new IntegerFeature("a2", 20)
		})), new ClassFeature("out", "f"));
		
		writer.write(new ArrayList<Feature>(Arrays.asList(new Feature[]{
				new IntegerFeature("a2", 20),
				new NominalFeature("a1", "c")
		})), new ClassFeature("out", "t"));
		
		writer.finish();
		
		assertEquals(getContent(test), getContent(target));
	}
	
	protected String getContent(File f) throws IOException {
		return FileUtils.readFileToString(f);
	}

}
