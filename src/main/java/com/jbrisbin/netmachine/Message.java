package com.jbrisbin.netmachine;

import java.nio.channels.AsynchronousByteChannel;
import java.util.List;
import java.util.Map;

import com.jbrisbin.netmachine.io.Buffer;

/**
 * @author Jon Brisbin <jon@jbrisbin.com>
 */
public interface Message<T extends Message> {

	T header(String name, String value, boolean replace);

	T header(String name, String value);

	String header(String name);

	List<String> headers(String name);

	T headers(Map<String, String> headers);

	Map<String, String> headers();

	AsynchronousByteChannel io();

	@SuppressWarnings({"unchecked"}) T completionHandlers(Handler<Void>... completionHandlers);

	T completionHandler(Handler<Void> completionHandler);

	List<Handler<Void>> completionHandlers();

	T readHandler(Handler<Buffer> handler);

	Handler<Buffer> readHandler();

	T writeHandler(WriteHandler<Object> handler);

	WriteHandler<Object> writeHandler();

	<V> T write(V obj);

	<V> T write(V obj, Handler<Void> completionHandler);

	T replyHandler(Handler<Message> reply);

	T reply(Message reply);

	T complete();

}
