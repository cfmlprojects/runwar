component {

	this.name = "WebSocketTest";

	this.sessionManagement = true;
	this.sessionStorage = "Cookie";


	function onApplicationStart() {

		/*
		When you register a websocket listener, some references are cached, e.g. to the web context, application, etc.  if these object are replaced then you should re-register the listener.  It is therefore a good idea to call RegisterWebsocket() from onApplicationStart().

		If you make changes to the listener's code, then you must register it again in order for the changes to take effect.

		If you want to be able to access the listener from you Application then keep a reference to it in the Application scope, e.g.

			Application.objects.echoListener = new EchoListener();
			RegisterWebsocket("/lucee-echo", Application.objects.echoListener);

		the EchoListener has no state of its own, so for this example we just create a new listener, e.g.

			RegisterWebsocket("/lucee-echo", new EchoListener());
		 */

	}

}