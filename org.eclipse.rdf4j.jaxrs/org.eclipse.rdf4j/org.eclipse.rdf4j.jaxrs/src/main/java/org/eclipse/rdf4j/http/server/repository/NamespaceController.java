package org.eclipse.rdf4j.http.server.repository;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.eclipse.rdf4j.http.server.ServerHTTPException;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.eclipse.rdf4j.repository.config.ConfigTemplate;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;


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
				@PathParam("prefix") String prefix,@QueryParam("context") Resource[] context,@QueryParam("baseURI") IRI baseURI, String body)throws  
					 WebApplicationException, IOException {
			if (body.length()==0) {
				System.out.println("Не получили неймспейс");
				throw new WebApplicationException("Error create" ,Response.Status.BAD_REQUEST);
			}
			else {
				Repository repository = null;
				RepositoryConnection repositoryCon  = null;
	  
				try {
						repository = repositoryManager.getRepository(repId);
						repositoryCon = repository.getConnection();
				}
				catch (Exception e){
					System.out.println("Не получили репозиторий из менеджера");
					e.printStackTrace();
				}
				if (repository != null) {
					System.out.println(repId + " найден");
					try {
						if (repositoryCon.getNamespace(prefix) != null) {
							repositoryCon.setNamespace(prefix, body);
							System.out.println("Обновлен = " + prefix + " " + repositoryCon.getNamespace(prefix));
						}	
						else {
							repositoryCon.setNamespace(prefix, body);
							System.out.println("Создан = " + prefix + " " + repositoryCon.getNamespace(prefix));
						}
					}
					catch (Exception e) {
						System.out.println("Ошибка создания неймспейса");
						e.printStackTrace();
					}
				}
				else {
					System.out.println(repId + " не найден");
					createRepoConfig(rcc, repId);
					try {
						repository = repositoryManager.getRepository(repId);
						repository.init();
						repositoryCon = repository.getConnection();
						System.out.println("Репозиторий "+ repId + " создан");
					}
					catch (Exception e){
						System.out.println("Ошибка создания репозитория");
						e.printStackTrace();
					}
					try {
						if (repositoryCon.getNamespace(prefix) != null) {
							repositoryCon.setNamespace(prefix, body);
							System.out.println("Обновлен = " + prefix + " " + repositoryCon.getNamespace(prefix));	
						}
						else {
							repositoryCon.setNamespace(prefix, body);
							System.out.println("Создан = " + prefix + " " + repositoryCon.getNamespace(prefix));
						}	
					}
					catch (Exception e) {
						System.out.println("Ошибка создания неймспейса");
						e.printStackTrace();
					}
				}
			}
	}
		
	
	@DELETE 
	@Path ("/repositories/{repId}/namespaces/{prefix}")
	public void delNamesp ( @PathParam("repId") String repId,
				@PathParam("prefix") String prefix ) throws ClientHTTPException, ServerHTTPException, IOException {
		Repository repository = null;
		RepositoryConnection repositoryCon  = null;
		  
        try {
        	repository = repositoryManager.getRepository(repId);
        	repositoryCon = repository.getConnection();
        }
        catch (Exception e){
        	System.out.println("Не получили репозиторий из менеджера");
        	e.printStackTrace();
        }
        if (repository != null) {
        	System.out.println(repId + " найден");
        	try {
        		if (repositoryCon.getNamespace(prefix) != null) {
        			repositoryCon.removeNamespace(prefix);
        			System.out.println("Удален = " + prefix + " " + repositoryCon.getNamespace(prefix));
        		}
        		else System.out.println("Неймспейс не существует");
        	}
        	catch (Exception e) {
        		System.out.println("Ошибка удаления неймспейса");
        		e.printStackTrace();
        	}
        }
        else {
        	System.out.println(repId + " не найден");
        	createRepoConfig(rcc, repId);
        	try {
        		repository = repositoryManager.getRepository(repId);
        		repository.init();
        		repositoryCon = repository.getConnection();
        		System.out.println("Репозиторий "+ repId + " создан");
        	}
        	catch (Exception e){
        		System.out.println("Ошибка создания репозитория");
	        	e.printStackTrace();
        	}
        	try {
        		if (repositoryCon.getNamespace(prefix) != null) {
        			repositoryCon.removeNamespace(prefix);
        			System.out.println("Удален = " + prefix + " " + repositoryCon.getNamespace(prefix));
        		}
        		else System.out.println("Неймспейс не существует");
        	}
        	catch (Exception e) {
        		System.out.println("Ошибка удаления неймспейса");
        		e.printStackTrace();
        	}
        	
        }
	}
	
}
