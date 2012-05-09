package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.io;

import static de.tudarmstadt.ukp.dkpro.core.api.resources.ResourceUtils.resolveLocation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.util.FSCollectionFactory;
import org.uimafit.util.JCasUtil;

import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.core.api.anomaly.type.SuggestedAction;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.semantics.spelling.type.GoldSpellingAnomaly;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.type.HOOParagraph;
import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.type.Part;

/**
 * Evaluates the result. (2012 version)
 */
public class HOO2012Evaluator extends JCasAnnotator_ImplBase {

	private static final String LF = "\n";

	public static final String PARAM_OUTPUT_PATH = "OutputPath";
	@ConfigurationParameter(name = PARAM_OUTPUT_PATH, mandatory = true)
	private File outputPath;

	public static final String PARAM_EXTRACTION_PATH = "ExtractionPath";
	@ConfigurationParameter(name = PARAM_EXTRACTION_PATH, mandatory = true)
	private File extractionPath;

	public static final String PARAM_GOLD_PATH = "GoldPath";
	@ConfigurationParameter(name = PARAM_GOLD_PATH, mandatory = true)
	private String goldPathString;
	private File goldPath;

	public static final String PARAM_TEAM_ID = "TeamID";
	@ConfigurationParameter(name = PARAM_TEAM_ID, mandatory = true)
	private String teamId;

	public static final String PARAM_RUN_ID = "RunID";
	@ConfigurationParameter(name = PARAM_RUN_ID, mandatory = true)
	private String runId;

	public static final String PARAM_WRITE_EDITS = "WriteEdits";
	@ConfigurationParameter(name = PARAM_WRITE_EDITS, mandatory = true, defaultValue = "true")
	private boolean writeEdits;

	@Override
	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		super.initialize(context);

		try {
			goldPath = new File(resolveLocation(goldPathString, this, context).getFile());
		} catch (IOException e) {
			throw new ResourceInitializationException(e);
		}
		
