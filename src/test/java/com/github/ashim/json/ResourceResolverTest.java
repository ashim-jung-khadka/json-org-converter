package com.github.ashim.json;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.github.ashim.json.common.Utility;
import com.github.ashim.json.model.Role;
import com.github.ashim.json.model.User;
import com.github.ashim.json.parser.ResourceResolver;

/**
 * Testing functionality of JSON API converter.
 *
 * @author Ashim Jung Khadka
 */
public class ResourceResolverTest {

	private ResourceResolver resolver;

	@Before
	public void setup() {
		resolver = new ResourceResolver(User.class, Role.class);
	}

	@Test
	public void testReadObject() {

		String jsonResponse = Utility.getJsonAsString("user.json");
		User user = resolver.readJson(jsonResponse.getBytes(), User.class);

		System.out.println(user);
	}

	@Test
	public void testReadCollectionObject() {

		String jsonResponse = Utility.getJsonAsString("users.json");
		List<User> users = resolver.readJsonCollection(jsonResponse.getBytes(), User.class);

		System.out.println(users);
	}

	@Test
	public void testWriteObject() {

		String jsonResponse = Utility.getJsonAsString("user.json");
		User user = resolver.readJson(jsonResponse.getBytes(), User.class);

		String json = resolver.writeJson(user);
		System.out.println(json);
	}

	@Test
	public void testWriteCollectionObject() {

		String jsonResponse = Utility.getJsonAsString("users.json");
		List<User> users = resolver.readJsonCollection(jsonResponse.getBytes(), User.class);

		String json = resolver.writeJsonCollection(users);
		System.out.println(json);
	}

	@Test
	public void testReadObjectWithRelationship() {

		String jsonResponse = Utility.getJsonAsString("user-relationship.json");
		User user = resolver.readJson(jsonResponse.getBytes(), User.class);

		System.out.println(user);

	}

	@Test
	public void testWriteObjectWithRelationship() {

		String jsonResponse = Utility.getJsonAsString("user-relationship.json");
		User user = resolver.readJson(jsonResponse.getBytes(), User.class);

		List<Role> roles = new ArrayList<>();
		Role role1 = new Role();
		role1.setId(1);
		role1.setTitle("ADMIN");
		Role role2 = new Role();
		role2.setId(2);
		role2.setTitle("USER");

		roles.add(role1);
		roles.add(role2);

		user.setRoles(roles);

		String json = resolver.writeJson(user);
		System.out.println(json);
	}
}