package org.example.app;

import java.util.List;

import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/people")
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class PersonResource {

    @GET
    @Path("{id}")
    public Person getPerson(@PathParam("id") long id) {
        return Person.findById(id);
    }
    
    @POST
    public long createPerson(@QueryParam("first") String firstName, 
                            @QueryParam("last") String lastName) {
      Person p = new Person();
      p.firstName = firstName;
      p.lastName = lastName;
      p.persist();
      return p.id;
    }
    
    @GET
    public List<Person> getByFirst(@QueryParam("firstName") String firstName) {
      return Person.findByName(firstName);
    }
}