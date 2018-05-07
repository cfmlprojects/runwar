package runwar.util;

import runwar.LaunchUtil;

import java.util.LinkedHashSet;

import static runwar.logging.RunwarLogger.LOG;

public class PortRequisitioner {
    private LinkedHashSet<Port> ports = new LinkedHashSet<>();
    private String defaultHost = "127.0.0.1";

    public PortRequisitioner() {
    }

    public PortRequisitioner(String defaultHost) {
        this.defaultHost = defaultHost;
    }

    public PortRequisitioner add(String name, int socket) {
        Port port = get(name);
        if (port == null) {
            port = new Port(name, socket, defaultHost);
            ports.add(port);
        } else {
            port.socket = socket;
        }
        return this;
    }

    public PortRequisitioner add(String name, int socket, boolean enabled) {
        Port port = get(name);
        if (port == null) {
            port = new Port(name, socket, defaultHost, enabled);
            ports.add(port);
        } else {
            port.socket = socket;
            port.enabled = enabled;
        }
        return this;
    }

    public PortRequisitioner add(String name, int socket, String host) {
        Port port = get(name);
        if (port == null) {
            port = new Port(name, socket, host);
            ports.add(port);
        } else {
            port.socket = socket;
            port.host = host;
        }
        return this;
    }

    public PortRequisitioner add(String name, int socket, String host, boolean enabled) {
        Port port = get(name);
        if (port == null) {
            port = new Port(name, socket, host, enabled);
            ports.add(port);
        } else {
            port.socket = socket;
            port.enabled = enabled;
            port.host = host;
        }
        return this;
    }

    public Port get(String name) {
        return ports.stream().filter(x -> name.equalsIgnoreCase(x.name)).findAny().orElse(null);
    }

    public Port get(String name, String host) {
        return ports.stream().filter(x -> name.equalsIgnoreCase(x.name) && host.equalsIgnoreCase(x.host)).findAny().orElse(null);
    }

    public void requisition() {
        final StringBuilder logLine = new StringBuilder("Requisitioning ports ");
        ports.forEach(port -> {
            if (port.enabled) {
                logLine.append(port.host).append(":").append(port.socket).append(" (").append(port.name).append(") ");
            }
        });
        LOG.debug(logLine.toString());
        ports.forEach(port -> {
            if (port.enabled) {
                LOG.tracef("Requisitioning port %s:%s (%s)", port.host, port.socket, port.name);
                port.socket(LaunchUtil.getPortOrErrorOut(port.socket, port.host));
                LOG.tracef("Requisitioned port %s:%s (%s)", port.host, port.socket, port.name);
            }
        });
    }

    public class Port {
        public int socket;
        public boolean enabled = true;
        public String name, host;

        public Port(String name, int socket, String host, boolean enabled) {
            this.socket = socket;
            this.host = host;
            this.name = name;
            this.enabled = enabled;
        }

        public Port(String name, int socket, String host) {
            this.socket = socket;
            this.host = host;
            this.name = name;
        }

        public Port name(String string) {
            name = string;
            return this;
        }

        public Port socket(int number) {
            socket = number;
            return this;
        }

        public Port host(String string) {
            host = string;
            return this;
        }

        public Port enabled(boolean enable) {
            enabled = enable;
            return this;
        }

        public String toString() {
            return Integer.toString(socket);
        }

    }
}