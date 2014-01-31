package org.commonjava.aprox.dotmaven.vertx;

import static org.commonjava.aprox.bind.vertx.util.ResponseUtils.formatResponse;

import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import net.sf.webdav.exceptions.WebdavException;

import org.commonjava.aprox.bind.vertx.boot.MasterRouter;
import org.commonjava.aprox.bind.vertx.util.PathParam;
import org.commonjava.aprox.dotmaven.inject.DotMavenApp;
import org.commonjava.aprox.dotmaven.webctl.DotMavenService;
import org.commonjava.util.logging.Logger;
import org.commonjava.vertx.vabr.Method;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.anno.Routes;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.web.vertx.impl.VertXWebdavRequest;
import org.commonjava.web.vertx.impl.VertXWebdavResponse;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( key = "dotMavenDAV" )
@DotMavenApp
@ApplicationScoped
public class DotMavenHandler
    implements RequestHandler
{

    private final Logger logger = new Logger( getClass() );

    @Inject
    private DotMavenService service;

    // NOTE: /mavdav/ prefix is in the DotMavenRouter.
    @Routes( { @Route( method = Method.ANY, path = ":?path=(/.+)" ) } )
    public void handle( final HttpServerRequest request )
    {
        final String path = request.params()
                                   .get( PathParam.path.key() );
        try
        {
            logger.info( "WebDAV request: '%s'", path );
            service.service( new VertXWebdavRequest( request, MasterRouter.PREFIX, "/mavdav", path, null ),
                             new VertXWebdavResponse( request.response() ) );
        }
        catch ( WebdavException | IOException e )
        {
            logger.error( "Failed to service mavdav request: %s", e, e.getMessage() );
            formatResponse( e, request.response() );
        }
    }

}