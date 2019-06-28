package org.eclipse.rdf4j.http.server.repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.common.iteration.CloseableIteration;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.server.ProtocolUtil;
import org.eclipse.rdf4j.http.server.ServerHTTPException;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.config.ConfigTemplate;
import org.eclipse.rdf4j.repository.config.RepositoryConfig;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.query.algebra.evaluation.QueryBindingSet;
import org.eclipse.rdf4j.query.impl.IteratingTupleQueryResult;
import org.eclipse.rdf4j.query.impl.ListBindingSet;
@Path("/rdf4j-server")
@Component(service = NamespacesComponent.class, property = { "osgi.jaxrs.resource=true" })
	public class NamespacesComponent {

	@Reference
	private RepositoryManager repositorymanager;

	@Reference
	RepositoryConfigController rcc;

	public NamespacesComponent() {
		System.out.println("NamespacesComponent");
	}

	private void createRepoConfig (RepositoryConfigController rcc, String repId) throws RDF4JException, IOException {
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

	@GET
	@Path("/repositories/{repId}/namespaces")
	@Produces({"application/json", "application/sparql-results+json"})
	public List<BindingSet>/*TupleQueryResult*/ get(@Context UriInfo uriInfo, @PathParam("repId") String repId, @QueryParam("context") Resource[] context) throws RDF4JException, IOException, ServerHTTPException {
		Repository repository = null;
		List<String> columnNames = Arrays.asList("prefix", "namespace");
		List<BindingSet> namespaces = new ArrayList<>();
		
		/*ValueFactory vf = SimpleValueFactory.getInstance();
		Literal prefix = vf.createLiteral("ddd");
		Literal namespace = vf.createLiteral("http://rrr.tu/678");

		BindingSet bindingSet = new ListBindingSet(columnNames, prefix, namespace);
		namespaces.add(bindingSet);*/
		try {
			repository = repositorymanager.getRepository(repId);
		}
		catch (Exception e){
			System.out.println("Не получили репозиторий из менеджера");
			e.printStackTrace();
		}
		if (repository != null) {
			repository = repositorymanager.getRepository(repId);
	
			try (RepositoryConnection repositoryCon = repository.getConnection()) {
				System.out.println("Hello");			
				final ValueFactory vf = repositoryCon.getValueFactory();
				repositoryCon.setNamespace("str", "ejgierjgierjigjr");
				try {
					System.out.println("trn");
					try (RepositoryResult<Namespace> iter = repositoryCon.getNamespaces()) {
						System.out.println("pty");
						
						while (iter.hasNext()) {
							System.out.println("tttyyy");
							Namespace ns = iter.next();
							System.out.println("tttyyy1");
							Literal prefix = vf.createLiteral(ns.getPrefix());
							System.out.println("tttyyy2");
							Literal namespace = vf.createLiteral(ns.getName());
							System.out.println("tttyyy3");
							BindingSet bindingSet = new ListBindingSet(columnNames, prefix, namespace);
							System.out.println("tttyyy4");
							namespaces.add(bindingSet);
							System.out.println(namespaces);
						}
						repositoryCon.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				} catch (RepositoryException e) {
					e.printStackTrace();
					repositoryCon.close();
					System.out.println("zet");
					throw new ServerHTTPException("Repository error: " + e.getMessage(), e);
				}
				repositoryCon.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else {
			System.out.println(repId + " не найден");
			createRepoConfig(rcc, repId);
			try {
				repository = repositorymanager.getRepository(repId);
			}
			catch (Exception e){
				System.out.println("Ошибка создания репозитория");
				e.printStackTrace();
			}
	
			try (RepositoryConnection repositoryCon = repository.getConnection()) {
				System.out.println("Hello");			
				final ValueFactory vf = repositoryCon.getValueFactory();
				repositoryCon.setNamespace("str", "ejgierjgierjigjr");
				try {
					System.out.println("trn");
					try (RepositoryResult<Namespace> iter = repositoryCon.getNamespaces()) {
						System.out.println("pty");
						
						while (iter.hasNext()) {
							System.out.println("tttyyy");
							Namespace ns = iter.next();
							Literal prefix = vf.createLiteral(ns.getPrefix());
							Literal namespace = vf.createLiteral(ns.getName());
							BindingSet bindingSet = new ListBindingSet(columnNames, prefix, namespace);
							
							System.out.println(bindingSet);
							namespaces.add(bindingSet);
							System.out.println(namespaces.size());
						}
						repositoryCon.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				} catch (RepositoryException e) {
					e.printStackTrace();
					repositoryCon.close();
					System.out.println("zet");
					throw new ServerHTTPException("Repository error: " + e.getMessage(), e);
				}
				repositoryCon.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println(namespaces);
		return namespaces;//new IteratingTupleQueryResult(columnNames, namespaces);
    }


	@DELETE
    @Path("/repositories/{repId}/namespaces")
	@Produces({"application/json", "application/sparql-results+xml"})
    public void remove(@PathParam("repId") String repId) throws ServerHTTPException {
		Repository repository = null;
		repository = repositorymanager.getRepository(repId);
		RepositoryConnection repositoryCon = repository.getConnection();
    	try {
    		System.out.println("Привет");
    		repositoryCon.clearNamespaces();
    		System.out.println("Привет");
    		repositoryCon.close();
    	}catch (Exception e) {
    		System.out.println("ой ой");
		}
    	repositoryCon.close();
    }
}
