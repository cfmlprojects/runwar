<!DOCTYPE html>
<html>
<head>
	<meta charset="utf-8">
	<title>Echo WebSocket Example</title>

	<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/prism/1.6.0/themes/prism.min.css" integrity="sha256-vtR0hSWRc3Tb26iuN2oZHt3KRUomwTufNIf5/4oeCyg=" crossorigin="anonymous" />
	<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/prism/1.6.0/themes/prism-okaidia.min.css" integrity="sha256-Xd/oN7fJaAoVq6T+MSWamYuXPoDZ1pT8J4UuOUpOQMs=" crossorigin="anonymous" />

	<style>

		.language-css .token.string, .style .token.string, .token.entity, .token.operator, .token.url, .token.variable { background: transparent; }


		*     { box-sizing: border-box; }
		body  { width: 960px; margin: 1em auto; font-family: sans-serif; font-size: 14px; }
		.code { margin-top: 4em; }
		li                { margin-bottom: 0.25em; padding: 0.25em; color: #333; border-bottom: 1px solid #333; }
		li.message        { background-color: #bfb; }
		li.error          { font-size: 1.25em; background-color: #fbb; }
		li.close, li.open { font-size: 1.25em; background-color: #ff9d33; }
		pre, .pre { white-space: pre-wrap; word-spacing: normal; word-break: normal; word-wrap: normal; }
	</style>
</head>
<body>

<div style="float: left; padding: 0.5em; width: 35%;">
	<h2>Echo Client:</h2>
	<div style="font-size: 1.25em; color: #888;">type a message and hit &lt;Enter&gt;</div>
	<textarea id="client-input" style="margin-top: 1em; width: 100%; height: 8em; font-size: 16px;"></textarea>
</div>
<div style="float: left; padding: 0.5em; width: 65%; border-left: 1px dotted #888;">
	<h2>Echo From Server:</h2>
	<div style="font-size: 1.25em; color: #888;">messages here come back from the server</div>
	<ol id="server-messages"></ol>
</div>
<br style="clear: both;">


<cfset RegisterWebsocket("/lucee-echo", new EchoListener())>

<div class="code">
	<h3>Register the WebSocket endpoint and the Listener Component:</h3>
	<pre><code class="language-javascript">&lt;cfset RegisterWebsocket("/lucee-echo", new EchoListener())&gt;</code></pre>
</div>



<cfset listenerCode = fileRead("./EchoListener.cfc")>

<div class="code">
	<h3>EchoListener.cfc CFScript Code:</h3>
	<pre><code class="language-javascript"><cfset echo(listenerCode)></code></pre>
</div>



<cfsavecontent variable="javascript"><script>
var endpoint = "/lucee-echo";
var wsecho;

connect = function(){

	// create the websocket client and point it to the server's websocket endpoint
	wsecho = new WebSocket("ws://" + document.location.host + endpoint);

	// set an event handler for errors
	wsecho.onerror = function(evt){
		echo(evt);
	};

	// set an event handler for close event
	wsecho.onclose = function(evt){
		echo(evt);
	};

	// set an event handler for incoming messages to log the event to the console
	wsecho.onmessage = function(evt){
		echo(evt);
	};

	// set an event handler for onopen and send the first message only once the connection is opened
	wsecho.onopen = function(evt){
		echo(evt);
		// send a text message to the server
		wsecho.send("Hello WebSocket at " + new Date());
	}
}

/* call connect() when document is ready to create the websocket connection */
// connect();

/* send further messages via the WebSocket's send() method */
// wsecho.send("WebSockets are cool!");
</script>
</cfsavecontent>


<div class="code">
	<h3>WebSocket JavaScript Code:</h3>
	<pre><code class="language-javascript"><cfset echo(javascript)></code></pre>
</div>


<cfset echo(javascript)>

<script>

	var input, output;


	// ready event
	document.addEventListener("DOMContentLoaded", function(evt) {

		input  = document.querySelector("#client-input");
		output = document.querySelector("#server-messages");

		input.addEventListener("keypress", onKeypress);
		input.focus();

		connect();	// connect
	});


	/** client UI below */
	function echo(evt){

		console.log(evt);

		var li  = document.createElement("li");
		li.className = evt.type + " pre";

		var txt;
		if (evt.data){
			var data = JSON.parse(evt.data);
			txt = document.createTextNode(JSON.stringify(data, null, 2));
		}
		else {
			txt = document.createTextNode(evt.type);
		}

		li.appendChild(txt);
		output.insertBefore(li, output.firstChild);
	}


	function onKeypress(evt){

		if (evt.which == 13 || evt.which == 10){

			if (evt.ctrlKey){
				console.log("evt")
				input.value = input.value + '\n';
				return true;
			}
			else if (input.value.length){

				wsecho.send(input.value);
				input.value = "";
				evt.preventDefault();
				return false;
			}
		}
	}
</script>

<script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.6.0/prism.min.js" integrity="sha256-Zb9yKJ/cfs+jG/zIOFL0QEuXr2CDKF7FR5YBJY3No+c=" crossorigin="anonymous"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/prism/1.6.0/components/prism-javascript.min.js" integrity="sha256-c+rbyAMS5uc5kNg/9l6huKo8HTw7mgkWmFrcSbEX3TI=" crossorigin="anonymous"></script>

</body>
</html>