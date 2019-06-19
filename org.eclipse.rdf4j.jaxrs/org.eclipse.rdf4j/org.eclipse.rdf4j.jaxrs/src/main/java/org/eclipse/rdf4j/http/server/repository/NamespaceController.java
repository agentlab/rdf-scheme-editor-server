package org.eclipse.rdf4j.http.server.repository;

import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.osgi.service.component.annotations.Component;

import java.io.IOException;
import java.util.HashMap;
import javax.ws.rs.DELETE;
import javax.ws.rs.PUT;

@Component (service = NamespaceController.class, property = { "osgi.jaxrs.resource=true" })
@Path ("/rdf4j2-server")
public class NamespaceController {
	
	
	HashMap<String,String> namespases = new HashMap<String, String>(); 
	
	public NamespaceController() {
		System.out.println("CompIntefrace started");
	}
	
	
	@PUT
	@Path ("/repositories/{repId}/namespaces/{prefix}")
	public void putNamesp (@Context UriInfo uriInfo, @PathParam("repId") String repId,
				@PathParam("prefix") String prefix, String body)throws RepositoryException, IOException, WebApplicationException {
			if (body.length() == 0){
				throw new WebApplicationException("No namespace name found in request body", Response.Status.BAD_REQUEST);
			}
			else 
				if (namespases.get(prefix) == null){
					namespases.put(prefix, body);
					System.out.println("Объект создан");
					System.out.println("Namespace: prefix -"+" " + prefix + " body - " + namespases.get(prefix));
				}
				else { 
					System.out.println("Такой префикс уже существует");
					System.out.println("Old namespace: prefix -"+" " + prefix + " body - " + namespases.get(prefix));
					namespases.put(prefix, body);
					System.out.println("New namespace: prefix -"+" " + prefix + " body - " + namespases.get(prefix));
				
				}
	}
	@DELETE 
	@Path ("/repositories/{repId}/namespaces/{prefix}")
	public void delNamesp ( @PathParam("repId") String repId,
				@PathParam("prefix") String prefix ) {
			if (namespases.get(prefix) != null) { 
				namespases.remove(prefix);
				System.out.println("Prefix for delete = " + prefix);
				System.out.println(namespases.get(prefix));	
			}
			else 
				throw new WebApplicationException("Error create" ,Response.Status.NO_CONTENT);
	}
	
}
