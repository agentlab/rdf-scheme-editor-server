package org.eclipse.rdf4j.http.server.repository.statements;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.rdf4j.http.server.repository.RepositoryInterceptor;
import org.eclipse.rdf4j.repository.Repository;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* , HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT + "=(" +
 * HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=filtersample)"
 * 
 */
@SuppressWarnings("serial")
@Component(service = Servlet.class, property = {
		HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN + "=/other/*"
})
public class StatementsServlet extends HttpServlet {
	private Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		doLog("Init with config [" + config + "]");
		super.init(config);
	}

	@Override
	public void destroy() {
		doLog("Destroyed servlet");
		super.destroy();
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse resp) throws ServletException, IOException {
		Repository repository = RepositoryInterceptor.getRepository(request);
		
		resp.setContentType("text/plain");
		resp.getWriter().write("Hello World FROM MyServlet!!! 2222222");
	}

	private void doLog(String message) {
		System.out.println("## [" + this.getClass().getCanonicalName() + "] " + message);
	}
}
