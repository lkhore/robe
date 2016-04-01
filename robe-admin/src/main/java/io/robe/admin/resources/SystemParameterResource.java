package io.robe.admin.resources;

import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.jersey.PATCH;
import io.robe.admin.hibernate.dao.SystemParameterDao;
import io.robe.admin.hibernate.entity.SystemParameter;
import io.robe.auth.Credentials;
import io.robe.common.utils.FieldReflection;
import org.hibernate.CacheMode;
import org.hibernate.FlushMode;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static org.hibernate.CacheMode.GET;

@Path("systemparameters")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SystemParameterResource {

    @Inject
    private SystemParameterDao systemParameterDao;

    @GET
    @UnitOfWork(readOnly = true, cacheMode = GET, flushMode = FlushMode.MANUAL)
    public List<SystemParameter> getAll(@Auth Credentials credentials) {
        return systemParameterDao.findAll(SystemParameter.class);
    }

    @Path("{id}")
    @GET
    @UnitOfWork(readOnly = true, cacheMode = CacheMode.GET, flushMode = FlushMode.MANUAL)
    public SystemParameter get(@Auth Credentials credentials, @PathParam("id") String id) {

        SystemParameter entity = systemParameterDao.findById(id);
        if (entity == null) {
            throw new WebApplicationException(Response.status(404).build());
        }
        return entity;
    }

    @POST
    @UnitOfWork
    public SystemParameter create(@Auth Credentials credentials, @Valid SystemParameter model) {
        return systemParameterDao.create(model);
    }

    @Path("{id}")
    @PUT
    @UnitOfWork(flushMode = FlushMode.MANUAL)
    public SystemParameter update(@Auth Credentials credentials, @PathParam("id") String id, @Valid SystemParameter model) {
        if (!id.equals(model.getOid())) {
            throw new WebApplicationException(Response.status(412).build());
        }
        SystemParameter entity = systemParameterDao.findById(id);
        systemParameterDao.detach(entity);
        if (entity == null) {
            throw new WebApplicationException(Response.status(404).build());
        }
        return systemParameterDao.update(model);
    }

    @Path("{id}")
    @PATCH
    @UnitOfWork
    public SystemParameter merge(@Auth Credentials credentials, @PathParam("id") String id, SystemParameter model) {

        if (id.equals(model.getOid()))
            throw new WebApplicationException(Response.status(412).build());
        SystemParameter dest = systemParameterDao.findById(id);
        systemParameterDao.detach(dest);
        if (dest == null) {
            throw new WebApplicationException(Response.status(404).build());
        }
        FieldReflection.mergeRight(model, dest);
        return systemParameterDao.update(model);
    }

    @Path("{id}")
    @DELETE
    @UnitOfWork
    public SystemParameter delete(@Auth Credentials credentials, @PathParam("id") String id, @Valid SystemParameter model) {

        if (!id.equals(model.getOid())) {
            throw new WebApplicationException(Response.status(412).build());
        }
        SystemParameter entity = systemParameterDao.findById(id);
        if (entity == null) {
            throw new WebApplicationException(Response.status(404).build());
        }
        return systemParameterDao.delete(entity);
    }

}
