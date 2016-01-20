package models;

import javax.persistence.*;

import com.avaje.ebean.Model;
import play.data.validation.*;

import java.util.List;

@Entity
public class User extends Model {
    @Id
    @Constraints.Min(10)
    public Long id;

    @Constraints.Required
    @Column(unique=true)
    public String name;

    public static Finder<Long, User> find = new Finder<Long, User>(User.class);

    public static User getOrCreateUser (String name) {
        // get user if exists, create otherwise
        List<User> users = User.find.where().eq("name", name).findList();

        User user = null;
        // if no user is found, create one
        if (users.isEmpty()) {
            user = new User();
            user.name = name;
            user.save();

            // if save failed, error
            if (user.id == null) {
                return null;
            }

        } else {
            // if a user is found, there can be only one because of a constraint on the db
            user = users.get(0);
        }

        return user;
    }
}
