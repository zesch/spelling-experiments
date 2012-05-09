package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka;

import org.cleartk.classifier.Feature;

/**
 * We normally classify a complete features vector.
 * This is one value of the vector
 *
 * @param <T> The features type
 */
public class WekaFeature<T> extends Feature {
	
	public WekaFeature(String name, T value) {
		super(name, value);
	}
	
	public T getTypedValue() {
		return (T) getValue();
	}
}
