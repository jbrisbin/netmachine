package com.jbrisbin.netmachine.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jbrisbin.netmachine.HttpMessage;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class HttpRequest extends HttpMessage<HttpRequest> {

	private Method method;
	private Map<String, List<String>> queryParameters = new HashMap<>();
	private Map<String, String> pathParameters = new HashMap<>();

	public HttpRequest method(Method m) {
		this.method = m;
		return this;
	}

	public Method method() {
		return this.method;
	}

	public HttpRequest pathParameters(Map<String, String> pathParameters) {
		this.pathParameters = pathParameters;
		return this;
	}

	public Map<String, String> pathParameters() {
		return pathParameters;
	}

	public HttpRequest params(Map<String, List<String>> params) {
		queryParameters.putAll(params);
		return this;
	}

	public List<String> params(String name) {
		return queryParameters.get(name);
	}

	public String param(String name) {
		List<String> values = queryParameters.get(name);
		if (null != values && values.size() > 0) {
			return values.get(0);
		} else {
			return null;
		}
	}

	public String paramOr(String name, String defaultVal) {
		String val = param(name);
		if (null == val) {
			return defaultVal;
		} else {
			return val;
		}
	}

	public HttpRequest param(String name, String value) {
		List<String> values = queryParameters.get(name);
		if (null == values) {
			values = new ArrayList<>();
			queryParameters.put(name, values);
		}
		values.add(value);
		return this;
	}

	@Override public String toString() {
		return "HttpRequest{" +
				"method=" + method +
				", pathParameters=" + pathParameters +
				super.toString() +
				'}';
	}

}
