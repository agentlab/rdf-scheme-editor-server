package org.eclipse.rdf4j.http.server.repository;

import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.protocol.Protocol.*;
import org.eclipse.rdf4j.http.server.transaction.ActiveTransactionRegistry;
import org.eclipse.rdf4j.http.server.ClientHTTPException;
import org.eclipse.rdf4j.http.server.ProtocolUtil;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import java.util.HashMap;
import java.util.Map;


@Component(service = TransactionControllerSize.class, property = {"osgi.jaxrs.resource=true"})
@Path("/rdf4j2-server")
public class TransactionControllerSize {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public TransactionControllerSize() {
        System.out.println("Init TransactionControllerUpdate");
    }

    @PUT
    @Path("/repositories/{repId}/transactions/{txnId}")
    public void handleRequestInternal(@Context HttpServletRequest request,
                                      @PathParam("repId") String repId,
                                      @PathParam("txnId") String transactionId) throws Exception {
        String reqMethod = request.getMethod();
        logger.debug("transaction id: {}", transactionId);
        logger.debug("request content type: {}", request.getContentType());


        // if no action is specified in the request, it's a rollback (since it's
        // the only txn operation that does not require the action parameter).
        final String actionParam = request.getParameter(Protocol.ACTION_PARAM_NAME);
        final Action action = actionParam != null ? Action.valueOf(actionParam) : Action.ROLLBACK;
        if (action == Action.SIZE) {
            RepositoryConnection connection = ActiveTransactionRegistry.INSTANCE.getTransactionConnection(
                    transactionId);

            if (connection == null) {
                logger.warn("could not find connection for transaction id {}", transactionId);
                throw new WebApplicationException(
                        "unable to find registerd connection for transaction id '" + transactionId + "'", Response.Status.BAD_REQUEST);
            }
            getSize(connection, transactionId, request);
            logger.info("{} txn size request finished", reqMethod);
        } else {
            throw new WebApplicationException("Action not supported: " + action, Response.Status.METHOD_NOT_ALLOWED
                    );
        }
    }

    private void getSize(RepositoryConnection conn, String txnId, HttpServletRequest request
                         )
            throws WebApplicationException, ClientHTTPException {
        try {
            ProtocolUtil.logRequestParameters(request);

            Map<String, Object> model = new HashMap<String, Object>();
//            model.put(ExportStatementsView.HEADERS_ONLY, METHOD_HEAD.equals(request.getMethod()));
            final boolean headersOnly = false;//METHOD_HEAD.equals(request.getMethod());


            if (!headersOnly) {
                Repository repository = RepositoryInterceptor.getRepository(request);

                ValueFactory vf = repository.getValueFactory();
                Resource[] contexts = ProtocolUtil.parseContextParam(request, Protocol.CONTEXT_PARAM_NAME,
                        vf);

                long size = -1;

                try {
                    size = conn.size(contexts);
                } catch (RepositoryException e) {
                    throw new WebApplicationException("Repository error: " + e.getMessage(), e);
                }
//                model.put(SimpleResponseView.CONTENT_KEY, String.valueOf(size));
            }
        } finally {
            ActiveTransactionRegistry.INSTANCE.returnTransactionConnection(txnId);
        }
    }

}
