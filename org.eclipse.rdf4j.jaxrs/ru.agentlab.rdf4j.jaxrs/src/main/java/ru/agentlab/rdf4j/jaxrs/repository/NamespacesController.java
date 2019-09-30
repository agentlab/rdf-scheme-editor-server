package ru.agentlab.rdf4j.jaxrs.repository;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.impl.ListBindingSet;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.agentlab.rdf4j.jaxrs.ServerHTTPException;
import ru.agentlab.rdf4j.repository.RepositoryManagerComponent;

@Component(service = NamespacesController.class, property = { "osgi.jaxrs.resource=true" })
@Path("/rdf4j-server")
public class NamespacesController {
    private static final Logger logger = LoggerFactory.getLogger(NamespacesController.class);

    @Reference
    private RepositoryManagerComponent repositoryManager;

    @Activate
    public void activate() {
        logger.info("Activate " + this.getClass().getSimpleName());
    }

    @Deactivate
    public void deactivate() {
        logger.info("Deactivate " + this.getClass().getSimpleName());
    }

    @GET
    @Path("/repositories/{repId}/namespaces")
    @Produces({ "application/json", "application/sparql-results+json" })
    public List<BindingSet>/* TupleQueryResult */ get(@Context UriInfo uriInfo, @PathParam("repId") String repId, @QueryParam("context") Resource[] context) throws RDF4JException, IOException, ServerHTTPException {
        Repository repository = repositoryManager.getRepository(repId);
        if (repository == null)
            throw new WebApplicationException("Repository with id=" + repId + " not found", NOT_FOUND);

        List<String> columnNames = Arrays.asList("prefix", "namespace");
        List<BindingSet> namespaces = new ArrayList<>();

        // ValueFactory vf = SimpleValueFactory.getInstance();
        // Literal prefix = vf.createLiteral("ddd");
        // Literal namespace = vf.createLiteral("http://rrr.tu/678");

        // BindingSet bindingSet = new ListBindingSet(columnNames, prefix, namespace);
        // namespaces.add(bindingSet);

        try (RepositoryConnection repositoryCon = repository.getConnection()) {
            System.out.println("Hello");
            final ValueFactory vf = repositoryCon.getValueFactory();
            try {
                try (RepositoryResult<Namespace> iter = repositoryCon.getNamespaces()) {
                    while (iter.hasNext()) {
                        Namespace ns = iter.next();
                        Literal prefix = vf.createLiteral(ns.getPrefix());
                        Literal namespace = vf.createLiteral(ns.getName());
                        BindingSet bindingSet = new ListBindingSet(columnNames, prefix, namespace);
                        namespaces.add(bindingSet);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (RepositoryException e) {
                e.printStackTrace();
                throw new WebApplicationException("Repository error: " + e.getMessage(), INTERNAL_SERVER_ERROR);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return namespaces;// new IteratingTupleQueryResult(columnNames, namespaces);
    }

    @DELETE
    @Path("/repositories/{repId}/namespaces")
    @Produces({ "application/json", "application/sparql-results+xml" })
    public void remove(@PathParam("repId") String repId) throws ServerHTTPException {
        Repository repository = repositoryManager.getRepository(repId);
        try (RepositoryConnection repositoryCon = repository.getConnection()){
            repositoryCon.clearNamespaces();
        } catch (Exception e) {
            throw new WebApplicationException("Repository error: " + e.getMessage(), INTERNAL_SERVER_ERROR);
        }
    }
}
