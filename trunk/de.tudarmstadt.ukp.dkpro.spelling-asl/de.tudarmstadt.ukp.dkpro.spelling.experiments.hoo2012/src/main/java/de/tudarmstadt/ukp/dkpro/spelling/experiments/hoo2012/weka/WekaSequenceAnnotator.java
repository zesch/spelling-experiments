package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka;

import java.io.IOException;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.classifier.Classifier;
import org.cleartk.classifier.DataWriter;
import org.cleartk.util.CleartkInitializationException;
import org.cleartk.util.ReflectionUtil;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.factory.ConfigurationParameterFactory;
import org.uimafit.factory.initializable.Initializable;
import org.uimafit.factory.initializable.InitializableFactory;

import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.feature.ClassFeature;

public abstract class WekaSequenceAnnotator extends JCasAnnotator_ImplBase
		implements Initializable {

	public static final String PARAM_CLASSIFIER_FACTORY_CLASS_NAME = ConfigurationParameterFactory
			.createConfigurationParameterName(WekaSequenceAnnotator.class,
					"classifierFactoryClassName");

	@ConfigurationParameter(mandatory = false, description = "provides the full name of the ClassifierFactory class to be used.", defaultValue = "org.cleartk.classifier.jar.JarClassifierFactory")
	private String classifierFactoryClassName;

	public static final String PARAM_DATA_WRITER_FACTORY_CLASS_NAME = ConfigurationParameterFactory
			.createConfigurationParameterName(WekaSequenceAnnotator.class,
					"dataWriterFactoryClassName");

	@ConfigurationParameter(mandatory = false, description = "provides the full name of the DataWriterFactory class to be used.")
	private String dataWriterFactoryClassName;

	public static final String PARAM_IS_TRAINING = ConfigurationParameterFactory
			.createConfigurationParameterName(WekaSequenceAnnotator.class,
					"isTraining");

	@ConfigurationParameter(mandatory = false, description = "determines whether this annotator is writing training data or using a classifier to annotate. Normally inferred automatically based on whether or not a DataWriterFactory class has been set.")
	private Boolean isTraining;

	private boolean primitiveIsTraining;

	protected WekaClassifier classifier;

	protected ARFFWriter dataWriter;

	@Override
	public void initialize(UimaContext context)
			throws ResourceInitializationException {
		super.initialize(context);
		
		if (dataWriterFactoryClassName == null
				&& classifierFactoryClassName == null) {
			CleartkInitializationException.neitherParameterSet(
					PARAM_DATA_WRITER_FACTORY_CLASS_NAME,
					dataWriterFactoryClassName,
					PARAM_CLASSIFIER_FACTORY_CLASS_NAME,
					classifierFactoryClassName);
		}

		// determine whether we start out as training or predicting
		if (this.isTraining == null) {
			this.primitiveIsTraining = dataWriterFactoryClassName != null;
		} else {
			this.primitiveIsTraining = this.isTraining;
		}

		if (this.isTraining()) {
			// create the factory and instantiate the data writer
			WekaDataWriterFactory factory = InitializableFactory.create(context,
					dataWriterFactoryClassName, WekaDataWriterFactory.class);
			DataWriter<ClassFeature> untypedDataWriter;
			try {
				untypedDataWriter = factory.createDataWriter();
			} catch (IOException e) {
				throw new ResourceInitializationException(e);
			}

			InitializableFactory.initialize(untypedDataWriter, context);
			this.dataWriter = ReflectionUtil.uncheckedCast(untypedDataWriter);
		} else {
			// create the factory and instantiate the classifier
			WekaClassifierFactory factory = InitializableFactory.create(context,
					classifierFactoryClassName, WekaClassifierFactory.class);
			Classifier<String> untypedClassifier;
			try {
				untypedClassifier = factory.createClassifier();
			} catch (IOException e) {
				throw new ResourceInitializationException(e);
			}

			this.classifier = ReflectionUtil.uncheckedCast(untypedClassifier);
//			ReflectionUtil.checkTypeParameterIsAssignable(
//					CleartkAnnotator.class, "OUTCOME_TYPE", this,
//					Classifier.class, "OUTCOME_TYPE", this.classifier);
			InitializableFactory.initialize(untypedClassifier, context);
		}
	}
	
	@Override
	public void collectionProcessComplete()
			throws AnalysisEngineProcessException {
		super.collectionProcessComplete();
		if (isTraining()) {
			dataWriter.finish();
		}
	}

	protected boolean isTraining() {
		return this.primitiveIsTraining;
	}
}
