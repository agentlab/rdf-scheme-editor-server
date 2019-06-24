package org.eclipse.rdf4j.http.server.repository;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.eclipse.rdf4j.http.server.ServerHTTPException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.eclipse.rdf4j.repository.config.ConfigTemplate;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;

@Component (service = NamespaceController.class, property = { "osgi.jaxrs.resource=true" })
@Path ("/rdf4j2-server")
public class NamespaceController {
	
	@Reference
    private RepositoryManager repositoryManager;

    @Reference
    RepositoryConfigController rcc;
	
	
	public NamespaceController() {
		System.out.println("NamespaceController started");
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

	
	
	@PUT
	@Path ("/repositories/{repId}/namespaces/{prefix}")
	public void putNamesp (@Context UriInfo uriInfo, @PathParam("repId") String repId,
				@PathParam("prefix") String prefix,@QueryParam("context") Resource[] context,@QueryParam("baseURI") IRI baseURI, String body)throws ClientHTTPException, ServerHTTPException, WebApplicationException, IOException {
			Repository repository = null;
			
	        try {
	        repository = repositoryManager.getRepository(repId);
	        }
	        catch (Exception e){
	        	System.out.println("Не получили репозиторий из менеджера");
	        	e.printStackTrace();
	        }
	        if (repository != null) {
	        	System.out.println(repId + " найден");
	        	repository.getConnection().setNamespace(prefix, body);
	        	System.out.println("Создан = " + prefix + " " + body);
	        }
	        else {
	        	System.out.println(repId + " не найден");
	        	createRepoConfig(rcc, repId);
	        	try {
	        	repository = repositoryManager.getRepository(repId);
	        	repository.init();
	        	System.out.println(repId + " создан");
	        	}
	        	catch (Exception e){
	        		System.out.println("Ошибка создания репозитория");
		        	e.printStackTrace();
	        	}
	        	/*try {
	        		repositoryCon.setNamespace(prefix, body);
	        	}
	        	catch(Exception e) {
	        		System.out.println("Ошибка создания неймспейса");
		        	e.printStackTrace();
	        	}
	        	System.out.println("Создан = " + prefix + " " + body);
	        	repositoryCon.commit();
	        	repositoryCon.close();
	        	*/
	        }
	}
		
	/*
	@DELETE 
	@Path ("/repositories/{repId}/namespaces/{prefix}")
	public void delNamesp ( @PathParam("repId") String repId,
				@PathParam("prefix") String prefix ) throws ClientHTTPException, ServerHTTPException, IOException {
		try {
			repository = repositoryManager.getRepository(repId);
		}
		catch (Exception e) {
				System.out.println("Не получили репозиторий из менеджера");
				e.printStackTrace();
		}
			if (repository != null) {
				repository.init();
				RepositoryConnection repositoryCon = repository.getConnection();
				repositoryCon.begin();
				try {
					repositoryCon.removeNamespace(prefix);
					System.out.println("Remove namespace = "+ prefix);
				}
				catch (RepositoryException e) {
					throw new ServerHTTPException("Repository put error: " + e.getMessage(), e);
				}
				repositoryCon.commit();
				repositoryCon.close();
			}
			else {
				System.out.println(repId + " не найден");
			        
			}
	}
	*/	
}
