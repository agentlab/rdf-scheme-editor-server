package org.eclipse.rdf4j.http.server.repository;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.ConfigTemplate;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Path("/rdf4j2-server")
@Component(service = AddDataToRepositoryComponent.class, property = { "osgi.jaxrs.resource=true" })
public class AddDataToRepositoryComponent {
	@Reference
	private RepositoryManager repositoryManager;

	@Reference
	RepositoryConfigController rcc;
	public AddDataToRepositoryComponent(){
		System.out.println("NewComponent started!");
	}
	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	@PUT
	@Path("/repositories/{repId}/statements")
	public Object method(@Context UriInfo uriInfo,
			@PathParam("repId") String repId,
			@QueryParam("context") String context,
			@QueryParam("baseURI") IRI baseURI,
			String body) throws RepositoryException, IOException {

		ConfigTemplate ct = rcc.getConfigTemplate("native");
		System.out.println("ConfigTemplate: " + ct);

		Map<String, String> queryParams = new HashMap<>();
		queryParams.put("Repository ID", repId);
		String strConfTemplate = ct.render(queryParams);
		System.out.println("ConfigTemplate render: " + strConfTemplate);
		RepositoryConfig rc = rcc.updateRepositoryConfig(strConfTemplate);
		System.out.println("RepositoryConfig.id: " + rc.getID());
		System.out.println("RepositoryConfig: " + rc.toString());
		for (String s : repositoryManager.getRepositoryIDs()) {
			System.out.println(s);
		}

		Repository repository = null;
		try {
			System.out.println("Ищем в менеджере \"" + repId + "\"");
			repository = repositoryManager.getRepository(repId);
		} catch (Exception e) {
			logger.debug("Не получили репозиторий из менеджера");
			e.printStackTrace();
		}
		try {
			repository.init();
		} catch (Exception e) {
			logger.debug("Не проинициализировался репозиторий.");
			e.printStackTrace();
		}
		RepositoryConnection repositoryCon = null;
		try {
			repositoryCon = repository.getConnection();
		} catch (Exception e) {
			logger.debug("Не создалось подключение к репозиторию.");
			e.printStackTrace();
		}

		ValueFactory vf = repository.getValueFactory();

		System.out.println("uriInfo = " + uriInfo);
		System.out.println("repId=" + repId);
		System.out.println("context=" + context);
		System.out.println("baseURI=" + baseURI);
		System.out.println("body=" + body);

		if (baseURI == null) {
			baseURI = vf.createIRI("foo:bar");
			}
		try {
			repositoryCon.begin();
			InputStream in = new ByteArrayInputStream(body.getBytes());
			repositoryCon.add(in, baseURI.toString(), RDFFormat.RDFXML);
			repositoryCon.commit();
			System.out.println("Данные успешно добавлены! Successful");
		} catch (Exception e) {
			repositoryCon.rollback();
			logger.debug("Данные не добавлены! Fail.");
			e.printStackTrace();
		}

		repositoryCon.close();
		return null;
	}

}
