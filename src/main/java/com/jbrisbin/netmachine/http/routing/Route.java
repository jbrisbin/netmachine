package com.jbrisbin.netmachine.http.routing;

import java.util.Map;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class Route<T> {

	private UriMatcher uriMatcher;
	private T resource;

	public Route() {
	}

	public Route(UriMatcher uriMatcher, T resource) {
		this.uriMatcher = uriMatcher;
		this.resource = resource;
	}

	public UriMatcher uriMatcher() {
		return uriMatcher;
	}

	public Route uriMatcher(UriMatcher uriMatcher) {
		this.uriMatcher = uriMatcher;
		return this;
	}

	public T resource() {
		return resource;
	}

	public Route resource(T resource) {
		this.resource = resource;
		return this;
	}

	public boolean matches(String uri) {
		return uriMatcher.matches(uri);
	}

	public Map<String, String> match(String uri) {
		return uriMatcher.match(uri);
	}

}
