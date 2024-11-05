package org.cloudbus.cloudsim.tieredconfigurations;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.core.CloudSim;

public class MyMain {

    public static void main(String[] args) {
        int numJobs = 1000;
        int numVMs = 100;
        int numAZs = 5;

        try {
            // Initialize the CloudSim library
            int numUsers = 1;
            CloudSim.init(numUsers, null, false);

            Datacenter datacenter = DatacenterFactory.createHighResourceDatacenter("first-dc");

            // Example power data, in real scenario this will come from the API
            PowerData powerData = new PowerData();

            // Create broker
            CustomDatacenterBroker broker = new CustomDatacenterBroker("Broker");

           // Create VMs and submit to broker
            List<Vm> vmList = VmAndCloudletCreation.createVMs(0, numVMs);
            broker.submitGuestList(vmList);

            // Create Cloudlets (jobs) and submit to broker
            List<Cloudlet> cloudletList = VmAndCloudletCreation.createCloudlets(0, numJobs);
            broker.submitCloudletList(cloudletList);


            // Start simulation
            CloudSim.startSimulation();
            CloudSim.stopSimulation();


            List<Cloudlet> finishedCloudlets = broker.getCloudletReceivedList();
            optimizeForCO2(finishedCloudlets, vmList);


            printCloudletResults(finishedCloudlets);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void optimizeForCO2(List<Cloudlet> cloudlets, List<Vm> vms) {
        // Sort VMs by ascending CO2 emissions
        List<Vm> sortedVMs = vms.stream()
                .sorted(Comparator.comparingDouble(vm -> getCO2Emissions(vm)))
                .collect(Collectors.toList());

        // Assign cloudlets to VMs in a round-robin fashion, prioritizing low-CO2 VMs
        int vmIndex = 0;
        for (Cloudlet cloudlet : cloudlets) {
            Vm vm = sortedVMs.get(vmIndex);
            cloudlet.setVmId(vm.getId());
            vmIndex = (vmIndex + 1) % sortedVMs.size();
        }
    }

    private static double getCO2Emissions(Vm vm) {
        // Assume there is an API that provides CO2 data for each VM
        return vm.getId() < 50 ? 10.5 : 12.3;
    }

    public static void printCloudletResults(List<Cloudlet> cloudlets) {
        String indent = "    ";
        System.out.println("========== OUTPUT ==========");
        System.out.println("Cloudlet ID" + indent + "STATUS" + indent + "Data center ID" + indent + "VM ID" + indent
                + "Time" + indent + "Start Time" + indent + "Finish Time");

        for (Cloudlet cloudlet : cloudlets) {
            System.out.print(indent + cloudlet.getCloudletId() + indent + indent);

            if (cloudlet.getStatus() == Cloudlet.CloudletStatus.SUCCESS) {
                System.out.println("SUCCESS" + indent + indent + cloudlet.getResourceId() + indent + indent
                        + cloudlet.getVmId() + indent + indent + cloudlet.getActualCPUTime() + indent + indent
                        + cloudlet.getExecStartTime() + indent + indent + cloudlet.getFinishTime());
            }
        }
    }
}