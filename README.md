#RunWAR

RunWAR is a web server targeted at running CFML applications, with some added 
features for Railo (configurable web/admin context locations, and opening the
Railo admin web page from the taskbar).

It has a small memory footprint (2.5M on disk, ~5M min heap), while leveraging
enterprise grade server technology-- and the inner workings are simple.

It mainly provides a separation of your WAR from your project, without having 
to use CFML mappings, or sub-par workarounds like overriding "/" via a mapping.
Directories which you would like to serve content from can be arbitrary
locations, just pass in a list of file system paths.

There's nothing really amazing about it -- I reckon it breaks the spec
something fierce -- however it is pretty handy for development.

I take that back.  It has a taskbar widget with a configurable icon for starting
and stopping the server instance, and a dock icon for OS X that's configurable 
too-- which _is_ pretty amazing, really.  Quality stuff!  Heh.

*Undertow* actually does all the work-- _"Undertow is a flexible performant
web server written in java, providing both blocking and non-blocking API’s 
based on NIO."_ http://undertow.io/

--which is swell, as it has a small footprint while being featureful,
performant, and configurabuhble in a lovely fashion.

So, yeah, that's mostly it.  Running "java -jar runwar-${version}.jar"
should give you a list of available options, the basics being:

java -jar runwar.jar -war path/to/war --port 8787 --dirs list/of,content/dirs --background false


