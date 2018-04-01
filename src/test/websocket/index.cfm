<!DOCTYPE html>
<html>
<head>
	<meta charset="utf-8">
	<title>Lucee WebSocket Extension</title>

	<style>

		*     { box-sizing: border-box; }
		body  { width: 960px; margin: 1em auto; font-family: sans-serif; font-size: 14px; }
		.inline-code {
			font-family: monospace;
			font-size: 1.1em;
			margin-top: 1em;
			line-height: 1.5;
			font-weight: bold;
			color: #333;
		}
		ol > li { margin-top: 2em; }
		h2 { margin-top: 2.5em; }
	</style>
</head>
<body>

<h1>Lucee WebSocket Extension</h1>

<h2>Requirements</h2>

<ul>
	<li>Lucee 5.1.1.39 (or newer)</li>
	<li>Java 1.8 (or newer)</li>
	<li>Servlet Container that supports JSR-356, e.g. Tomcat 8, Jetty 9, etc.</li>
</ul>

<h2>Installation</h2>

<p>The extension is packed in a ".lex" (Lucee EXtension) archive, <span>lucee-websocket-extension-x.y.z.lex</span>.  You can install it by visiting the (either Web or Server should work) <a href="/lucee/admin/web.cfm?action=ext.applications" target="_blank">Lucee Admin &gt; Extensions &gt; Applications</a> page, and Uploading the archive from the bottom of the page:

<p><img src="res/upload-extension.png">

<h2>Usage</h2>

<ol>
<li>Create a Listener Component.  The component can have the following event handlers which will be called by the extension as events take place:

	<ul class="api">
		<li>
			<div class="inline-code">onHandshake(serverEndpointConfig, handshakeRequest, handshakeResponse, sessionScope, applicationScope)</div>
			<div>if a boolean false is returned then the connection is rejected</div>
		</li>
		<li>
			<div class="inline-code">onOpen(websocket, endpointConfig, sessionScope, applicationScope)</div>
			<div>if a boolean false is returned then the connection is rejected</div>
		</li>
		<li>
			<div class="inline-code">onMessage(websocket, message, sessionScope, applicationScope)</div>
			<div>if a string is returned then it is sent back to the websocket client as a reply</div>
		</li>
		<li>
			<div class="inline-code">onClose(websocket, closeReason, sessionScope, applicationScope)</div></li>
		<li>
			<div class="inline-code">onError(websocket, exception, sessionScope, applicationScope)</div></li>
	</ul>

	<p>All of the event handlers are optional (though if you do not implement onMessage then you really don't need this extension).

	<p>Your code can interact with the Java objects (websocket, endpointConfig, etc.) directly, or return values as described above.

	<p>For further exploration of the available arguments, dump them to a file from within the event handler and inspect the values, e.g.

	<div class="inline-code">dump(var: arguments, output: getTempDirectory() &amp; createUUID() &amp; ".html");</div>
</li>
<li>Register a WebSocket endpoint and map it to an instance of the Listener Component type by calling the RegisterWebsocket(endpoint, listner) function.  For example, if your listener is of type EchoListener.cfc, and you want it mapped to /lucee-echo, then call:
<p><div class="inline-code">RegisterWebsocket("/lucee-echo", new EchoListener());</div>
</li>
<li>There is no step 3!  You're done.  Well, at least on the server side.  Go and connect to the WebSocket with a client, e.g. a Web Browser.
</li>
</ol>

<h2>Example</h2>

<p><span style="color: red;">First install the extension as described above.</span>  Then check out the example at <a href="websocket-example.cfm">websocket-example.cfm</a>.

<br>
<br>
<br>
<br>
<br>

</body>
</html>