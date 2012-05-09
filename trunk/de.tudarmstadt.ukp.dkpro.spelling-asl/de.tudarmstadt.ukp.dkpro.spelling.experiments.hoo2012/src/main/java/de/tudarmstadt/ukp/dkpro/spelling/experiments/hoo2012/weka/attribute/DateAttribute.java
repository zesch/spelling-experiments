package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.attribute;

import de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka.IWekaAttribute;

/**
 * Wrapper for wekas date attribute
 */
public class DateAttribute implements IWekaAttribute {

	private String name;
	private String format;
	
	public DateAttribute(String name, String format) {
		this.name = name;
		this.format = format;
	}
	
	public DateAttribute(String name) {
		this(name, "yyyy-MM-dd'T'HH:mm:ss");
	}

	public String getFormat() {
		return format;
	}

	@Override
	public String getName() {
		return name;
	}	
}