		outputPath.delete();
		outputPath.mkdirs();
	}

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		DocumentMetaData dmd = DocumentMetaData.get(jcas);
		String baseName = FilenameUtils.removeExtension(dmd.getDocumentTitle());
		String outputFilename = baseName + teamId + runId;

		System.out.println("Evaluating " + dmd.getDocumentTitle());

		if (writeEdits) {
			try {
				extractionPath.delete();
				extractionPath.mkdirs();
				File runExtractionPath = new File(extractionPath, runId);
				runExtractionPath.mkdir();

				writeEdits(jcas, runExtractionPath, outputFilename);

			} catch (Exception e) {
				throw new AnalysisEngineProcessException(e);
			}
		}
	}

	private void batchEvaluate(String gold, String system, String output)
			throws Exception {
		batchEvaluate(gold, system, output, null);
	}

	private void batchEvaluate(String gold, String system, String output,
			String typeArg) throws Exception {

		String[] args;
		if (typeArg != null) {
			args = new String[] { "-o", output, "-t", typeArg, gold, system };
		} else {
			args = new String[] { "-o", output, gold, system };
		}

		String command = "python src/main/resources/eval/evalrun.py "
				+ StringUtils.join(args, " ");

		runProcess(command);
	}

	private void runProcess(String command) throws Exception {
		ProcessBuilder mProBuilder;
		Process mProcess;
		BufferedReader mResultReader;

		System.out.println(command);

		mProBuilder = new ProcessBuilder("/bin/sh", "-c", command);
		mProcess = mProBuilder.start();
		mResultReader = new BufferedReader(new InputStreamReader(
				mProcess.getInputStream()));

		while (!mResultReader.ready()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		while (mResultReader.ready()) {
			String line = mResultReader.readLine();
			if (line == null)
				throw new IOException("Unexpected end of OutputStream.");

			System.out.println(line);
		}
	}

	private void writeEdits(JCas jcas, File path, String file)
			throws IOException, AnalysisEngineProcessException, CASException {

		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(
				path, file) + ".xml"));
		writer.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
		writer.write(LF);
		writer.write("<edits file=\""+file.substring(0,4)+"\">");
		writer.write(LF);
		int i = 1;
		
		for (Part part: JCasUtil.select(jcas, Part.class)) {
			for (HOOParagraph para: JCasUtil.selectCovered(HOOParagraph.class, part)) {
				for (SpellingAnomaly anomaly: JCasUtil.selectCovered(SpellingAnomaly.class, para)) {
					if (!(anomaly instanceof GoldSpellingAnomaly)) {
						String original = StringEscapeUtils.escapeXml(anomaly
								.getCoveredText());
						if (original.equals(""))
							original = "<empty/>";
						
						int offset = part.getBegin() + para.getCount();
						int start = anomaly.getBegin() - offset;
						int end = anomaly.getEnd() - offset;
						
						writer.write("  <edit");
						writer.write(" type=\"" + anomaly.getCategory() + "\"");
						writer.write(" file=\"" + file.substring(0,4) + "\"");
						writer.write(" part=\"" + part.getPid() + "\"");
						writer.write(" index=\"" + i + "\"");
						writer.write(" start=\"" + start + "\""); // relative
																					// to
																					// part
																					// begin
						writer.write(" end=\"" + end + "\">"); // relative to part begin
						writer.write(LF);
						writer.write("    <original>" + original + "</original>");
						writer.write(LF);
						writer.write("    <corrections>");
						writer.write(LF);
						// TODO: Only one correction needed
						for (String corrected : getCorrections(anomaly)) {
							writer.write("      <correction>" + corrected
									+ "</correction>");
							writer.write(LF);
						}
						writer.write("    </corrections>");
						writer.write(LF);
						writer.write("  </edit>");
						writer.write(LF);

						i++;
					}
				}
			}
		}
		writer.write("</edits>");
		writer.flush();
	}

	private HashSet<String> getCorrections(SpellingAnomaly error) {
		HashSet<String> corrections = new HashSet<String>();
		
		if (error.getSuggestions() != null && error.getSuggestions().size() > 0) {
			for (FeatureStructure fs : FSCollectionFactory.create(error
					.getSuggestions())) {
				SuggestedAction action = (SuggestedAction) fs;

				String s = StringEscapeUtils.escapeXml(action.getReplacement());
				// if (s == "") s = "<empty/>";
				corrections.add(s);
			}
		} else {
			corrections.add("<empty/>");
		}
		return corrections;
	}

	@Override
	public void collectionProcessComplete()
			throws AnalysisEngineProcessException {
		super.collectionProcessComplete();

		try {
			File path = new File(extractionPath, runId);
			if (path.isDirectory()) {
				// Evaluate all type classes
				batchEvaluate(goldPath.getAbsolutePath(),
						path.getAbsolutePath(), outputPath + "/results"
								+ teamId + "_" + runId + "_all.xml");
				
				batchEvaluate(goldPath.getAbsolutePath(),
						path.getAbsolutePath(), outputPath + "/results"
								+ teamId + "_" + runId + "_RT.xml", "\"RT\"");
				
				batchEvaluate(goldPath.getAbsolutePath(),
						path.getAbsolutePath(), outputPath + "/results"
								+ teamId + "_" + runId + "_RD.xml", "\"RD\"");

				// Evaluate just preposition type classes
				batchEvaluate(goldPath.getAbsolutePath(),
						path.getAbsolutePath(), outputPath + "/results"
								+ teamId + "_" + runId + "_prep.xml", "\"RT,MT,UT\"");
				
				// Evaluate just determiner type classes
				batchEvaluate(goldPath.getAbsolutePath(),
						path.getAbsolutePath(), outputPath + "/results"
								+ teamId + "_" + runId + "_det.xml", "\"RD,MD,UD\"");
			}
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}
	}
}