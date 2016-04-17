package com.github.ashim.json.model;

import java.util.List;

import com.github.ashim.json.annotations.JsonId;
import com.github.ashim.json.annotations.JsonRelation;
import com.github.ashim.json.annotations.JsonType;

/**
 * User Model
 *
 * @author Ashim Jung Khadka
 */
@JsonType("users")
public class User {

	@JsonId
	public Integer id;
	public String name;

	@JsonRelation(value = "roles", included = false)
	private List<Role> roles;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Role> getRoles() {
		return roles;
	}

	public void setRoles(List<Role> roles) {
		this.roles = roles;
	}

	@Override
	public String toString() {
		return "User [id=" + id + ", name=" + name + ", roles=" + roles + "]";
	}

}