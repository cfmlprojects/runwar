[![Build Status](https://travis-ci.org/Ortus-Solutions/runwar.svg?branch=develop)](https://travis-ci.com/Ortus-Solutions/runwar)

```
██████╗ ██╗   ██╗███╗   ██╗██╗    ██╗ █████╗ ██████╗ 
██╔══██╗██║   ██║████╗  ██║██║    ██║██╔══██╗██╔══██╗
██████╔╝██║   ██║██╔██╗ ██║██║ █╗ ██║███████║██████╔╝
██╔══██╗██║   ██║██║╚██╗██║██║███╗██║██╔══██║██╔══██╗
██║  ██║╚██████╔╝██║ ╚████║╚███╔███╔╝██║  ██║██║  ██║
╚═╝  ╚═╝ ╚═════╝ ╚═╝  ╚═══╝ ╚══╝╚══╝ ╚═╝  ╚═╝╚═╝  ╚═╝
```                                                     


RunWAR is a web server targeted at running Java and CFML applications, with some added features for Adobe ColdFusion and Lucee (configurable web/admin context locations, and opening the admin pages from the taskbar).

## Lightweight

RunWAR has a small memory footprint (10 MB on disk, ~5M min heap), while leveraging enterprise grade server technology-- and the inner workings are simple.  It starts up very fast and has very small overhead.  It's great for running sites on even limited hardware like a Raspberry Pi since it has a smaller footprint than, say, Tomcat.

## Extensible

As small as RunWAR is, it still packs a lot of features.  It can run any J2EE war including all recent versions of Adobe ColdFusion, Railo, and Lucee Server.  It has a powerful Java-based web server built in that provides:

 * URL Rewriting
 * Virtual directories
 * Basic Authencitcation
 * Custom error pages
 * Full control of JVM args
 * System tray integration for managing servers

## Standards Compliant

Runwar is powered under the hood by a project called *Undertow* which actually does all the work.

> Undertow is a flexible performant web server written in java, providing both blocking and non-blocking API’s based on NIO.
> http://undertow.io

Undertow is what powers JBoss WildFly and is a very active project with tons of support for stuff like web sockets and HTTP 2.0 and has excellent performance.  

## Usage

RunWAR can be run from the commandline like so.  This will give you all the available options:
```bash
$> java -jar runwar-${version}.jar
```

To start a quick server, it would look like this:
```bash
$> java -jar runwar.jar -war "path/to/war" --background false
```

or

```bash
$> java -jar runwar.jar -war "path/to/war" --port 8787 --dirs "virtualdir=/path/to/dir,virtualdir2=/path/to/dir2" --background false
```

## Building

To run the build, execute the `gradlew` or `gradlew.bat` gradle wrapper script:

```
./gradlew
```

which will run the `clean` and `shadow` tasks.  Resulting jar is in `./dist/libs/`

To fire up a pre-configured IDE for working on the project, execute the `ide` task (`./gradlew ide`).

## CommandBox
Runwar also powers the servers for CommandBox, a CLI, REPL, package manager, and server for CFML developers.  

## License

Source code for this project is licensed under the [MIT License (MIT)] (http://www.opensource.org/licenses/mit-license.php).


