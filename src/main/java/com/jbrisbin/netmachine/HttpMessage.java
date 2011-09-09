package com.jbrisbin.netmachine;

import static com.jbrisbin.netmachine.http.HttpHeaders.*;

import java.net.URI;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public class HttpMessage<M extends HttpMessage<M>> extends BaseMessage<M> {

	protected URI uri;
	protected int contentLength = -1;

	public URI uri() {
		return uri;
	}

	@SuppressWarnings({"unchecked"})
	public M uri(URI uri) {
		this.uri = uri;
		return (M) this;
	}

	public int contentLength() {
		return contentLength;
	}

	@SuppressWarnings({"unchecked"})
	public M contentLength(int contentLength) {
		this.contentLength = contentLength;
		header(CONTENT_LENGTH, "" + contentLength);
		return (M) this;
	}

	public String contentType() {
		return header(CONTENT_TYPE);
	}

	@SuppressWarnings({"unchecked"})
	public M contentType(String contentType) {
		if (null == contentType) {
			headers().remove(CONTENT_TYPE);
		} else {
			header(CONTENT_TYPE, contentType);
		}
		return (M) this;
	}

	@Override public String toString() {
		return ", uri=" + uri +
				", contentLength=" + contentLength +
				super.toString();
	}

}
