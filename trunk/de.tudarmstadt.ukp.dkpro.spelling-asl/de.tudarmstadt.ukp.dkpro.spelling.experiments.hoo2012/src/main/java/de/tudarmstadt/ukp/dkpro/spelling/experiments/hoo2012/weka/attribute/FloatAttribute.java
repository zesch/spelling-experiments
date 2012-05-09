package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute;

import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.INumericAttribute;

/**
 * A float attribute
 */
public class FloatAttribute implements INumericAttribute {

	private String name;
	
	public FloatAttribute(String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
	}
}