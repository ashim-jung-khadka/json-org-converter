package com.github.ashim.json.model;

import com.github.ashim.json.annotations.JsonId;
import com.github.ashim.json.annotations.JsonRelation;
import com.github.ashim.json.annotations.JsonType;

/**
 * Role Model
 *
 * @author Ashim Jung Khadka
 */
@JsonType("roles")
public class Role {

	@JsonId
	private Integer id;
	private String title;

	@JsonRelation("users")
	private User user;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	@Override
	public String toString() {
		return "Role [id=" + id + ", title=" + title + ", user=" + user + "]";
	}

}
