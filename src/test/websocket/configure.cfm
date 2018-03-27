<cfscript>

	/* if you do not want to register the websocket in onApplicationStart(), then you can register it from another script */

	RegisterWebsocket("/lucee-echo", new EchoListener());
	echo("<p>Registered Websocket");

</cfscript>