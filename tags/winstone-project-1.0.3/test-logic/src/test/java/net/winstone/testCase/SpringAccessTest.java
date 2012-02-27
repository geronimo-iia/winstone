/**
 * 
 */
package net.winstone.testCase;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import net.winstone.testCase.load.Launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.HttpException;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * SpringAccessTest. 
 *
 * @author JGT
 *
 */
public class SpringAccessTest extends TestCase{
	protected Logger logger = LoggerFactory.getLogger(getClass());
	
	/**
	 * Test the keep alive case
	 */
	public void testJndiAccessServlet() throws IOException, InterruptedException, SAXException {
		// Initialise container
		final Map<String, String> args = new HashMap<String, String>();
		args.put("webroot","../test-webapp-spring/target/test-webapp-spring");
		args.put("prefix", "/examples");
		args.put("httpPort", "10009");
		args.put("ajp13Port", "-1");
		args.put("controlPort", "-1");
		args.put("debug", "8");
		args.put("logThrowingLineNo", "true");
		args.put("useJNDI", "true");

		args.put("useJNDI", "true");
		args.put("jndi.resource.jdbc/testDatasource", "javax.sql.DataSource");
		args.put("jndi.param.jdbc/testDatasource.url", "jdbc:h2:~/test");

		//
		final Launcher winstone = new Launcher(args);
		winstone.launch();
		// Check for a simple connection
		final WebConversation wc = new WebConversation();
		final WebRequest wreq = new GetMethodWebRequest("http://localhost:10009/examples/index.html");
		try {
			final WebResponse wresp1 = wc.getResponse(wreq);
			logger.info("ResponseCode: " + wresp1.getResponseCode());
		} catch (final HttpException exception) {
			Assert.fail("Datasource must be found. " + exception.getMessage());
		}

		winstone.shutdown();
		Thread.sleep(500);
	}
}

