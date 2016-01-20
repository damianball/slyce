package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.QASession;
import models.User;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;

public class QASession_controller extends Controller {

    public Result index() {
        return ok();
    }

    public Result view(long id) {
        ObjectNode result = Json.newObject();

        QASession session = QASession.find.byId(id);

        // if session exists, return information as JSON
        if (session != null) {
            // TODO: think about a toJSON() method on the models
            ObjectNode jsonQASession = result.putObject("session");
            jsonQASession.put("id", session.id);
            jsonQASession.put("start_time", session.datetimeStart.toString());
            jsonQASession.put("end_time", session.datetimeEnd.toString());

            // since ebeans lazy loads, this should create another db request
            ObjectNode jsonUser = jsonQASession.putObject("host");
            jsonUser.put("id", session.host.id);
            jsonUser.put("name", session.host.name);

            return ok(result);
        } else {
            ObjectNode error = result.putObject("error");
            error.put("Message", "Unable to find QA Session by id: " + id);
            return badRequest(result);
        }
    }

    @BodyParser.Of(BodyParser.Json.class)
    public Result create() {
        JsonNode json = request().body().asJson();

        ObjectNode result = Json.newObject();

        String name = json.findPath("host_name").textValue();
        String start_time = json.findPath("start_time").textValue();
        String end_time = json.findPath("end_time").textValue();

        // checking incoming data
        // null checks
        if(name == null || name.isEmpty()) {
            ObjectNode error = result.putObject("error");
            error.put("Message", "Missing parameter [host_name]");
            return badRequest(result);
        } else if (start_time == null || start_time.isEmpty()) {
            ObjectNode error = result.putObject("error");
            error.put("Message", "Missing or incorrect parameter [start_time], expecting something like: " + DateTime.now(DateTimeZone.UTC));
            return badRequest(result);
        } else if (end_time == null || end_time.isEmpty()) {
            ObjectNode error = result.putObject("error");
            error.put("Message", "Missing parameter [end_time], expecting something like: " + DateTime.now(DateTimeZone.UTC));
            return badRequest(result);
        }

        // parse date strings
        DateTime datetime_start = null;
        DateTime datetime_end = null;

        // attempt to parse
        try {
            datetime_start = DateTime.parse(start_time);
            datetime_end = DateTime.parse(end_time);
        } catch (IllegalArgumentException e) {
            ObjectNode error = result.putObject("error");
            error.put("Message", "Failed to parse start or end time: " + e.getMessage());
            return badRequest(result);
        }

        // get user if exists, create otherwise
        User user = User.getOrCreateUser(name);

        if (user == null) {
            ObjectNode error = result.putObject("error");
            error.put("Message", "Unable to find or create user");
            return internalServerError(result);
        }

        // create the new session object and save it to the db
        QASession session = new QASession();
        session.host = user;
        session.datetimeStart = datetime_start;
        session.datetimeEnd = datetime_end;
        session.save();

        // if save to db failed, error
        if (session.id == null) {
            ObjectNode error = result.putObject("error");
            error.put("Message", "Unable to save new QA session.");
            return internalServerError(result);
        }

        // populate JSON response object with data on new QA session
        // TODO: think about a toJSON() method on the models
        ObjectNode jsonQASession = result.putObject("session");
        jsonQASession.put("id", session.id);
        jsonQASession.put("start_time", session.datetimeStart.toString());
        jsonQASession.put("end_time", session.datetimeEnd.toString());

        ObjectNode jsonUser = jsonQASession.putObject("host");
        jsonUser.put("id", session.host.id);
        jsonUser.put("name", session.host.name);

        return ok(result);
    }
}
