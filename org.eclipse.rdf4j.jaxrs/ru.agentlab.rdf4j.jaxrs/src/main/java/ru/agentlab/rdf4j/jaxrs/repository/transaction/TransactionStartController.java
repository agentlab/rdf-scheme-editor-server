package org.eclipse.rdf4j.http.server.transaction;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.server.ProtocolUtil;
import org.eclipse.rdf4j.http.server.ServerHTTPException;
import org.eclipse.rdf4j.http.server.repository.RepositoryInterceptor;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//Аннотация для создания компонента и аннотация для адреса
@Component(service = TransactionStartController.class, property = {"osgi.jaxrs.resource=true"})
@Path("/rdf4j2-server")
public void handleRequestInternal(@Context HttpServletRequest request, @PathParam("repId") String repId) throws Exception {

    //для логирования (вывод информации)
    private static Logger logger = LoggerFactory.getLogger(TransactionStartController.class.getName());

    //конструктор, караф выведет эту строку при создании класса
    public TransactionStartController() {
        System.out.println("Init TransactionStartController");
    }


    //аннотация метода и ссылки по которым будет вызываться функция ниже (handleRequestInternal)
    @POST
    @Path("/repositories/{repId}/transactions")
    public HttpServletResponse handleRequestInternal(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("repId") String repId) throws Exception {
        logger.info("POST transaction start");
        Repository repository = repositoryManager.getRepository(repId);
        if (repository == null)
            throw new WebApplicationException("Repository with id=" + repId + " not found", NOT_FOUND);

        String transactionID = startTransaction(repository, request);
        response.setHeader("Location", transactionID);
        logger.info("transaction started");

        return response;
    }

    private void startTransaction(Repository repository, HttpServletRequest request)
            throws ServerHTTPException
    {
        ProtocolUtil.logRequestParameters(request);
		String txnId = null;

        IsolationLevel isolationLevel = null;
        final String isolationLevelString = request.getParameter(Protocol.ISOLATION_LEVEL_PARAM_NAME);
        if (isolationLevelString != null) {
            final IRI level = SimpleValueFactory.getInstance().createIRI(isolationLevelString);

            // для каждого пользоввтеля свой уровень(изолированный)
            for (IsolationLevel standardLevel : IsolationLevels.values()) {
                if (standardLevel.getURI().equals(level)) {
                    isolationLevel = standardLevel;
                    break;
                }
            }
        }

        // получаем подключение к нужному репозиторию
        try (RepositoryConnection conn = repository.getConnection()) {

            // настраиваем конфигурации
            ParserConfig config = conn.getParserConfig();
            config.set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
            config.addNonFatalError(BasicParserSettings.VERIFY_DATATYPE_VALUES);
            config.addNonFatalError(BasicParserSettings.VERIFY_LANGUAGE_TAGS);
            // начинаем подключение
            conn.begin(isolationLevel);
            // рандомно называем будущую транзакцию
            txnId = UUID.randomUUID().toString();

            // регистрируем новую транзакцию с данным подключением
            ActiveTransactionRegistry.INSTANCE.register(txnId, conn);
        } catch (RepositoryException e) {
            throw new ServerHTTPException("Transaction start error: " + e.getMessage(), e);
        }
		
		return txnId;
    }

}