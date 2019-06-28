/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.repository;


import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static org.eclipse.rdf4j.http.protocol.Protocol.BINDING_PREFIX;
import static org.eclipse.rdf4j.http.protocol.Protocol.DEFAULT_GRAPH_PARAM_NAME;
import static org.eclipse.rdf4j.http.protocol.Protocol.NAMED_GRAPH_PARAM_NAME;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.common.lang.service.FileFormatServiceRegistry;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.eclipse.rdf4j.http.server.HTTPException;
import org.eclipse.rdf4j.http.server.ProtocolUtil;
import org.eclipse.rdf4j.http.server.ServerHTTPException;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.GraphQueryResult;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryInterruptedException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.query.resultio.BooleanQueryResultWriterRegistry;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.ConfigTemplate;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.repository.manager.SystemRepository;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.eclipse.rdf4j.http.server.repository.RepositoryConfigController;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;

/**
 * Handles queries and admin (delete) operations on a repository and renders the results in a format suitable to the
 * type of operation.
 * 
 */
@Component(service = RepositoryController.class, property = { "osgi.jaxrs.resource=true" })
@Path("/rdf4j2-server")
public class RepositoryController  {
	@Reference
	RepositoryConfigController rcc;
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Reference
	private RepositoryManager repositoryManager;
	

	public RepositoryController() {
		System.out.println("Init RepositoryController");
	}
	
	@GET
	@Path("/repositories/{repId}")
    @Produces({"application/json", "application/sparql-results+json"})
    public boolean get(
			@Context UriInfo uriInfo, @PathParam("repId") String repId,
			@QueryParam("query") String query, @QueryParam("queryLn") String queryLn,
			@QueryParam("infer") String infer, @QueryParam("timeout") String timeout,
			@QueryParam("queryLn") String distinct, @QueryParam("limit") String limit,
			@QueryParam("offset") String offset) throws WebApplicationException {
		System.out.println("RepositoryController.get");
		System.out.println("repId_get=" + repId);
		System.out.println("query_get=" + query);
		return true;
	}
		
	@PUT
	@Path("/repositories/{repId}")
	public void createRep(String body, @PathParam("repId") String repId, @Context HttpHeaders headers) 
	throws Exception {
		ConfigTemplate ct = rcc.getConfigTemplate("native");
		Map<String, String> queryParams = new HashMap<>();
		queryParams.put("Repository ID", repId);
		String strConfTemplate = ct.render(queryParams);
		System.out.println("ConfigTemplate render: " + strConfTemplate);
		RepositoryConfig rc = rcc.updateRepositoryConfig(strConfTemplate);
		logger.info("PUT request invoked for repository '" + repId + "'");
		System.out.println("PUT request");
		System.out.println("OK");
	}
	
	@POST
	@Path("/repositories/{repId}")
    @Produces({"application/json", "application/sparql-results+json"})
	@Consumes(Protocol.FORM_MIME_TYPE)
    public boolean createForm(@Context UriInfo uriInfo, @PathParam("repId") String repId,
    		@FormParam("query") String query, @FormParam("queryLn") String queryLn,
    		@FormParam("queryLn") String infer, @FormParam("queryLn") String timeout,
    		@FormParam("queryLn") String distinct, @FormParam("queryLn") String limit,
    		@FormParam("queryLn") String offset) throws WebApplicationException {
		System.out.println("RepositoryController.createForm");
		System.out.println("repId=" + repId);
		System.out.println("query=" + query);
		return true;
	}
	
	@POST
	@Path("/repositories_sparql/{repId}")
    @Produces({"application/json", "application/sparql-results+json"})
	@Consumes(Protocol.SPARQL_QUERY_MIME_TYPE)
    public Query createSparql(@Context UriInfo uriInfo, @PathParam("repId") String repId,
    		@QueryParam("query") Query query, @QueryParam("queryLn") QueryLanguage queryLn,
    		@QueryParam("queryLn") boolean infer, @QueryParam("queryLn") int timeout,
    		@QueryParam("queryLn") boolean distinct, @QueryParam("queryLn") long limit,
    		@QueryParam("offset") long offset) throws WebApplicationException, HTTPException, RDF4JException, IOException {
		System.out.println("RepositoryController.createSparql");
		System.out.println("repId=" + repId);
		System.out.println("query=" + query);
		
		ConfigTemplate ct = rcc.getConfigTemplate("native");
        System.out.println("ConfigTemplate: " + ct);
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("Repository ID", repId);
        String strConfTemplate = ct.render(queryParams);
        System.out.println("ConfigTemplate render: " + strConfTemplate);
        RepositoryConfig rc = rcc.updateRepositoryConfig(strConfTemplate);
        System.out.println("RepositoryConfig.id: " + rc.getID());
        System.out.println("RepositoryConfig: " + rc.toString());

        Repository repository = repositoryManager.getRepository(repId);
        repository.init();
        RepositoryConnection repositoryCon = repository.getConnection();
		
		boolean headersOnly = false;
		Object queryResult = null;
		FileFormatServiceRegistry<? extends FileFormat, ?> registry;
		
		try {
			if (query instanceof GraphQuery) {
				if (!headersOnly) {
					GraphQuery gQuery = (GraphQuery) query;
					final GraphQueryResult qqr = distinct ? QueryResults.distinctResults(gQuery.evaluate())
							: gQuery.evaluate();
					queryResult = QueryResults.limitResults(qqr, limit, offset);
				}
				registry = RDFWriterRegistry.getInstance();
			}
			else {
				throw new ClientHTTPException(SC_BAD_REQUEST,
						"Unsupported query type: " + query.getClass().getName());
			}
		}
		catch (QueryInterruptedException e) {
			throw new ServerHTTPException(SC_SERVICE_UNAVAILABLE, "Query evaluation took too long");
		} 
		catch (QueryEvaluationException e) {
			if (e.getCause() != null && e.getCause() instanceof HTTPException) {
				throw (HTTPException) e.getCause();
			} 		
			else {
				throw new ServerHTTPException("Query evaluation error: " + e.getMessage());
			}
		}	
		return (GraphQuery) queryResult;
	}
	
	@DELETE
	@Path("/repositories/{repId}")
    @Produces({"application/json", "application/sparql-results+json"})
    public boolean delete(@Context UriInfo uriInfo, @PathParam("repId") String repId, @QueryParam("query") String query) throws WebApplicationException {
		logger.info("DELETE request invoked for repository '" + repId + "'");
		
		if (query != null) {
			logger.warn("query supplied on repository delete request, aborting delete");
			throw new WebApplicationException("Repository delete error: query supplied with request", Response.Status.BAD_REQUEST);
		}
		
		if (SystemRepository.ID.equals(repId)) {
			logger.warn("attempted delete of SYSTEM repository, aborting");
			throw new WebApplicationException("SYSTEM Repository can not be deleted", Response.Status.FORBIDDEN);
		}

		try {
			boolean success = repositoryManager.removeRepository(repId);
			if (success) {
				logger.info("DELETE request successfully completed");
				return true;
			} else {
				logger.error("error while attempting to delete repository '" + repId + "'");
				throw new WebApplicationException("could not locate repository configuration for repository '" + repId + "'.", Response.Status.BAD_REQUEST);
			}
		} catch (RDF4JException e) {
			logger.error("error while attempting to delete repository '" + repId + "'", e);
			throw new WebApplicationException("Repository delete error: " + e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
		}
	}
}

