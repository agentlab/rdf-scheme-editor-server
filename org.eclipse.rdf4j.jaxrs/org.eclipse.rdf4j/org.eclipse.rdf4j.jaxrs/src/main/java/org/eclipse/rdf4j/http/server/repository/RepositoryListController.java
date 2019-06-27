/*******************************************************************************
 * Copyright (c) 2015 Eclipse RDF4J contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *******************************************************************************/
package org.eclipse.rdf4j.http.server.repository;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.impl.IteratingTupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Handles requests for the list of repositories available on this server.
 * 
 */
@Component(service = RepositoryListController.class, property = { "osgi.jaxrs.resource=true" })
@Path("/rdf4j2-server")
public class RepositoryListController {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Reference
	private RepositoryManager repositoryManager;
	
	public RepositoryListController() {
		System.out.println("Init RepositoryListController");
	}
	
	@GET
	@Path("/repositories")
    @Produces({"application/json", "application/sparql-results+json"})
    public TupleQueryResult list(@Context UriInfo uriInfo) throws WebApplicationException {
		System.out.println("Start repo list retrieving");
		ValueFactory vf = SimpleValueFactory.getInstance();
		
		List<BindingSet> bindingSets = new ArrayList<>();
		List<String> bindingNames = new ArrayList<>();
		
		try {
			// Determine the repository's URI
			StringBuffer requestURL = new StringBuffer(uriInfo.getPath());
			if (requestURL.charAt(requestURL.length() - 1) != '/') {
				requestURL.append('/');
			}
			String namespace = "https://agentlab.ru/rdf4j2-server/repositories/";//requestURL.toString();
			System.out.println("namespace=" + namespace);
			
			repositoryManager.getAllRepositoryInfos(false).forEach(info -> {
				QueryBindingSet bindings = new QueryBindingSet();
				bindings.addBinding("uri", vf.createIRI(namespace, info.getId()));
				bindings.addBinding("id", vf.createLiteral(info.getId()));
				if (info.getDescription() != null) {
					bindings.addBinding("title", vf.createLiteral(info.getDescription()));
				}
				bindings.addBinding("readable", vf.createLiteral(info.isReadable()));
				bindings.addBinding("writable", vf.createLiteral(info.isWritable()));
				bindingSets.add(bindings);
			});

			bindingNames.add("uri");
			bindingNames.add("id");
			bindingNames.add("title");
			bindingNames.add("readable");
			bindingNames.add("writable");
		} catch (RepositoryException e) {
			logger.error("Repository list query error", e);
			throw new WebApplicationException("Repository list query error: " + e.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
		}
		
		System.out.println("bindingSets=" + bindingSets.toString());
		
		System.out.println("End repo list retrieving");
        return new IteratingTupleQueryResult(bindingNames, bindingSets);
    }
}
