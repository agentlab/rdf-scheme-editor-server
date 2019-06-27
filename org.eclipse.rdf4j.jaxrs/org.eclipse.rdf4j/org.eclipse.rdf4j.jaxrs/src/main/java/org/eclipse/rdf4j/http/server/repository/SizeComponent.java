package org.eclipse.rdf4j.http.server.repository;

import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.config.RepositoryConfigException;
import org.eclipse.rdf4j.repository.manager.RepositoryManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

@Path("/rdf4j2-server")
@Component(service = SizeComponent.class, property = {"osgi.jaxrs.resource=true"})
public class SizeComponent {
    @Reference
    private RepositoryManager repositoryManager;

    public SizeComponent() {
        System.out.println("SizeComponent started!");
    }

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @GET
    @Path("/repositories/{repId}/size")
    public Object method(@PathParam("repId") String repId,
                         @QueryParam("context") String context)
            throws RepositoryException {
        Repository repository = null;
        RepositoryConnection repositoryCon = null;

        try {
            repository = repositoryManager.getRepository(repId);
            repositoryCon = repository.getConnection();
        } catch (RepositoryConfigException e) {
            logger.debug("Косяк с логгером");
            e.printStackTrace();
        } catch (RepositoryException e) {
            logger.debug("Косяк с репозиторием");
            e.printStackTrace();
        }
        long size = 0;

        ValueFactory vf = repository.getValueFactory();
        String[] context_str = new String[1];
        context_str[0] = context;
        Resource[] contexts = Protocol.decodeContexts(context_str, vf);

        try {
            if (repositoryCon != null) {
                size = repositoryCon.size(contexts);
            }
            System.out.println("Размера графа rdf репозитория = " + size);
        } catch (Exception e) {
            repositoryCon.close();
            logger.debug("Произошла ошибка получения размера контекста!");
            e.printStackTrace();
        }
        repositoryCon.close();
        return null;
    }
}
