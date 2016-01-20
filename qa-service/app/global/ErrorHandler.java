package global;

import play.http.HttpErrorHandler;
import play.libs.F;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;

public class ErrorHandler implements HttpErrorHandler {
    /**
     * {@inheritDoc}
     */
    @Override
    public F.Promise<Result> onClientError(final Http.RequestHeader requestHeader, final int status, final String message) {
        return F.Promise.pure(Results.status(status));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public F.Promise<Result> onServerError(final Http.RequestHeader requestHeader, final Throwable throwable) {
        return F.Promise.pure(Results.status(500));
    }
}
