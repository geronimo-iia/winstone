package net.winstone.testApplication.servlets;

import java.io.IOException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

/**
 * Check DataSource
 * 
 * @author Jerome Guibert
 */
public class JndiDataSourceServlet extends HttpServlet {

	private static final long serialVersionUID = -2418327420316254139L;
	public static final String JNDI_DATASOURCE = "jdbc/myDatasource";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			InitialContext initialContext = new InitialContext();
			Context envCtx = (Context) initialContext.lookup("java:comp/env");
			DataSource dataSource = (DataSource) envCtx.lookup(JNDI_DATASOURCE);
			if (dataSource == null) {
				throw new ServletException("Datasource 'jdbc/myDatasource' should be found");
			}
		} catch (NamingException ex) {
			throw new ServletException(ex);
		}
	}
}
