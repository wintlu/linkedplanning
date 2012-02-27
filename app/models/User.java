package models;

import javax.persistence.Entity;
import javax.persistence.Id;

import play.db.jpa.Model;

@Entity
public class User extends Model {
	public long uid;
	public String access_token;

	public static User get(long id) {
		return find("uid", id).first();
	}

	public User(long id) {
		this.uid = id;
	}

	public static User createNew() {
		long uid = (long) Math.floor(Math.random() * 10000);
		User user = new User(uid);
		user.create();
		return user;
	}

	@Override
	public String toString() {
		return "uid:" + uid + " token:" + access_token;
	}
}
