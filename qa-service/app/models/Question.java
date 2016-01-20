package models;

import javax.persistence.*;

import com.avaje.ebean.Model;
import play.data.validation.*;
import org.joda.time.*;

@Entity
public class Question extends Model {
    @Id
    @Constraints.Min(10)
    public Long id;

    @ManyToOne
    @Column(name="qa_session_id")
    public QASession qaSession;

    @ManyToOne
    @Column(name="asked_by")
    @Constraints.Required
    public User askedBy;

    @Constraints.Required
    public String text;

    @Constraints.Required
    @Column(name="datetime_asked")
    public DateTime datetimeAsked;

    // Answer section //
    @ManyToOne
    @Column(name="answered_by")
    public User answeredBy;

    @Column(name="answer_text")
    public String answerText;

    @Column(name="answer_image_url")
    public String answerImageUrl;

    @Column(name="datetime_answered")
    public DateTime datetimeAnswered;

    public static Finder<Long, Question> find = new Finder<Long, Question>(Question.class);
}
