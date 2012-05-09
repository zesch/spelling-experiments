package de.tudarmstadt.ukp.dkpro.spelling.experiments.hoo2012.weka;

/**
 * All Weka attributes have at least a name in common
 * The other values can be found in the implementation of
 * this interface.
 * 
 * This interface is used as a wrapper to Weka's Attribute
 * class. This brings us more type safety.
 */
public interface IWekaAttribute {

	/**
	 * Returns the name of the attribute
	 * @return
	 */
	public String getName();
}
