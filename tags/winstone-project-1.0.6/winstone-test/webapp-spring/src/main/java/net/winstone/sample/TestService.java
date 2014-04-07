/**
 * 
 */
package net.winstone.sample;

import java.sql.Connection;
import java.sql.SQLException;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * TestService.
 * 
 * @author JGT
 * 
 */
@Service
@Scope(value = BeanDefinition.SCOPE_SINGLETON)
public class TestService {

	private Logger logger = LoggerFactory.getLogger(TestService.class);

	@Autowired
	private DataSource dataSource;

	@PostConstruct
	protected void initialize() {
		logger.info("Hello From TestService");
		Connection connection = null;
		try {
			connection = dataSource.getConnection();
			logger.info("\"Datasource 'jdbc/myDatasource' connection was open");
		} catch (SQLException e) {
			logger.info("Exception occur", e);
		} finally {
			if (connection != null) {
				try {
					connection.close();
					logger.info("\"Datasource 'jdbc/myDatasource' connection was close");
				} catch (SQLException e) {
					logger.info("\"Datasource 'jdbc/myDatasource' connection was close with exception ", e);
				}
			}
		}
	}
}
