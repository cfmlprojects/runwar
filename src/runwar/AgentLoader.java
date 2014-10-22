package runwar;

import java.io.*;
import java.lang.management.*;
import java.util.*;

import org.jboss.logging.Logger;

import com.sun.tools.attach.*;
import com.sun.tools.attach.spi.*;

import sun.tools.attach.*;

final class AgentLoader {
	private static final AttachProvider ATTACH_PROVIDER = new AttachProvider() {
		@Override
		public String name() {
			return null;
		}

		@Override
		public String type() {
			return null;
		}

		@Override
		public VirtualMachine attachVirtualMachine(String id) {
			return null;
		}

		@Override
		public List<VirtualMachineDescriptor> listVirtualMachines() {
			return null;
		}
	};

	private final String jarFilePath;
	private final String pid;
	private static Logger log = Logger.getLogger("RunwarLogger");

	AgentLoader(String jarFilePath) {
		this.jarFilePath = jarFilePath;
		pid = discoverProcessIdForRunningVM();
	}

	private String discoverProcessIdForRunningVM() {
		String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
		int p = nameOfRunningVM.indexOf('@');

		return nameOfRunningVM.substring(0, p);
	}

	boolean loadAgent() {
		VirtualMachine vm;

		if (AttachProvider.providers().isEmpty()) {
			vm = getVirtualMachineImplementationFromEmbeddedOnes();
		} else {
			vm = attachToThisVM();
		}

		if (vm != null) {
			loadAgentAndDetachFromThisVM(vm);
			return true;
		}

		return false;
	}

	private VirtualMachine getVirtualMachineImplementationFromEmbeddedOnes() {
		try {
			if (File.separatorChar == '\\') {
				return new WindowsVirtualMachine(ATTACH_PROVIDER, pid);
			}

			String osName = System.getProperty("os.name");

			if (osName.startsWith("Linux") || osName.startsWith("LINUX")) {
				return new LinuxVirtualMachine(ATTACH_PROVIDER, pid);
			} else if (osName.startsWith("Mac OS X")) {
				return new BsdVirtualMachine(ATTACH_PROVIDER, pid);
			} else if (osName.startsWith("Solaris")) {
				return new SolarisVirtualMachine(ATTACH_PROVIDER, pid);
			}
		} catch (Exception e) {
			log.warn(e.getMessage());
		} catch (UnsatisfiedLinkError e) {
			log.warn(e.getMessage());			
		}
		return null;
	}

	private VirtualMachine attachToThisVM() {
		try {
			return VirtualMachine.attach(pid);
		} catch (Exception e) {
			log.warn(e.getMessage());
		}
		return null;
	}

	private void loadAgentAndDetachFromThisVM(VirtualMachine vm) {
		try {
			vm.loadAgent(jarFilePath, null);
			vm.detach();
		} catch (Exception e) {
			System.err.println("Could not load agent. Error: " + e.getMessage());
		} catch (UnsatisfiedLinkError e) {
			System.err.println("Could not load agent. Error: " + e.getMessage());
		}
		/*
		 * catch (AgentLoadException e) { throw new RuntimeException(e); } catch
		 * (AgentInitializationException e) { throw new RuntimeException(e); }
		 * catch (IOException e) { throw new RuntimeException(e); }
		 */
	}
}
