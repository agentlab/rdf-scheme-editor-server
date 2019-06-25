/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors, Aduna, and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.repository;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.repository.manager.SystemRepository;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles queries and admin (delete) operations on a repository and renders the results in a format suitable to the
 * type of operation.
 * 
 */
@Component(service = RepositoryController.class, property = { "osgi.jaxrs.resource=true" })
@Path("/rdf4j2-server")
public class RepositoryController  {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Reference
	private RepositoryManager repositoryManager;
	
	public RepositoryController() {
		System.out.println("Init RepositoryController");
	}
	
	@GET
	@Path("/repositories/{repId}")
    @Produces({"application/json", "application/sparql-results+json"})
    public boolean get(@Context UriInfo uriInfo, @PathParam("repId") String repId,
    		@QueryParam("query") String query, @QueryParam("queryLn") String queryLn,
    		@QueryParam("queryLn") String infer, @QueryParam("queryLn") String timeout,
    		@QueryParam("queryLn") String distinct, @QueryParam("queryLn") String limit,
    		@QueryParam("queryLn") String offset) throws WebApplicationException {
		System.out.println("RepositoryController.get");
		System.out.println("repId=" + repId);
		System.out.println("query=" + query);
		return true;
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
	@Path("/repositories/{repId}")
    @Produces({"application/json", "application/sparql-results+json"})
	@Consumes(Protocol.SPARQL_QUERY_MIME_TYPE)
    public boolean createSparql(@Context UriInfo uriInfo, @PathParam("repId") String repId,
    		@QueryParam("query") String query) throws WebApplicationException {
		System.out.println("RepositoryController.createSparql");
		System.out.println("repId=" + repId);
		System.out.println("query=" + query);
		return true;
	}
		@PUT
	@Path("/repositories/{repId}")
//    @Produces({"application/json", "application/sparql-results+json"})
	public void createRep(/*String body, */@PathParam("repId") String repId/*, @Context HttpHeaders headers*/) throws Exception {
		logger.info("PUT request invoked for repository '" + repId + "'");
		System.out.println("PUT request");
		/*String contentType = headers.getHeaderString(HttpHeaders.CONTENT_TYPE);
		try {
			InputStream in = new ByteArrayInputStream(body.getBytes("UTF-8"));
			Model model = Rio.parse(in, " ",Rio.getParserFormatForMIMEType(contentType).orElseThrow(() -> 
					new HTTPException(HttpStatus.SC_BAD_REQUEST,
					"unrecognized content type " + contentType)));
			RepositoryConfig config = RepositoryConfigUtil.getRepositoryConfig(model, repId);
			repositoryManager.addRepositoryConfig(config);
		} catch (RDFParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedRDFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (HTTPException e) {
			throw new ServerHTTPException("Repository create error: " + e.getMessage(), e);
			// TODO Auto-generated catch block
		}*/
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
