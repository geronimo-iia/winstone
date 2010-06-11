package winstone;

/**
 * Common interface for component life cycle methods.
 * 
 * @author Jerome Guibert
 */
public interface LifeCycle {
    /**
     * Component instance initialization.
     */
    public void initialize();
    
    /**
     * Component instance disposal.
     */
    public void dispose();
}
