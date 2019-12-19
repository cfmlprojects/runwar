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

    public PortRequisitioner add(String name, int socket, boolean enable) {
        Port port = get(name);
        if (port == null) {
            port = new Port(name, socket, defaultHost, enable);
            ports.add(port);
        } else {
            port.socket = socket;
            port.enable = enable;
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

    public PortRequisitioner add(String name, int socket, String host, boolean enable) {
        Port port = get(name);
        if (port == null) {
            port = new Port(name, socket, host, enable);
            ports.add(port);
        } else {
            port.socket = socket;
            port.enable = enable;
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
            if (port.enable) {
                logLine.append(port.host).append(":").append(port.socket).append(" (").append(port.name).append(") ");
            }
        });
        LOG.debug(logLine.toString());
        ports.forEach(port -> {
            if (port.enable) {
                LOG.tracef("Requisitioning port %s:%s (%s)", port.host, port.socket, port.name);
                port.socket(LaunchUtil.getPortOrErrorOut(port.socket, port.host));
                LOG.tracef("Requisitioned port %s:%s (%s)", port.host, port.socket, port.name);
            }
        });
    }

    public class Port {
        public int socket;
        public boolean enable = true;
        public String name, host;

        public Port(String name, int socket, String host, boolean enable) {
            this.socket = socket;
            this.host = host;
            this.name = name;
            this.enable = enable;
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

        public Port enable(boolean enable) {
            enable = enable;
            return this;
        }

        public String toString() {
            return Integer.toString(socket);
        }

    }
}