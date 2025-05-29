package org.acme;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class GreetingResource {
    private final UserEntityDao userEntityDao;

    @Inject
    public GreetingResource(UserEntityDao userEntityDao) {
        this.userEntityDao = userEntityDao;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{id}")
    public String queryUser(@PathParam("id") String idStr) {
        Long id = Long.parseLong(idStr);
        UserEntity user = userEntityDao.findById(id);
        return "Hello " + user.getName();
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Quarkus REST";
    }
}
