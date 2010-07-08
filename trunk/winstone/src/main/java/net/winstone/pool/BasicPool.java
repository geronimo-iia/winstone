package net.winstone.pool;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * BasicPool implement a fake pool, all acquire call are equivalent to a newly created resource. This is usefull to test a mechanism with or
 * without object pool.
 * 
 * @author Jerome Guibert
 * @param <T> Pooled Object.
 */
public class BasicPool<T> implements Pool<T> {
    
    protected final ResourceFactory<T> factory;
    protected final int capacity;
    protected final AtomicInteger active;
    
    /**
     * Build anew instance of BasicPool.
     * 
     * @param factory instance of factory to manage resource life cycle.
     * @param capacity Maximum pool size, if capacity <=0 pool size is infinite.
     */
    public BasicPool(final ResourceFactory<T> factory, final int capacity) {
        super();
        this.factory = factory;
        this.capacity = capacity;
        this.active = capacity > 0 ? new AtomicInteger() : null;
    }
    
    public T acquire() {
        if (capacity > 0) {
            if (active.incrementAndGet() > capacity) {
                active.decrementAndGet();
                return null;
            }
        }
        return factory.create();
    }
    
    public void invalidate(T resource) {
        release(resource);
    }
    
    public void release(T resource) {
        if (resource != null) {
            factory.destroy(resource);
            active.decrementAndGet();
        }
    }
    
    public int getNumActive() {
        return active != null ? active.get() : -1;
    }
    
    public int getCapacity() {
        return capacity;
    }
    
}
