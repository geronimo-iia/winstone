package net.winstone.util;

/**
 * Interface to declare function.
 * 
 * @author Jerome Guibert
 * @param <R>
 *            Returned type
 * @param <P>
 *            Parameter type
 */
public interface Function<R, P> {
	/**
	 * Apply a process on 'from' parameter.
	 * 
	 * @param from
	 * @return result.
	 */
	public R apply(P from);
}
