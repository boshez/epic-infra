package eventapi.resource;

import eventapi.representation.DatabaseProperties;
import eventapi.representation.Event;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Path("/events")
public class EventResources {
    private DatabaseProperties postgres;
    public EventResources(DatabaseProperties postgres) {
        this.postgres=postgres;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createEvent(Event event) throws SQLException {
        //curl -d '{"name":"name 2","description":"d2","keywords":["k1","k2"]}' 'http://localhost:8080/events' -H "Content-Type: application/json"
        Connection conn = postgres.getConnection();
        Statement stmnt = conn.createStatement();

        event.setNormalizedName(event.getName().replaceAll("\\s+", "-")
                .replaceAll("[^-a-zA-Z0-9]", ""));
        event.setCreated_at();
        event.setStatus("ACTIVE");
        String sql = "INSERT INTO events (name, description, normalized_name, status, created_at) VALUES ( '" + event.getName() + "','"+event.getDescription()+"','"+event.getNormalizedName()+"','"+event.getStatus()+"','"+event.getCreated_at()+"')";
        stmnt.executeUpdate(sql);
        String SQL = "INSERT INTO keywords (event_name,keyword) VALUES (?,?)";
        PreparedStatement statement = conn.prepareStatement(SQL);
        for (String keyword : event.getKeywords()) {
            statement.setString(1, event.getName());
            statement.setString(2, keyword);
            System.out.println(keyword);
            statement.addBatch();
        }
        statement.executeBatch();
        conn.close();
        return Response.ok().build();
    }



    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEvents(@PathParam("id") String normalized_name) throws SQLException {
        Connection conn = postgres.getConnection();
        Statement stmnt = conn.createStatement();
        String sql="";
        if(normalized_name.compareTo("all")!=0){
            sql="SELECT * from events,keywords where name=event_name and normalized_name='"+normalized_name+"';";
        }else{
            sql="SELECT * from events,keywords where name=event_name;";
        }
        System.out.println(sql);
        ResultSet rs = stmnt.executeQuery(sql);
        HashMap<String,Event> eventList=new HashMap<String, Event>();
        while(rs.next()){
            Event e=eventList.get(rs.getString("normalized_name"));
            if(e!=null){
                e.appendKeywords(rs.getString("keyword"));
            }else{
                e=new Event();
                e.setName(rs.getString("name"));
                e.setNormalizedName(rs.getString("normalized_name"));
                e.setDescription(rs.getString("description"));
                e.appendKeywords(rs.getString("keyword"));
                e.setStatus(rs.getString("status"));
                e.setCreated_at(rs.getString("created_at"));
            }
            eventList.put(rs.getString("normalized_name"), e);
        }
        ArrayList<Event> e=new ArrayList<Event>(eventList.values());
        conn.close();
        return Response.ok().entity(e).build();
    }

    @PUT
    @Path("/{id}/{status}")
    public Response setStatus(@PathParam("id") String normalized_name, @PathParam("status") String status) throws SQLException {
        Connection conn=postgres.getConnection();
        String sql="UPDATE events set status = '"+status+"' where normalized_name='"+normalized_name+"';";
        Statement stmt=conn.createStatement();
        System.out.println(sql);
        stmt.execute(sql);
        return Response.ok().build();
    }
}