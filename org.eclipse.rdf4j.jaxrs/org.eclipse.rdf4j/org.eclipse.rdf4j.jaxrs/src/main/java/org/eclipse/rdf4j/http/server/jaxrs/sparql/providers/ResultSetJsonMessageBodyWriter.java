package org.eclipse.rdf4j.http.server.jaxrs.sparql.providers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryInterruptedException;
import org.eclipse.rdf4j.query.QueryResultHandler;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.resultio.BasicQueryWriterSettings;
import org.eclipse.rdf4j.query.resultio.TupleQueryResultWriter;
import org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLResultsJSONWriter;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MessageBodyWirter for <code>TupleQueryResult</code>.
 * Resulting output conforms to:
 * http://www.w3.org/TR/2007/NOTE-rdf-sparql-json-res-20070618/
 * 
 */
@Component(service=ResultSetJsonMessageBodyWriter.class, property={"javax.ws.rs=true"})
@Provider
//@JaxrsExtension
@Produces({"application/json", "application/sparql-results+json"})
public class ResultSetJsonMessageBodyWriter implements MessageBodyWriter<TupleQueryResult> {
	private final Logger logger = LoggerFactory.getLogger(ResultSetJsonMessageBodyWriter.class);
	
	protected static final String DEFAULT_JSONP_CALLBACK_PARAMETER = "callback";

	protected static final Pattern JSONP_VALIDATOR = Pattern.compile("^[A-Za-z]\\w+$");
	
	@Context
	Request request;
	
	public ResultSetJsonMessageBodyWriter() {
		System.out.println("Init ResultSetJsonMessageBodyWriter");
	}

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations,
			MediaType mediaType) {
		return TupleQueryResult.class.isAssignableFrom(type);
	}

	@Override
	public long getSize(TupleQueryResult t, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType) {
		return -1;
	}

	@Override
	public void writeTo(TupleQueryResult tupleQueryResult, Class<?> type, Type genericType,
			Annotation[] annotations, MediaType mediaType, MultivaluedMap<String,
			Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
		
		try {
			TupleQueryResultWriter qrWriter = new SPARQLResultsJSONWriter(entityStream);
			/*if (qrWriter.getSupportedSettings().contains(BasicQueryWriterSettings.JSONP_CALLBACK)) {
				String parameter = request.getParameter(DEFAULT_JSONP_CALLBACK_PARAMETER);
	
				if (parameter != null) {
					parameter = parameter.trim();
	
					if (parameter.isEmpty()) {
						parameter = BasicQueryWriterSettings.JSONP_CALLBACK.getDefaultValue();
					}
	
					// check callback function name is a valid javascript function
					// name
					if (!JSONP_VALIDATOR.matcher(parameter).matches()) {
						throw new IOException("Callback function name was invalid");
					}
	
					qrWriter.getWriterConfig().set(BasicQueryWriterSettings.JSONP_CALLBACK, parameter);
				}
			}*/
			
			QueryResults.report(tupleQueryResult, qrWriter);
		} catch (QueryInterruptedException e) {
			logger.error("Query interrupted", e);
			throw new WebApplicationException("Query evaluation took too long", Response.Status.SERVICE_UNAVAILABLE);
		} catch (QueryEvaluationException e) {
			logger.error("Query evaluation error", e);
			throw new WebApplicationException("Query evaluation error: " + e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
		} catch (TupleQueryResultHandlerException e) {
			logger.error("Serialization error", e);
			throw new WebApplicationException("Serialization error: " + e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
		}
	}
}
