package ru.agentlab.rdf4j.jaxrs.repository.transaction;

import org.apache.commons.io.IOUtils;
import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.protocol.Protocol.*;
import org.eclipse.rdf4j.http.protocol.error.ErrorInfo;
import org.eclipse.rdf4j.http.protocol.error.ErrorType;

import ru.agentlab.rdf4j.jaxrs.ClientHTTPException;
import ru.agentlab.rdf4j.jaxrs.HTTPException;
import ru.agentlab.rdf4j.jaxrs.ProtocolUtil;
import ru.agentlab.rdf4j.jaxrs.ServerHTTPException;
import ru.agentlab.rdf4j.jaxrs.repository.RepositoryInterceptor;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryInterruptedException;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.UnsupportedQueryLanguageException;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RDFWriterFactory;
import org.eclipse.rdf4j.rio.RDFWriterRegistry;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.agentlab.rdf4j.repository.RepositoryManagerComponent;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.eclipse.rdf4j.http.protocol.Protocol.*;
import static org.eclipse.rdf4j.http.protocol.Protocol.Action.QUERY;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component(service = TransactionController.class, property = { "osgi.jaxrs.resource=true" })
@Path("/rdf4j-server")
public class TransactionController {
    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

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

    @PUT
    @Path("/repositories/{repId}/transactions/{txnId}")
    public void handleRequestInternal(@Context HttpServletRequest request, @Context HttpServletResponse response, @PathParam("repId") String repId, @PathParam("txnId") String transactionId) throws Exception {
        String reqMethod = request.getMethod();
        logger.debug("transaction id: {}", transactionId);
        logger.debug("request content type: {}", request.getContentType());

        // if no action is specified in the request, it's a rollback (since it's
        // the only txn operation that does not require the action parameter).
        final String actionParam = request.getParameter(Protocol.ACTION_PARAM_NAME);
        final Action action = actionParam != null ? Action.valueOf(actionParam) : Action.ROLLBACK;
        if (action == Action.GET) {
            RepositoryConnection connection = ActiveTransactionRegistry.INSTANCE.getTransactionConnection(transactionId);

            if (connection == null) {
                logger.warn("could not find connection for transaction id {}", transactionId);
                throw new WebApplicationException("unable to find registerd connection for transaction id '" + transactionId + "'", Response.Status.BAD_REQUEST);
            }
            getExportStatementsResult(connection, transactionId, request, response);
            logger.info("{} txn size request finished", reqMethod);
        } else if (action == Action.SIZE) {
            RepositoryConnection connection = ActiveTransactionRegistry.INSTANCE.getTransactionConnection(transactionId);

            if (connection == null) {
                logger.warn("could not find connection for transaction id {}", transactionId);
                throw new WebApplicationException("unable to find registerd connection for transaction id '" + transactionId + "'", Response.Status.BAD_REQUEST);
            }
            getSize(connection, transactionId, request);
            logger.info("{} txn size request finished", reqMethod);
        }
        if (action == QUERY) {
            RepositoryConnection connection = ActiveTransactionRegistry.INSTANCE.getTransactionConnection(transactionId);

            if (connection == null) {
                logger.warn("could not find connection for transaction id {}", transactionId);
                throw new WebApplicationException("unable to find registerd connection for transaction id '" + transactionId + "'", Response.Status.BAD_REQUEST);
            }
            processQuery(connection, transactionId, request, response);
        } else {
            throw new WebApplicationException("Action not supported: " + action, Response.Status.METHOD_NOT_ALLOWED);
        }
    }

    private void getExportStatementsResult(RepositoryConnection conn, String txnId, HttpServletRequest request, HttpServletResponse response) throws ClientHTTPException {
        ProtocolUtil.logRequestParameters(request);

        ValueFactory vf = conn.getValueFactory();
        Resource subj = ProtocolUtil.parseResourceParam(request, SUBJECT_PARAM_NAME, vf);
        IRI pred = ProtocolUtil.parseURIParam(request, PREDICATE_PARAM_NAME, vf);
        Value obj = ProtocolUtil.parseValueParam(request, OBJECT_PARAM_NAME, vf);
        Resource[] contexts = ProtocolUtil.parseContextParam(request, CONTEXT_PARAM_NAME, vf);
        boolean useInferencing = ProtocolUtil.parseBooleanParam(request, INCLUDE_INFERRED_PARAM_NAME, true);

        RDFWriterFactory rdfWriterFactory = ProtocolUtil.getAcceptableService(request, response, RDFWriterRegistry.getInstance());

        Map<String, Object> model = new HashMap<String, Object>();
//        model.put(ExportStatementsView.SUBJECT_KEY, subj);
//        model.put(ExportStatementsView.PREDICATE_KEY, pred);
//        model.put(ExportStatementsView.OBJECT_KEY, obj);
//        model.put(ExportStatementsView.CONTEXTS_KEY, contexts);
//        model.put(ExportStatementsView.USE_INFERENCING_KEY, useInferencing);
//        model.put(ExportStatementsView.FACTORY_KEY, rdfWriterFactory);
    }

