package ru.agentlab.rdf4j.jaxrs.repository.statements;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;
import static javax.ws.rs.core.Response.Status.UNSUPPORTED_MEDIA_TYPE;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.protocol.error.ErrorInfo;
import org.eclipse.rdf4j.http.protocol.error.ErrorType;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryInterruptedException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.query.UpdateExecutionException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.agentlab.rdf4j.jaxrs.HTTPException;
import ru.agentlab.rdf4j.jaxrs.repository.ProtocolUtils;
import ru.agentlab.rdf4j.jaxrs.util.HttpServerUtil;
import ru.agentlab.rdf4j.repository.RepositoryManagerComponent;

@Path("/rdf4j-server")
@Component(service=StatementsController.class, property={"osgi.jaxrs.resource=true"})
public class StatementsController {
	private static final Logger logger = LoggerFactory.getLogger(StatementsController.class);

	@Reference
	private RepositoryManagerComponent repositoryManager;

	@Activate
	public void activate() {
		logger.info("Activate " + this.getClass().getSimpleName());
	}

	@Deactivate
	public void deactivate() {
		logger.info("Deactivate " + this.getClass().getSimpleName());
	}

	@GET
	@Path("/repositories/{repId}/statements")
	public String getStatements(@PathParam("repId") String repId,
			@QueryParam("subj") String subjStr,
			@QueryParam("pred") String predStr,
			@QueryParam("obj") String objStr,
			@QueryParam("infer") @DefaultValue("true") boolean useInferencing) {
		logger.info("GET statements");
		Repository repository = repositoryManager.getRepository(repId);
		ValueFactory vf = repository.getValueFactory();

		Resource subj = parseResourceParam("subj", subjStr, vf);
		IRI pred = parseURIParam("pred", predStr, vf);
		Value obj = parseValueParam("obj", objStr, vf);
		//Resource[] contexts = ProtocolUtil.parseContextParam(request, CONTEXT_PARAM_NAME, vf);

		OutputStream out = new ByteArrayOutputStream();

		try {
			RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE, out);
			RepositoryConnection conn1 = repository.getConnection();
			conn1.exportStatements(subj, pred, obj, useInferencing, rdfWriter);
		} catch (RDFHandlerException e) {
			throw new WebApplicationException("Serialization error: " + e.getMessage(), e,
					INTERNAL_SERVER_ERROR);
		} catch (RepositoryException e) {
			throw new WebApplicationException("Repository error: " + e.getMessage(), e,
					INTERNAL_SERVER_ERROR);
		}
		return out.toString();
	}

	@PUT
	@Path("/repositories/{repId}/statements")
	public void replaceStatements(@Context HttpHeaders headers,
			@PathParam("repId") String repId,
			@QueryParam("context") String context,
			@QueryParam("baseURI") String baseUriStr,
			@QueryParam("preserveNodeId") @DefaultValue("false") boolean preserveNodeIds,
			InputStream in) throws RepositoryException, IOException, HTTPException {
		Repository repository = repositoryManager.getRepository(repId);
		if(repository == null)
			throw new WebApplicationException("Cannot find repository '" + repId, NOT_FOUND);

		String mimeType = HttpServerUtil.getMIMEType(headers.getHeaderString(HttpHeaders.CONTENT_TYPE));
		getAddDataResult(repository, headers, baseUriStr, preserveNodeIds, mimeType, in, true);
	}

	@POST
	@Path("/repositories/{repId}/statements")
	//@Consumes ({"application/x-www-form-urlencoded", "text/turtle"})
	public void addStatements(@Context HttpHeaders headers,
			@PathParam("repId") String repId,
			@QueryParam("context") String context,
			@QueryParam("baseURI") String baseUriStr,
			@QueryParam("infer") @DefaultValue("true") boolean includeInferred,
			@QueryParam("timeout") int maxQueryTime,
			@QueryParam("queryLn") String queryLnStr,
			@QueryParam("preserveNodeId") @DefaultValue("false") boolean preserveNodeIds,
			InputStream inStream) throws RepositoryException, IOException, HTTPException {
		logger.info("POST data to repository");
		Repository repository = repositoryManager.getRepository(repId);
		if(repository == null)
			throw new WebApplicationException("Cannot find repository '" + repId, NOT_FOUND);

		String mimeType = HttpServerUtil.getMIMEType(headers.getHeaderString(HttpHeaders.CONTENT_TYPE));

		//if (queryUpdate == null && formUpdate != null)
		//	queryUpdate = formUpdate;

		//int qryCode = 0;
		//if (logger.isInfoEnabled() || logger.isDebugEnabled()) {
		//	qryCode = String.valueOf(formUpdate).hashCode();
		//}
		//logger.info("query {} = {}", qryCode, formUpdate);
		logger.info("repId={}, queryLn={}, baseURI={}, infer={}, timeout={}, distinct={}, limit={}, offset={}", repId, queryLnStr, includeInferred, maxQueryTime);

		if (Protocol.TXN_MIME_TYPE.equals(mimeType)) {
			logger.info("POST transaction to repository");
			//getTransactionResultResult(repository, request, response);
		} else if (Protocol.SPARQL_UPDATE_MIME_TYPE.equals(mimeType) /*|| queryUpdate != null*/) {
			logger.info("POST SPARQL update request to repository");
			String queryUpdate = null;
			// The query should be the entire body
			try {
				queryUpdate = IOUtils.toString(new BufferedReader(new InputStreamReader(inStream)));
			} catch (IOException e) {
				throw new WebApplicationException("Error reading request message body", e, BAD_REQUEST);
			}
			if (queryUpdate.isEmpty())
				queryUpdate = null;
			getSparqlUpdateResult(repository, headers, baseUriStr, preserveNodeIds, mimeType, queryUpdate, queryLnStr, includeInferred, maxQueryTime);
		} else {
			logger.info("POST data to repository");
			getAddDataResult(repository, headers, baseUriStr, preserveNodeIds, mimeType, inStream, false);
		}
	}

	@POST
	@Path("/repositories/{repId}/statements")
	@Consumes ({"application/x-www-form-urlencoded"})
	public void addStatements(@Context HttpHeaders headers,
			@PathParam("repId") String repId,
			@QueryParam("context") String context,
			@QueryParam("baseURI") String baseUriStr,
			@QueryParam("infer") @DefaultValue("true") boolean includeInferred,
			@QueryParam("timeout") int maxQueryTime,
			@QueryParam("queryLn") String queryLnStr,
			@QueryParam("preserveNodeId") @DefaultValue("false") boolean preserveNodeIds,
			@FormParam("update") String formUpdate,
			@QueryParam("update") String queryUpdate) throws RepositoryException, IOException, HTTPException {
		logger.info("POST data to repository");
		Repository repository = repositoryManager.getRepository(repId);
		if(repository == null)
			throw new WebApplicationException("Cannot find repository '" + repId, NOT_FOUND);

		String mimeType = HttpServerUtil.getMIMEType(headers.getHeaderString(HttpHeaders.CONTENT_TYPE));

		if (queryUpdate == null && formUpdate != null)
			queryUpdate = formUpdate;

		int qryCode = 0;
		if (logger.isInfoEnabled() || logger.isDebugEnabled()) {
			qryCode = String.valueOf(formUpdate).hashCode();
		}
		logger.info("query {} = {}", qryCode, formUpdate);
		logger.info("repId={}, queryLn={}, baseURI={}, infer={}, timeout={}, distinct={}, limit={}, offset={}", repId, queryLnStr, includeInferred, maxQueryTime);

		if (Protocol.TXN_MIME_TYPE.equals(mimeType)) {
			logger.info("POST transaction to repository");
			//getTransactionResultResult(repository, request, response);
		} else if (Protocol.SPARQL_UPDATE_MIME_TYPE.equals(mimeType) || queryUpdate != null) {
			logger.info("POST SPARQL update request to repository");
			getSparqlUpdateResult(repository, headers, baseUriStr, preserveNodeIds, mimeType, queryUpdate, queryLnStr, includeInferred, maxQueryTime);
		}
	}

	private void getSparqlUpdateResult(Repository repository, HttpHeaders headers, String baseURI,
			boolean preserveNodeIds, String mimeType, String sparqlUpdateString, String queryLnStr, boolean includeInferred, int maxQueryTime)
			throws RepositoryException, IOException, HTTPException {
		//ProtocolUtil.logRequestParameters(request);

		// default query language is SPARQL
		QueryLanguage queryLn = QueryLanguage.SPARQL;

		logger.debug("query language param = {}", queryLnStr);

		if (queryLnStr != null) {
			queryLn = QueryLanguage.valueOf(queryLnStr);

			if (queryLn == null) {
				throw new WebApplicationException("Unknown query language: " + queryLnStr, BAD_REQUEST);
			}
		}

		// build a dataset, if specified
		/*String[] defaultRemoveGraphURIs = request.getParameterValues(REMOVE_GRAPH_PARAM_NAME);
		String[] defaultInsertGraphURIs = request.getParameterValues(INSERT_GRAPH_PARAM_NAME);
		String[] defaultGraphURIs = request.getParameterValues(USING_GRAPH_PARAM_NAME);
		String[] namedGraphURIs = request.getParameterValues(USING_NAMED_GRAPH_PARAM_NAME);

		SimpleDataset dataset = null;
		if (defaultRemoveGraphURIs != null || defaultInsertGraphURIs != null || defaultGraphURIs != null
				|| namedGraphURIs != null) {
			dataset = new SimpleDataset();
		}

		if (defaultRemoveGraphURIs != null) {
			for (String graphURI : defaultRemoveGraphURIs) {
				try {
					IRI uri = createURIOrNull(repository, graphURI);
					dataset.addDefaultRemoveGraph(uri);
				} catch (IllegalArgumentException e) {
					throw new ClientHTTPException(SC_BAD_REQUEST, "Illegal URI for default remove graph: " + graphURI);
				}
			}
		}

		if (defaultInsertGraphURIs != null && defaultInsertGraphURIs.length > 0) {
			String graphURI = defaultInsertGraphURIs[0];
			try {
				IRI uri = createURIOrNull(repository, graphURI);
				dataset.setDefaultInsertGraph(uri);
			} catch (IllegalArgumentException e) {
				throw new ClientHTTPException(SC_BAD_REQUEST, "Illegal URI for default insert graph: " + graphURI);
			}
		}

		if (defaultGraphURIs != null) {
			for (String defaultGraphURI : defaultGraphURIs) {
				try {
					IRI uri = createURIOrNull(repository, defaultGraphURI);
					dataset.addDefaultGraph(uri);
				} catch (IllegalArgumentException e) {
					throw new ClientHTTPException(SC_BAD_REQUEST, "Illegal URI for default graph: " + defaultGraphURI);
				}
			}
		}

		if (namedGraphURIs != null) {
			for (String namedGraphURI : namedGraphURIs) {
				try {
					IRI uri = createURIOrNull(repository, namedGraphURI);
					dataset.addNamedGraph(uri);
				} catch (IllegalArgumentException e) {
					throw new ClientHTTPException(SC_BAD_REQUEST, "Illegal URI for named graph: " + namedGraphURI);
				}
			}
		}*/

		try (RepositoryConnection repositoryCon = ProtocolUtils.getRepositoryConnection(repository)) {
			Update update = repositoryCon.prepareUpdate(queryLn, sparqlUpdateString, baseURI);

			update.setIncludeInferred(includeInferred);
			update.setMaxExecutionTime(maxQueryTime);

			//if (dataset != null) {
			//	update.setDataset(dataset);
			//}

			// determine if any variable bindings have been set on this
			// update.
			/*@SuppressWarnings("unchecked")
			Enumeration<String> parameterNames = request.getParameterNames();

			while (parameterNames.hasMoreElements()) {
				String parameterName = parameterNames.nextElement();

				if (parameterName.startsWith(BINDING_PREFIX) && parameterName.length() > BINDING_PREFIX.length()) {
					String bindingName = parameterName.substring(BINDING_PREFIX.length());
					Value bindingValue = ProtocolUtil.parseValueParam(request, parameterName,
							repository.getValueFactory());
					update.setBinding(bindingName, bindingValue);
				}
			}*/

			update.execute();
		} catch (QueryInterruptedException e) {
			throw new WebApplicationException("update execution took too long", SERVICE_UNAVAILABLE);
		} catch (UpdateExecutionException e) {
			if (e.getCause() != null && e.getCause() instanceof HTTPException) {
				// custom signal from the backend, throw as HTTPException
				// directly
				// (see SES-1016).
				throw (HTTPException) e.getCause();
			} else {
				throw new WebApplicationException("Repository update error: " + e.getMessage(), e, INTERNAL_SERVER_ERROR);
			}
		} catch (RepositoryException e) {
			if (e.getCause() != null && e.getCause() instanceof HTTPException) {
				// custom signal from the backend, throw as HTTPException
				// directly
				// (see SES-1016).
				throw (HTTPException) e.getCause();
			} else {
				throw new WebApplicationException("Repository update error: " + e.getMessage(), e, INTERNAL_SERVER_ERROR);
			}
		} catch (MalformedQueryException e) {
			ErrorInfo errInfo = new ErrorInfo(ErrorType.MALFORMED_QUERY, e.getMessage());
			throw new WebApplicationException(errInfo.toString(), BAD_REQUEST);
		}
	}

	/**
	 * Upload data to the repository.
	 */
	private void getAddDataResult(Repository repository, HttpHeaders headers, String baseUriStr,
			boolean preserveNodeIds, String mimeType, InputStream inStream, boolean replaceCurrent)
			throws RepositoryException, IOException, HTTPException {
		RDFFormat rdfFormat = Rio.getParserFormatForMIMEType(mimeType)
			.orElseThrow(
				() -> new WebApplicationException("Unsupported MIME type: " + mimeType, UNSUPPORTED_MEDIA_TYPE));

		ValueFactory vf = repository.getValueFactory();

		Resource[] contexts = {};//ProtocolUtil.parseContextParam(request, CONTEXT_PARAM_NAME, vf);
		IRI baseURI = parseURIParam("baseURI", baseUriStr, vf);

		if (baseURI == null) {
			baseURI = vf.createIRI("foo:bar");
			logger.info("no base URI specified, using dummy '{}'", baseURI);
		}

		try (RepositoryConnection repositoryCon = ProtocolUtils.getRepositoryConnection(repository)) {
			repositoryCon.begin();

			if (preserveNodeIds) {
				repositoryCon.getParserConfig().set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
			}

			if (replaceCurrent) {
				repositoryCon.clear(contexts);
			}
			repositoryCon.add(inStream, baseURI.toString(), rdfFormat, contexts);

			repositoryCon.commit();
		} catch (UnsupportedRDFormatException e) {
			throw new WebApplicationException("No RDF parser available for format " + rdfFormat.getName(),
				UNSUPPORTED_MEDIA_TYPE);
		} catch (RDFParseException e) {
			ErrorInfo errInfo = new ErrorInfo(ErrorType.MALFORMED_DATA, e.getMessage());
			throw new WebApplicationException(errInfo.toString(), BAD_REQUEST);
		} catch (IOException e) {
			throw new WebApplicationException("Failed to read data: " + e.getMessage(), e, INTERNAL_SERVER_ERROR);
		} catch (RepositoryException e) {
			if (e.getCause() != null && e.getCause() instanceof HTTPException) {
				// custom signal from the backend, throw as HTTPException
				// directly
				// (see SES-1016).
				throw (HTTPException) e.getCause();
			} else {
				throw new WebApplicationException("Repository update error: " + e.getMessage(), e, INTERNAL_SERVER_ERROR);
			}
		}
	}

	public static Resource parseResourceParam(String paramName, String paramValue, ValueFactory vf) throws WebApplicationException {
		try {
			return Protocol.decodeResource(paramValue, vf);
		} catch (IllegalArgumentException e) {
			throw new WebApplicationException("Invalid value for parameter '" + paramName + "': " + paramValue,
					BAD_REQUEST);
		}
	}

	public static IRI parseURIParam(String paramName, String paramValue, ValueFactory vf) throws WebApplicationException {
		try {
			return Protocol.decodeURI(paramValue, vf);
		} catch (IllegalArgumentException e) {
			throw new WebApplicationException("Invalid value for parameter '" + paramName + "': " + paramValue,
					BAD_REQUEST);
		}
	}

	public static Value parseValueParam(String paramName, String paramValue, ValueFactory vf) throws WebApplicationException {
		try {
			return Protocol.decodeValue(paramValue, vf);
		} catch (IllegalArgumentException e) {
			throw new WebApplicationException("Invalid value for parameter '" + paramName + "': " + paramValue,
					BAD_REQUEST);
		}
	}
	
	@DELETE
    @Path("/repositories/{repId}/statements")
	public void deleteStatements(@Context HttpHeaders headers,
            @PathParam("repId") String repId,
            @QueryParam("context") String[] context,
            @QueryParam("subj") String subjStr,
            @QueryParam("pred") String predStr,
            @QueryParam("obj") String objStr) throws RepositoryException, IOException, HTTPException {
	    logger.info("DELETE data from repository");
	    
	    Repository repository = repositoryManager.getRepository(repId);
	    if(repository == null)
            throw new WebApplicationException("Cannot find repository '" + repId, NOT_FOUND);
	    
        ValueFactory vf = repository.getValueFactory();

        Resource subj = Protocol.decodeResource(subjStr, vf);
        IRI pred = Protocol.decodeURI(predStr, vf);
        Value obj = Protocol.decodeValue(objStr, vf);
        Resource[] contexts = Protocol.decodeContexts(context, vf);

        try (RepositoryConnection repositoryCon = repository.getConnection()) {
            repositoryCon.remove(subj, pred, obj, contexts);
        } catch (RepositoryException e) {
            if (e.getCause() != null && e.getCause() instanceof HTTPException) {
                // custom signal from the backend, throw as HTTPException
                // directly
                // (see SES-1016).
                throw (HTTPException) e.getCause();
            } else {
                throw new WebApplicationException("Repository update error: " + e.getMessage(), e);
            }
        }
	}
}
