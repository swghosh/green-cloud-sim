// MainAgain.java
package org.cloudbus.cloudsim.tieredconfigurations;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.*;
import java.util.*;


public class MainAgain {
    public static void main(String[] args) {
        List<Host> hosts = initializeHosts();
        List<VirtualMachine> vms = initializeVMs();
        PowerAwareVMMigration migrationManager = new PowerAwareVMMigration(hosts);

        System.out.println("Starting power-aware VM migration optimization...");
        System.out.println("Initial power consumption: " + calculateTotalPower(hosts) + " watts");

        Map<VirtualMachine, Host> migrationPlan = migrationManager.optimizePlacements(vms);
        executeMigrations(migrationPlan);

        System.out.println("Final power consumption: " + calculateTotalPower(hosts) + " watts");
        printMigrationSummary(migrationPlan);
    }

    private static List<Host> initializeHosts() {
        List<Host> hosts = new ArrayList<>();
        // Create sample hosts with different power profiles
        hosts.add(new Host("Host1", 32, 128, 1000, new PowerProfile(100, 300)));
        hosts.add(new Host("Host2", 64, 256, 2000, new PowerProfile(150, 450)));
        hosts.add(new Host("Host3", 48, 192, 1500, new PowerProfile(120, 350)));
        return hosts;
    }

    private static List<VirtualMachine> initializeVMs() {
        List<VirtualMachine> vms = new ArrayList<>();
        // Create sample VMs with different resource requirements
        vms.add(new VirtualMachine("VM1", 4, 16, 100));
        vms.add(new VirtualMachine("VM2", 8, 32, 200));
        vms.add(new VirtualMachine("VM3", 6, 24, 150));
        vms.add(new VirtualMachine("VM4", 4, 16, 100));
        return vms;
    }

    private static double calculateTotalPower(List<Host> hosts) {
        return hosts.stream()
                   .mapToDouble(Host::getCurrentPowerConsumption)
                   .sum();
    }

    private static void executeMigrations(Map<VirtualMachine, Host> migrationPlan) {
        System.out.println("\nExecuting migrations...");
        migrationPlan.forEach((vm, targetHost) -> {
            System.out.println("Migrating " + vm.getName() + " to " + targetHost.getName());
            // Here you would implement actual migration logic
            // For example:
            // 1. Prepare target host
            // 2. Transfer VM memory pages
            // 3. Handle final memory synchronization
            // 4. Switch execution to target host
            targetHost.addVM(vm);
        });
    }

    private static void printMigrationSummary(Map<VirtualMachine, Host> migrationPlan) {
        System.out.println("\nMigration Summary:");
        migrationPlan.forEach((vm, host) -> {
            System.out.printf("VM: %s -> Host: %s (CPU: %d cores, Memory: %d GB)%n",
                vm.getName(),
                host.getName(),
                vm.getCpuCores(),
                vm.getMemoryGB());
        });
    }

    // Inner classes for reference (these would typically be in separate files)
    static class PowerProfile {
        private final double idlePower;
        private final double maxPower;

        public PowerProfile(double idlePower, double maxPower) {
            this.idlePower = idlePower;
            this.maxPower = maxPower;
        }

        public double calculatePower(double utilization) {
            return idlePower + (maxPower - idlePower) * utilization;
        }
    }

    static class Host {
        private final String name;
        private final int totalCpuCores;
        private final int totalMemoryGB;
        private final int storageGB;
        private final PowerProfile powerProfile;
        private final List<VirtualMachine> vms = new ArrayList<>();

        public Host(String name, int cpuCores, int memoryGB, int storageGB, PowerProfile powerProfile) {
            this.name = name;
            this.totalCpuCores = cpuCores;
            this.totalMemoryGB = memoryGB;
            this.storageGB = storageGB;
            this.powerProfile = powerProfile;
        }

        public String getName() { return name; }
        public void addVM(VirtualMachine vm) { vms.add(vm); }
        public double getCurrentPowerConsumption() {
            double utilization = calculateUtilization();
            return powerProfile.calculatePower(utilization);
        }

        private double calculateUtilization() {
            int usedCPU = vms.stream().mapToInt(VirtualMachine::getCpuCores).sum();
            return (double) usedCPU / totalCpuCores;
        }
    }

    static class VirtualMachine {
        private final String name;
        private final int cpuCores;
        private final int memoryGB;
        private final int storageGB;

        public VirtualMachine(String name, int cpuCores, int memoryGB, int storageGB) {
            this.name = name;
            this.cpuCores = cpuCores;
            this.memoryGB = memoryGB;
            this.storageGB = storageGB;
        }

        public String getName() { return name; }
        public int getCpuCores() { return cpuCores; }
        public int getMemoryGB() { return memoryGB; }
    }
}