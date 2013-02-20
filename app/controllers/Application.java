package controllers;

import play.*;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.mvc.*;

import java.util.*;

import org.apache.commons.collections.map.HashedMap;
import org.codehaus.groovy.util.StringUtil;

import antlr.StringUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import libs.OAuth2;
import libs.OAuth2.Response;
import models.*;


//add some development things now... (in develop branch)

//there is also something changed in master branch ...

public class Application extends Controller {
// one more change in master
	public static OAuth2 QQ = new OAuth2(
			"https://graph.qq.com/oauth2.0/authorize",
			"https://graph.qq.com/oauth2.0/token", "100246727",
			"66ce5746166f2e58f2569cd8e93f3912");

	public static void index() {
		User u = connected();
		Logger.info("User connected: " + u.toString());

		if (u != null && u.access_token != null) {
			home(u.uid);
		}

		render();
	}

	public static void auth(String error, String error_description) {

		if (error_description != null) {
			render(error_description);
		}

		if (OAuth2.isCodeResponse()) {
			User u = connected();
			Response response = QQ.retrieveAccessToken(authURL());
			Logger.info("http content in code reponse:"
					+ response.httpResponse.getString());
			u.access_token = response.accessToken;
			u.save();

			index();
		}

		QQ.retrieveVerificationCode(authURL());
	}
	
	public static void logout()
	{
		session.clear();
		index();
	}

	public static void home(long uid) {
		User u = connected();
		HttpResponse httpResponse = WS.url(
				"https://graph.qq.com/oauth2.0/me?access_token=%s",
				WS.encode(u.access_token)).get();
		String responseString = httpResponse.getString();
		// responseString is like: callback(
		// {"client_id":"100246727","openid":"A3A236146CE6B1F6946EF7A828AB4AEC"}
		// );
		String frontTrimmed = responseString.substring(9);
		String trimmed = frontTrimmed.substring(0, frontTrimmed.length() - 3);
		Logger.info("trimmed response : " + trimmed);

		JsonElement parsed = new JsonParser().parse(trimmed);
		JsonObject me = parsed.getAsJsonObject();
		String openid = me.get("openid").getAsString();
		String appid = Play.configuration
				.getProperty("userconnection.qq.appid");

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("access_token", u.access_token);
		params.put("oauth_consumer_key", appid);
		params.put("openid", openid);
		HttpResponse response2 = WS.url("https://graph.qq.com/user/get_user_info").params(params).get();
		Logger.info("getuserinfo: "+response2.getString());
		JsonObject me2 = response2.getJson().getAsJsonObject();
		String nickname = me2.get("nickname").getAsString();
		render(me2);
	}

	static String authURL() {
		return play.mvc.Router.getFullUrl("Application.auth");
	}

	@Before
	static void setUser() {
		User user = null;
		if (session.contains("uid")) {
			user = User.get(Long.parseLong(session.get("uid")));
		}
		if (user == null) {
			user = User.createNew();
			session.put("uid", user.uid);
		}

		renderArgs.put("user", user);
	}

	public static User connected() {
		return (User) renderArgs.get("user");
	}

}