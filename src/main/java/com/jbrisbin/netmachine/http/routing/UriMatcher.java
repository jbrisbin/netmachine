package com.jbrisbin.netmachine.http.routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class UriMatcher {

	private static final Pattern FULL_SPLAT_PATTERN = Pattern.compile("[\\*][\\*]");
	private static final String FULL_SPLAT_REPLACEMENT = ".*";

	private static final Pattern NAME_SPLAT_PATTERN = Pattern.compile("\\{([^/]+?)\\}[\\*][\\*]");
	private static final String NAME_SPLAT_REPLACEMENT = "(?<%NAME%>.*)";

	private static final Pattern NAME_PATTERN = Pattern.compile("\\{([^/]+?)\\}");
	private static final String NAME_REPLACEMENT = "(?<%NAME%>[^\\/.]*)";

	private Pattern uriPattern;
	private List<String> pathVariables = new ArrayList<>();
	private final HashMap<String, Matcher> matchers = new HashMap<>();
	private final HashMap<String, Map<String, String>> vars = new HashMap<>();

	public UriMatcher(String uriPattern) {
		String s = "^" + uriPattern;

		Matcher m = NAME_SPLAT_PATTERN.matcher(s);
		while (m.find()) {
			for (int i = 1; i <= m.groupCount(); i++) {
				String name = m.group(i);
				pathVariables.add(name);
				s = m.replaceFirst(NAME_SPLAT_REPLACEMENT.replaceAll("%NAME%", name));
				m.reset(s);
			}
		}

		m = NAME_PATTERN.matcher(s);
		while (m.find()) {
			for (int i = 1; i <= m.groupCount(); i++) {
				String name = m.group(i);
				pathVariables.add(name);
				s = m.replaceFirst(NAME_REPLACEMENT.replaceAll("%NAME%", name));
				m.reset(s);
			}
		}

		m = FULL_SPLAT_PATTERN.matcher(s);
		while (m.find()) {
			s = m.replaceAll(FULL_SPLAT_REPLACEMENT);
			m.reset(s);
		}

		this.uriPattern = Pattern.compile(s + "$");
	}

	public Pattern getUriPattern() {
		return uriPattern;
	}

	public boolean matches(String uri) {
		return matcher(uri).matches();
	}

	public Map<String, String> match(String uri) {
		Map<String, String> pathParameters = vars.get(uri);
		if (null != pathParameters) {
			return pathParameters;
		}

		pathParameters = new HashMap<>();
		Matcher m = matcher(uri);
		if (m.matches()) {
			for (String name : pathVariables) {
				String val = m.group(name);
				pathParameters.put(name, val);
			}
		}
		synchronized (vars) {
			vars.put(uri, pathParameters);
		}

		return pathParameters;
	}

	private Matcher matcher(String uri) {
		Matcher m = matchers.get(uri);
		if (null == m) {
			m = uriPattern.matcher(uri);
			synchronized (matchers) {
				matchers.put(uri, m);
			}
		}
		return m;
	}

}
