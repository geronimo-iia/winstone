package net.winstone.log;

import net.winstone.log.Logger;
import net.winstone.log.LoggerFactory;
import junit.framework.TestCase;

public class LoggerTestCase extends TestCase {
    
    public void testLoading() {
        assertNotNull(LoggerFactory.getLogger(getClass()));
    }
    
    public void testInfoLogging() {
        Logger logger = LoggerFactory.getLogger(getClass());
        assertNotNull(logger);
        logger.info("this is a fake information message");
    }
    
    public void testErrorLogging() {
        Logger logger = LoggerFactory.getLogger(getClass());
        assertNotNull(logger);
        logger.error("this is a fake error message", new Exception("Fake message exception"));
    }
    
}
