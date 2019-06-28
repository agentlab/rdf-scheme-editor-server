package org.eclipse.rdf4j.http.server.repository;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.config.ConfigTemplate;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Path("/rdf4j-server")
@Component(service = SizeControllerComponent.class, property = { "osgi.jaxrs.resource=true" })
public class SizeControllerComponent {
	@Reference
	private RepositoryManager repositoryManager;
	
	@Reference
	RepositoryConfigController rcc;
	
	public SizeControllerComponent() {
		System.out.println("SizeControllerComponent");
	}
	@GET
	@Path("/repositories/{repId}/size")
	public long get(@PathParam("repId") String repId, @QueryParam("context") Resource[] context) {
		
		Repository repository = null;
		long size = -1;
		try {
			System.out.println("Ищем в менеджере " + repId);
			repository = repositoryManager.getRepository(repId);
		} catch (Exception e) {
			System.out.println("Не получили репозиторий из менеджера");
			//logger.debug("Не получили репозиторий из менеджера");
			e.printStackTrace();
		}
		if (repository != null) {
			size = repository.getConnection().size(context);
			System.out.println(repId + " найден");
			System.out.println("Repository = " + repository + " с размером " + size);
			return size;
		} else {
			System.out.println(repId + " не найден");
			
			try {
				createRepoConfig(rcc, repId);
				repository = repositoryManager.getRepository(repId);
				size = repository.getConnection().size(context);
				System.out.println(repId + " создан с размером " + size);
				System.out.println("Repository = " + repository);
				return size;
			} catch (RDF4JException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new WebApplicationException("Repository " + repId + " not found", Response.Status.NOT_FOUND);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new WebApplicationException("Repository " + repId + " not found", Response.Status.NOT_FOUND);
			}
			
		//	throw new WebApplicationException("Repository " + repId + " not found", Response.Status.NOT_FOUND);
		}
		//System.out.println(repId);
		//return 5;
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
