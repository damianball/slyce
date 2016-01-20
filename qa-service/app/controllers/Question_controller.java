package controllers;

import com.avaje.ebean.ExpressionList;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import models.QASession;
import models.Question;
import models.User;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import play.db.ebean.Transactional;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class Question_controller  extends Controller {

    @BodyParser.Of(BodyParser.Json.class)
    public Result ask(Long qaId) {
        JsonNode json = request().body().asJson();

        ObjectNode result = Json.newObject();

        String text = json.findPath("text").textValue();
        String askedBy = json.findPath("asked_by_name").textValue();

        // checking incoming data
        // null checks
        if(qaId == null) {
            ObjectNode error = result.putObject("error");
            error.put("Message", "Missing parameter [qa_id]");
            return badRequest(result);
        } else if (text == null || text.isEmpty()) {
            ObjectNode error = result.putObject("error");
            error.put("Message", "Missing or incorrect parameter [text]");
            return badRequest(result);
        } else if (askedBy == null || askedBy.isEmpty()) {
            ObjectNode error = result.putObject("error");
            error.put("Message", "Missing parameter [asked_by]");
            return badRequest(result);
        }

        // get user if exists, create otherwise
        User user = User.getOrCreateUser(askedBy);

        if (user == null) {
            ObjectNode error = result.putObject("error");
            error.put("Message", "Unable to find or create user");
            return internalServerError(result);
        }

        // get QA Session if exists, error otherwise
        QASession session = QASession.find.byId(qaId);

        if (session == null) {
            ObjectNode error = result.putObject("error");
            error.put("Message", "Unable to find QA session by id: " + qaId);
            return badRequest(result);
        }

        // create new question and save to db
        Question question = new Question();
        question.qaSession = session;
        question.askedBy = user;
        question.text = text;
        question.datetimeAsked = DateTime.now(DateTimeZone.UTC);
        question.save();

        // error if unable to create question
        if (question.id == null) {
            ObjectNode error = result.putObject("error");
            error.put("Message", "Unable to create question");
            return internalServerError(result);
        }

        // TODO: think about a toJSON() method on the models
        ObjectNode jsonQuestion = result.putObject("question");
        jsonQuestion.put("id", question.id);
        jsonQuestion.put("text", question.text);
        jsonQuestion.put("datetime_asked", question.datetimeAsked.toString());

        ObjectNode jsonUser = jsonQuestion.putObject("asked_by");
        jsonUser.put("id", question.askedBy.id);
        jsonUser.put("name", question.askedBy.name);

        return ok(result);
    }

    @BodyParser.Of(BodyParser.Json.class)
    @Transactional
    public Result answer(Long questionId) {
        ObjectNode result = Json.newObject();

        JsonNode json = request().body().asJson();

        // get data from json payload
        String text = json.findPath("text").textValue();
        String imageUrl = json.findPath("image_url").textValue();
        String answeredByName = json.findPath("answered_by_name").textValue();

        // checking incoming data
        // null checks
        if(questionId == null) {
            ObjectNode error = result.putObject("error");
            error.put("Message", "Missing parameter [question_id]");
            return badRequest(result);
        }

        // checking for one value or the other (text or image_url)
        if ((text == null || text.isEmpty()) && (imageUrl == null || imageUrl.isEmpty())) {
            ObjectNode error = result.putObject("error");
            error.put("Message", "Missing parameter [text] or [image_url]");
            return badRequest(result);
        } else if (answeredByName == null || answeredByName.isEmpty()) {
            ObjectNode error = result.putObject("error");
            error.put("Message", "Missing parameter [answered_by_name]");
            return badRequest(result);
        }

        // get Question if exists, error otherwise
        Question question = Question.find.byId(questionId);

        if (question == null) {
            ObjectNode error = result.putObject("error");
            error.put("Message", "Unable to find Question by id: " + questionId);
            return badRequest(result);
        }

        // check to see if the question has already been answered, error if so
        if (question.answeredBy != null) {
            ObjectNode error = result.putObject("error");
            error.put("Message", "Question already answered, id: " + questionId);
            return badRequest(result);
        }

        // get user if exists, create otherwise
        User user = User.getOrCreateUser(answeredByName);

        if (user == null) {
            ObjectNode error = result.putObject("error");
            error.put("Message", "Unable to find or create user: " + answeredByName);
            return internalServerError(result);
        }

        // TODO: potential race condition, should be solved by @Transactional annotation on method, but should investigate / write tests
        question.answerText = text;
        question.answerImageUrl = imageUrl; // TODO: make sure string is in URL format
        question.answeredBy = user;
        question.datetimeAnswered = DateTime.now(DateTimeZone.UTC);
        question.save();

        // test if save succeeded
        // TODO: would prefer a way to know if save failed without querying the object again
        question = Question.find.byId(questionId);

        if (question == null || question.answeredBy == null) {
            ObjectNode error = result.putObject("error");
            error.put("Message", "Issue with saving question");
            return internalServerError(result);
        }

        // assemble resulting JSON
        // TODO: think about a toJSON() method on the models
        ObjectNode jsonQuestion = result.putObject("question");
        jsonQuestion.put("id", question.id);
        jsonQuestion.put("text", question.text);
        jsonQuestion.put("datetime_asked", question.datetimeAsked.toString());

        ObjectNode jsonUser = jsonQuestion.putObject("asked_by");
        jsonUser.put("id", question.askedBy.id);
        jsonUser.put("name", question.askedBy.name);

        if (question.answerText != null && !question.answerText.isEmpty()) jsonQuestion.put("answer_text", question.answerText);
        if (question.answerImageUrl != null && !question.answerImageUrl.isEmpty()) jsonQuestion.put("answer_image_url", question.answerImageUrl);
        // TODO: handle datetime timezone management in one place
        jsonQuestion.put("datetime_answered", question.datetimeAnswered.toDateTime(DateTimeZone.getDefault()).toString());

        ObjectNode jsonUserAnsweredBy = jsonQuestion.putObject("answered_by");
        jsonUserAnsweredBy.put("id", question.answeredBy.id);
        jsonUserAnsweredBy.put("name", question.answeredBy.name);

        return ok(result);
    }

    public Result list(Long qaId) {
        ObjectNode result = Json.newObject();

        // checking incoming data
        // null checks
        if(qaId == null) {
            ObjectNode error = result.putObject("error");
            error.put("Message", "Missing parameter [qa_id]");
            return badRequest(result);
        }

        // get QA Session if exists, error otherwise
        QASession session = QASession.find.byId(qaId);

        if (session == null) {
            ObjectNode error = result.putObject("error");
            error.put("Message", "Unable to find QA session by id: " + qaId);
            return badRequest(result);
        }

        // get filter parameters, if any
        // assumes showAnswered and showNonAnswered is the default state unless otherwise specified by the query string
        // TODO: review the filter approach (isolate, test, etc)
        boolean showAnswered = true;
        boolean showNonAnswered = true;

        final Set<Map.Entry<String,String[]>> entries = request().queryString().entrySet();
        for (Map.Entry<String,String[]> entry : entries) {
            final String key = entry.getKey();
            if (key.equals("answered") || key.equals("non_answered")) {
                final String[] values = entry.getValue();

                // pick the first entry
                // TODO: is this the best way to handle filtering?
                final String value = values[0];

                // input checking
                if (!value.equals("0") && !value.equals("1")) {
                    ObjectNode error = result.putObject("error");
                    error.put("Message", "Unknown filter value for '" + key + " only' filter: " + value);
                    return badRequest(result);
                }

                if (key.equals("answered")) showAnswered = (value.equals("0")) ? false : true;
                if (key.equals("non_answered")) showNonAnswered = (value.equals("0")) ? false : true;
            }
        }

        if (!showNonAnswered && !showAnswered) {
            ObjectNode error = result.putObject("error");
            error.put("Message", "No questions will be both answered and not answered at the same time.");
            return badRequest(result);
        }

        // build question query
        ExpressionList<Question> questionQuery = Question.find.where().eq("qa_session_id", qaId);

        if (!showAnswered) questionQuery.isNull("datetime_answered");
        if (!showNonAnswered) questionQuery.isNotNull("datetime_answered");

        List<Question> questions = questionQuery.findList();

        // assemble resulting JSON
        ArrayNode questionArray = result.putArray("questions");
        for (Question question : questions) {
            // TODO: think about a toJSON() method on the models
            ObjectNode jsonQuestion = questionArray.addObject();
            jsonQuestion.put("id", question.id);
            jsonQuestion.put("text", question.text);
            jsonQuestion.put("datetime_asked", question.datetimeAsked.toString());

            ObjectNode jsonUser = jsonQuestion.putObject("asked_by");
            jsonUser.put("id", question.askedBy.id);
            jsonUser.put("name", question.askedBy.name);

            // if the question was answered, include answer data
            if (question.answeredBy != null) {
                if (question.answerText != null && !question.answerText.isEmpty()) jsonQuestion.put("answer_text", question.answerText);
                if (question.answerImageUrl != null && !question.answerImageUrl.isEmpty()) jsonQuestion.put("answer_image_url", question.answerImageUrl);
                jsonQuestion.put("datetime_answered", question.datetimeAnswered.toString());

                ObjectNode jsonUserAnsweredBy = jsonQuestion.putObject("answered_by");
                jsonUserAnsweredBy.put("id", question.answeredBy.id);
                jsonUserAnsweredBy.put("name", question.answeredBy.name);
            }
        }

        return ok(result);
    }
}