    private void getSize(RepositoryConnection conn, String txnId, HttpServletRequest request) throws WebApplicationException, ClientHTTPException {
        try {
            ProtocolUtil.logRequestParameters(request);

            Map<String, Object> model = new HashMap<String, Object>();
            // model.put(ExportStatementsView.HEADERS_ONLY,
            // METHOD_HEAD.equals(request.getMethod()));
            final boolean headersOnly = false;// METHOD_HEAD.equals(request.getMethod());

            if (!headersOnly) {
                Repository repository = RepositoryInterceptor.getRepository(request);

                ValueFactory vf = repository.getValueFactory();
                Resource[] contexts = ProtocolUtil.parseContextParam(request, Protocol.CONTEXT_PARAM_NAME, vf);

                long size = -1;

                try {
                    size = conn.size(contexts);
                } catch (RepositoryException e) {
                    throw new WebApplicationException("Repository error: " + e.getMessage(), e);
                }
                // model.put(SimpleResponseView.CONTENT_KEY, String.valueOf(size));
            }
        } finally {
            ActiveTransactionRegistry.INSTANCE.returnTransactionConnection(txnId);
        }
    }

    private void processQuery(RepositoryConnection conn, String txnId, HttpServletRequest request, HttpServletResponse response) throws IOException, WebApplicationException, ClientHTTPException {
        String queryStr = null;
        final String contentType = request.getContentType();
        if (contentType != null && contentType.contains(Protocol.SPARQL_QUERY_MIME_TYPE)) {
            final String encoding = request.getCharacterEncoding() != null ? request.getCharacterEncoding() : "UTF-8";
            queryStr = IOUtils.toString(request.getInputStream(), encoding);
        } else {
            queryStr = request.getParameter(QUERY_PARAM_NAME);
        }
        Query query = getQuery(conn, queryStr, request, response);

        try {
            if (query instanceof TupleQuery) {
                TupleQuery tQuery = (TupleQuery) query;
                tQuery.evaluate();
            } else if (query instanceof GraphQuery) {
                GraphQuery gQuery = (GraphQuery) query;
                gQuery.evaluate();
            } else if (query instanceof BooleanQuery) {
                BooleanQuery bQuery = (BooleanQuery) query;
                bQuery.evaluate();
            } else {
                throw new WebApplicationException("Unsupported query type: " + query.getClass().getName(), Response.Status.BAD_REQUEST);
            }
        } catch (QueryInterruptedException e) {
            logger.info("Query interrupted", e);
            ActiveTransactionRegistry.INSTANCE.returnTransactionConnection(txnId);
            throw new WebApplicationException("Query evaluation took too long", Response.Status.SERVICE_UNAVAILABLE);
        } catch (QueryEvaluationException e) {
            logger.info("Query evaluation error", e);
            ActiveTransactionRegistry.INSTANCE.returnTransactionConnection(txnId);
            if (e.getCause() != null && e.getCause() instanceof HTTPException) {
                throw (WebApplicationException) e.getCause();
            } else {
                throw new WebApplicationException("Query evaluation error: " + e.getMessage());
            }
        }
    }

