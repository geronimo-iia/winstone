package net.winstone.testApplication.servlets;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

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
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		try {
			final InitialContext initialContext = new InitialContext();
			final Context envCtx = (Context) initialContext.lookup("java:comp/env");
			final DataSource dataSource = (DataSource) envCtx.lookup(JndiDataSourceServlet.JNDI_DATASOURCE);
			if (dataSource == null) {
				throw new ServletException("Datasource 'jdbc/myDatasource' should be found");
			}
			log("\"Datasource 'jdbc/myDatasource' was found");
			
			Connection connection = null;
			try {
				connection = dataSource.getConnection();
				log("\"Datasource 'jdbc/myDatasource' connection was open");
			} catch (SQLException e) {
			}finally {
				if (connection!=null) {
					try {
						connection.close();
						log("\"Datasource 'jdbc/myDatasource' connection was close");
					} catch (SQLException e) {
					}
				}
			}
			
		} catch (final NamingException ex) {
			throw new ServletException(ex);
		}
	}
}
