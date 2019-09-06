package org.eclipse.rdf4j.http.server.repository;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.config.ConfigTemplate;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Path("/rdf4j-server")
@Component(service = StatementsController.class, property = { "osgi.jaxrs.resource=true" })
public class StatementsController {
	@Reference
	private RepositoryManager repositoryManager;
	@Reference
	RepositoryConfigController rcc;
	public StatementsController() {
		System.out.println("StatementsController");
	}

	@GET
	@Path("/repositories/{repid}/statements")
	public String get(@PathParam("repid") String repId) {
		Repository repository = null;

		try {
			System.out.println("Ищем в менеджере " + repId);
			repository = repositoryManager.getRepository(repId);
			RepositoryConnection conn = repository.getConnection();
			ModelBuilder builder = new ModelBuilder();
			Model model = builder
					.setNamespace("ex", "http://example.org/")
					.subject("ex:Picasso")
						.add(RDF.TYPE, "ex:Artist")		// Picasso is an Artist
						.add(FOAF.FIRST_NAME, "Pablo") 	// his first name is "Pablo"
					.build();
			conn.add(model);
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
				RepositoryConnection conn = repository.getConnection();
				ModelBuilder builder = new ModelBuilder();
				Model model = builder
						.setNamespace("ex", "http://example.org/")
						.subject("ex:Picasso")
							.add(RDF.TYPE, "ex:Artist")		// Picasso is an Artist
							.add(FOAF.FIRST_NAME, "Pablo") 	// his first name is "Pablo"
						.build();
				conn.add(model);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		OutputStream out = new ByteArrayOutputStream();

		
		try {
			RDFWriter rdfWriter = Rio.createWriter(RDFFormat.TURTLE, out);
			RepositoryConnection conn1 = repository.getConnection();
			conn1.exportStatements(null, null, null, true, rdfWriter);
		} catch (Exception e) {
			System.out.println("Не удалось");
			e.printStackTrace();
		}
		String theout = out.toString();
		
		
		return theout;
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
