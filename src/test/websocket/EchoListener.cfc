/**
 * An example Websocket Listener which accepts messages and returns them with some
 * added data from the server.  The events are logged to an array in Session.messages.
 */
component {

	/**
	* event handler that is called before the connection is opened.  returning false or throwing an
	* exception from this event handler will terminate the connection
	*/
	public function onHandshake(serverEndpointConfig, handshakeRequest, handshakeResponse, sessionScope, applicationScope){

		param name="arguments.sessionScope.websocket_events" default=[];
		arguments.sessionScope.websocket_events.prepend({ time: now(), event: "handshake" });

		/* you can reject the connection by throwing an exception or returning false, e.g. */
		// if (!arguments.sessionScope.isAuthenticated) return false;
	}

	/**
	* event handler that is called when the connection is opened, after onHandshake() is called.
	* returning false from this event handler will terminate the connection.
	*/
	public function onOpen(websocket, endpointConfig, sessionScope, applicationScope){

		param name="arguments.sessionScope.websocket_events" default=[];
		arguments.sessionScope.websocket_events.prepend({ time: now(), event: "open" });

		/* you can dump the arguments to a file for further exploration, e.g. */
		// dump(var: arguments, output: getTempDirectory() & createUUID() & ".html");
	}

	/**
	* event handler that is called when a new message is received.  a message can be sent back to
	* the client either by using the websocket object, or by returning a string.
	*/
	public function onMessage(websocket, message, sessionScope, applicationScope){

		param name="arguments.applicationScope.websocket_messages" default=[];
		arguments.applicationScope.websocket_messages.prepend({ time: now(), from: arguments.sessionScope.cfid, message: arguments.message });

		param name="arguments.sessionScope.websocket_events" default=[];
		arguments.sessionScope.websocket_events.prepend({ time: now(), event: "message", message: message });

		return serializeJSON({

			 "from": "Lucee #Server.lucee.version#"
			,"application_name": arguments.applicationScope.applicationName
			,"message": "Echo: #arguments.message#"
			,"received_from": arguments.sessionScope.cfid
			,"server_time": dateTimeFormat(now(), "iso8601")
		});
	}

	/**
	* event handler that is called when the connection is closed
	*/
	public function onClose(websocket, closeReason, sessionScope, applicationScope){

		param name="arguments.sessionScope.websocket_events" default=[];
		arguments.sessionScope.websocket_events.prepend({ time: now(), event: "close" });
	}

	/**
	* event handler that is called when an error occurs
	*/
	public function onError(websocket, exception, sessionScope, applicationScope){

		param name="arguments.sessionScope.websocket_events" default=[];
		arguments.sessionScope.websocket_events.prepend({ time: now(), event: "error" });
	}
}