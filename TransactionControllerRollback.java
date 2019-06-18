package org.eclipse.rdf4j.http.server.repository;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.eclipse.rdf4j.http.protocol.Protocol;
import org.eclipse.rdf4j.http.protocol.Protocol.Action;
import org.eclipse.rdf4j.http.server.ClientHTTPException;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component(service = TransactionControllerRollback.class, property = {"osgi.jaxrs.resource=true"})
@Path("/rdf4j2-server")
public class TransactionControllerRollback {

    private static Logger logger = LoggerFactory.getLogger(TransactionControllerRollback.class.getName());

    public TransactionControllerRollback() {
        System.out.println("Init TransactionControllerRollback");
    }

    @DELETE
    @Path("/repositories/{repId}/transactions/{txnId}")
    public void handleRequestInternal(@Context HttpServletRequest request,
                                      @PathParam("repId") String repId,
                                      @PathParam("txnId") String transactionId) throws Exception {

        logger.debug("transaction id: {}", transactionId);
        logger.debug("request content type: {}", request.getContentType());
        RepositoryConnection connection = ActiveTransactionRegistry.INSTANCE.getTransactionConnection(
                transactionId);

        if (connection == null) {
            logger.warn("could not find connection for transaction id {}", transactionId);
            throw
                    new WebApplicationException("unable to find registerd connection for transaction id '" + transactionId + "'" , Response.Status.BAD_REQUEST);
        }

        // if no action is specified in the request, it's a rollback (since it's
        // the only txn operation that does not require the action parameter).
        final String actionParam = request.getParameter(Protocol.ACTION_PARAM_NAME);
        final Action action = actionParam != null ? Action.valueOf(actionParam) : Action.ROLLBACK;
        // modification operations - we can process these and then
        // immediately release the connection back to the registry.
        try {
            // TODO Action.ROLLBACK check is for backward compatibility with
            // older 2.8.x releases only. It's not in the protocol spec.
            if (action.equals(Action.ROLLBACK)) {
                logger.info("transaction rollback");
                try {
                    connection.rollback();
                } finally {
                    ActiveTransactionRegistry.INSTANCE.deregister(transactionId);
                    connection.close();
                }
                logger.info("transaction rollback request finished.");
            } else {
                throw new WebApplicationException("Action not supported: " + action, Response.Status.METHOD_NOT_ALLOWED);
                        //ClientHTTPException(HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                       // "Action not supported: " + action);
            }
        } finally {
            ActiveTransactionRegistry.INSTANCE.returnTransactionConnection(transactionId);
        }
    }

}