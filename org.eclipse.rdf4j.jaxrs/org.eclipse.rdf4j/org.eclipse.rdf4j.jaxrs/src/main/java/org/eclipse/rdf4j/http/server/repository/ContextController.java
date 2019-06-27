package org.eclipse.rdf4j.http.server.repository;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;

import javax.ws.rs.Path;

import javax.ws.rs.PathParam;

import javax.ws.rs.QueryParam;

import javax.ws.rs.WebApplicationException;

import javax.ws.rs.core.Response;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.http.protocol.Protocol;

import org.eclipse.rdf4j.http.server.ProtocolUtil;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.impl.IteratingTupleQueryResult;
import org.eclipse.rdf4j.query.impl.ListBindingSet;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.ConfigTemplate;

import org.eclipse.rdf4j.repository.config.RepositoryConfig;

import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.eclipse.rdf4j.rio.Rio;
import org.osgi.service.component.annotations.Component;

import org.osgi.service.component.annotations.Reference;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.config.ConfigTemplate;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Path("/rdf4j-server")
@Component(service = ContextController.class, property = { "osgi.jaxrs.resource=true" })
public class ContextController {
	@Reference

	private RepositoryManager repositoryManager;

	@Reference

	RepositoryConfigController rcc;

	public ContextController() {
		System.out.println("ContextController");
	}

	@GET
	@Path("/repositories/{repid}/context")
	public String get(@PathParam("repid") String repId) {
		Repository repository = null;
		try {
			System.out.println("Ищем в менеджере " + repId);
			repository = repositoryManager.getRepository(repId);

		} catch (Exception e) {
			System.out.println("Не получили репозиторий из менеджера");
			e.printStackTrace();
		}
		if (repository != null) {
			System.out.println(repId + " найден");
			System.out.println("Repository = " + repository);
		} else {
			System.out.println(repId + " не найден");
			try {
				createRepoConfig(rcc, repId);
				repository = repositoryManager.getRepository(repId);
			} catch (Exception e) {
			}
		}
		List<String> columnNames = Arrays.asList("contextID");
		List<BindingSet> contexts = new ArrayList<>();
		RepositoryConnection repositoryCon = repository.getConnection();
		ModelBuilder builder = new ModelBuilder();
		Model model = builder
				.setNamespace("ex", "http://example.org/")
				.namedGraph("http://www.google.com")
				.subject("ex:Picasso")
					.add(RDF.TYPE, "ex:Artist")		// Picasso is an Artist
					.add(FOAF.FIRST_NAME, "Pablo") 	// his first name is "Pablo"
				.build();
		repositoryCon.add(model);
		
		try (CloseableIteration<? extends Resource, RepositoryException> contextIter = repositoryCon.getContextIDs()) {
			while (contextIter.hasNext()) {
				BindingSet bindingSet = new ListBindingSet(columnNames, contextIter.next());
				contexts.add(bindingSet);
			}
		}
		return Arrays.toString(contexts.toArray());
	}

	private void createRepoConfig(RepositoryConfigController rcc, String repId) throws RDF4JException, IOException {
		ConfigTemplate ct = rcc.getConfigTemplate("native");
		System.out.println("ConfigTemplate: " + ct);

		Map<String, String> queryParams = new HashMap<>();
		queryParams.put("Repository ID", repId);
		String strConfTemplate = ct.render(queryParams);
		System.out.println("ConfigTemplate render: " + strConfTemplate);
		RepositoryConfig rc = rcc.updateRepositoryConfig(strConfTemplate);
		System.out.println("RepositoryConfig.id: " + rc.getID());
		System.out.println("RepositoryConfig: " + rc.toString());
	}

}
