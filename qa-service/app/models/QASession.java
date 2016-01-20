package models;

import javax.persistence.*;

import com.avaje.ebean.Model;
import play.data.validation.*;
import org.joda.time.*;

@Entity
public class QASession extends Model {
    @Id
    @Constraints.Min(10)
    public Long id;

    @ManyToOne
    @Constraints.Required
    public User host;

    @Constraints.Required
    @Column(name="datetime_start")
    public DateTime datetimeStart;

    @Constraints.Required
    @Column(name="datetime_end")
    public DateTime datetimeEnd;

    public static Finder<Long, QASession> find = new Finder<Long, QASession>(QASession.class);
}