    private Query getQuery(RepositoryConnection repositoryCon, String queryStr, HttpServletRequest request, HttpServletResponse response) throws IOException, WebApplicationException, ClientHTTPException {
        Query result = null;
        // default query language is SPARQL
        QueryLanguage queryLn = QueryLanguage.SPARQL;
        String queryLnStr = request.getParameter(QUERY_LANGUAGE_PARAM_NAME);
        logger.debug("query language param = {}", queryLnStr);

        if (queryLnStr != null) {
            queryLn = QueryLanguage.valueOf(queryLnStr);
            if (queryLn == null) {
                throw new WebApplicationException("Unknown query language: " + queryLnStr, Response.Status.BAD_REQUEST);
            }
        }
        String baseURI = request.getParameter(Protocol.BASEURI_PARAM_NAME);
        // determine if inferred triples should be included in query evaluation
        boolean includeInferred = ProtocolUtil.parseBooleanParam(request, INCLUDE_INFERRED_PARAM_NAME, true);

        // build a dataset, if specified
        String[] defaultGraphURIs = request.getParameterValues(DEFAULT_GRAPH_PARAM_NAME);
        String[] namedGraphURIs = request.getParameterValues(NAMED_GRAPH_PARAM_NAME);

        SimpleDataset dataset = null;
        if (defaultGraphURIs != null || namedGraphURIs != null) {
            dataset = new SimpleDataset();

            if (defaultGraphURIs != null) {
                for (String defaultGraphURI : defaultGraphURIs) {
                    try {
                        IRI uri = null;
                        if (!"null".equals(defaultGraphURI)) {
                            uri = repositoryCon.getValueFactory().createIRI(defaultGraphURI);
                        }
                        dataset.addDefaultGraph(uri);
                    } catch (IllegalArgumentException e) {
                        throw new WebApplicationException("Illegal URI for default graph: " + defaultGraphURI, Response.Status.BAD_REQUEST);
                    }
                }
            }
            if (namedGraphURIs != null) {
                for (String namedGraphURI : namedGraphURIs) {
                    try {
                        IRI uri = null;
                        if (!"null".equals(namedGraphURI)) {
                            uri = repositoryCon.getValueFactory().createIRI(namedGraphURI);
                        }
                        dataset.addNamedGraph(uri);
                    } catch (IllegalArgumentException e) {
                        throw new WebApplicationException("Illegal URI for named graph: " + namedGraphURI, Response.Status.BAD_REQUEST);
                    }
                }
            }
        }

        try {
            result = repositoryCon.prepareQuery(queryLn, queryStr, baseURI);
            result.setIncludeInferred(includeInferred);
            if (dataset != null) {
                result.setDataset(dataset);
            }

            // determine if any variable bindings have been set on this query.
            @SuppressWarnings("unchecked")
            Enumeration<String> parameterNames = request.getParameterNames();

            while (parameterNames.hasMoreElements()) {
                String parameterName = parameterNames.nextElement();

                if (parameterName.startsWith(BINDING_PREFIX) && parameterName.length() > BINDING_PREFIX.length()) {
                    String bindingName = parameterName.substring(BINDING_PREFIX.length());
                    Value bindingValue = ProtocolUtil.parseValueParam(request, parameterName, repositoryCon.getValueFactory());
                    result.setBinding(bindingName, bindingValue);
                }
            }
        } catch (UnsupportedQueryLanguageException e) {
            ErrorInfo errInfo = new ErrorInfo(ErrorType.UNSUPPORTED_QUERY_LANGUAGE, queryLn.getName());
            throw new WebApplicationException(errInfo.toString(), Response.Status.BAD_REQUEST);
        } catch (MalformedQueryException e) {
            ErrorInfo errInfo = new ErrorInfo(ErrorType.MALFORMED_QUERY, e.getMessage());
            throw new WebApplicationException(errInfo.toString(), Response.Status.BAD_REQUEST);
        } catch (RepositoryException e) {
            logger.error("Repository error", e);
            throw new WebApplicationException("Repository error" + e, Response.Status.INTERNAL_SERVER_ERROR);
        }
        return result;
    }

    @POST
    @Path("/repositories/{repId}/transactions")
    public void handleRequestInternal(@Context HttpServletRequest request, @PathParam("repId") String repId) throws Exception {
        logger.info("POST transaction start");
        Repository repository = repositoryManager.getRepository(repId);
        if (repository == null)
            throw new WebApplicationException("Repository with id=" + repId + " not found", NOT_FOUND);

        startTransaction(repository, request);
        logger.info("transaction started");
    }

    private void startTransaction(Repository repository, HttpServletRequest request) throws ServerHTTPException {
        ProtocolUtil.logRequestParameters(request);

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

        try {
            // получаем подключение к нужному репозиторию
            RepositoryConnection conn = repository.getConnection();

            // настраиваем конфигурации
            ParserConfig config = conn.getParserConfig();
            config.set(BasicParserSettings.PRESERVE_BNODE_IDS, true);
            config.addNonFatalError(BasicParserSettings.VERIFY_DATATYPE_VALUES);
            config.addNonFatalError(BasicParserSettings.VERIFY_LANGUAGE_TAGS);
            // начинаем подключение
            conn.begin(isolationLevel);
            // рандомно называем будущую транзакцию
            String txnId = UUID.randomUUID().toString();

            // регистрируем новую транзакцию с данным подключением
            ActiveTransactionRegistry.INSTANCE.register(txnId, conn);
        } catch (RepositoryException e) {
            throw new ServerHTTPException("Transaction start error: " + e.getMessage(), e);
        }
    }
}