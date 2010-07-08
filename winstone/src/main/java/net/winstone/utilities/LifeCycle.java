package net.winstone.utilities;

/**
 * Utility to define method name for initialize/destroy instance.
 * 
 * @author Jerome Guibert
 */
public interface LifeCycle {
    
    public void initialize();
    
    public void destroy();
}
