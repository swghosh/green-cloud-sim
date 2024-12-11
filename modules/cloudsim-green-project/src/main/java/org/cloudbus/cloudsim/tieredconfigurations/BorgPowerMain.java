package org.cloudbus.cloudsim.tieredconfigurations;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.visualdata.DatacenterSelectionData;
import org.cloudbus.cloudsim.energyapi.GreenMetricsCalculator;
import org.cloudbus.cloudsim.visualdata.CloudletExecutionData;

import org.cloudbus.cloudsim.workload.CloudletDataParser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Queue;
import org.cloudbus.cloudsim.workload.BorgMapper;
import org.cloudbus.cloudsim.workload.BorgMapper.CloudLet; 

public class BorgPowerMain {
    private static List<Cloudlet> cloudletList;
    private static List<Vm> vmList;
    private static Map<Integer, Vm> vmMap;
    private static int renewableWorkloadsCounter = 0;

    public static void main(String[] args) {
        try {
            // Step 1: Initialize CloudSim
            int numUsers = 1;
            Calendar calendar = Calendar.getInstance();
            boolean traceFlag = false;
            CloudSim.init(numUsers, calendar, traceFlag, 0.742);

            // Step 2: Create fixed datacenters
            DatacenterFactory datacenterFactory = new DatacenterFactory();
            Datacenter lowResDatacenter = datacenterFactory.getLowResDatacenter();

            // Step 3: Load fossil-free percentages
            PowerData initialPowerData = new PowerData(100, 100);
            RealTimeSimulationManager simulationManager = new RealTimeSimulationManager("SimulationManager", initialPowerData, datacenterFactory);
            CloudSim.addEntity(simulationManager);

            // Step 4: Create broker
            DatacenterBroker broker = createBroker();
            int brokerId = broker.getId();

            // Step 5: Initialize VM and Cloudlet lists
            vmList = new ArrayList<>();
            cloudletList = new ArrayList<>();
            vmMap = new HashMap<>();

            // Step 6: Load Borg dataset
            String borgFilePath = "/borg_traces_data.csv";
            int batchSize = 1000;

            try (InputStream inputStream = BorgMapper.class.getResourceAsStream(borgFilePath);
                 BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                List<BorgMapper.BorgDatasetRow> batch = new ArrayList<>();
                while ((line = br.readLine()) != null) {
                    // Parse the line into a BorgDatasetRow object
                    BorgMapper.BorgDatasetRow row = BorgMapper.parseLineToRow(line);

                    if (row != null) {
                        batch.add(row);
                    }

                    if (batch.size() == batchSize) {
                        processBatch(batch, broker, brokerId, lowResDatacenter);
                        batch.clear();
                    }
                }
                // Process any remaining rows
                if (!batch.isEmpty()) {
                    processBatch(batch, broker, brokerId, lowResDatacenter);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            vmList.addAll(vmMap.values());
            
            broker.submitGuestList(vmList);
            broker.submitCloudletList(cloudletList);
            System.out.println("Submitted " + cloudletList.size() + " cloudlets to broker, " + vmList.size() + " VMs to broker");
            // Step 7: Start Simulation
            CloudSim.startSimulation();

//            CloudSim.terminateSimulation(System.currentTimeMillis() + 120000);
            CloudSim.stopSimulation();

            // Step 8: Print Results
            printCloudletResults(broker.getCloudletReceivedList());

            // Step 9: Print Green Metrics
            GreenMetricsCalculator calculator = new GreenMetricsCalculator();
            computeGreenMetrics(calculator, initialPowerData, broker.getCloudletReceivedList());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void computeGreenMetrics(GreenMetricsCalculator calculator, PowerData powerData, List<Cloudlet> cloudlets) {
        // we can use the percentages for intensity, it yields the same result. Since renewable and
        // conventional are percentages of the total Carbon Intensity (gCO2eq/kWh)
        double renewableIntensity = powerData.getRenewablePercentage();
        double conventionalIntensity = 100 - renewableIntensity;
        double totalWorkloads = cloudletList.size();
        double totalWorkloadDuration = 0.742 * totalWorkloads;

        System.out.println("\n=== Green Metrics ===");
        System.out.printf("TOTAL: Total Workloads = %.2f, Total Workload Duration = %.2f%n", totalWorkloads, totalWorkloadDuration);
        System.out.printf("Carbon Emission Reduction: %.2f gCO2%n",
                calculator.calculateCER(conventionalIntensity, renewableIntensity, totalWorkloadDuration));
        System.out.printf("Renewable Energy Utilization: %.2f%%%n",
                calculator.calculateREU(renewableWorkloadsCounter, totalWorkloads));
    }

    private static void processBatch(List<BorgMapper.BorgDatasetRow> batch, DatacenterBroker broker, int brokerId, Datacenter lowResDatacenter) {
        for (BorgMapper.BorgDatasetRow row : batch) {
            // Create VM and Cloudlet from the row
                    // Create CloudLet instance
            Vm vm = null;
            if (vmMap.get(row.instanceIndex) == null) { 
                vm = new Vm(row.instanceIndex, brokerId, row.mips, row.pes, row.memory, 1000, 10000, "Xen", new CloudletSchedulerTimeShared());
                System.out.println("Created VM: " + vm.getId());
                vmMap.put(row.instanceIndex, vm);
            } else {
                vm = vmMap.get(row.instanceIndex);
            }
            Cloudlet cloudlet = new Cloudlet(row.cloudletId, row.cloudletLength, row.pes, row.cloudletFileSize, row.cloudletOutputSize,
                    new UtilizationModelStochastic(), new UtilizationModelStochastic(), new UtilizationModelStochastic());
            cloudlet.setUserId(brokerId);
            cloudlet.setGuestId(vm.getId());
            cloudlet.setSubmissionTime(System.currentTimeMillis()+720);
            System.out.println("Created cloudlet " + cloudlet.getCloudletId() + " for VM " + vm.getId());
            // Cloudlet to the lists
            cloudletList.add(cloudlet);

            if (vm.getHost() != null && vm.getHost().getDatacenter() == lowResDatacenter ) {
                renewableWorkloadsCounter++;
            }
        }


        // Submit VMs and Cloudlets to the broker
        //broker.submitGuestList(vmList);
        //broker.submitCloudletList(cloudletList);
    }

    private static void printCloudletResults(List<Cloudlet> cloudlets) {
        System.out.println("========== OUTPUT ==========");
        System.out.println("Cloudlet ID    STATUS    Datacenter ID    VM ID    Time    Start Time    Finish Time");
        for (Cloudlet cloudlet : cloudlets) {
            if (cloudlet.getStatus() == Cloudlet.CloudletStatus.SUCCESS) {
                System.out.printf("%10d    SUCCESS    %12d    %5d    %4.2f    %9.2f    %10.2f%n",
                        cloudlet.getCloudletId(), cloudlet.getResourceId(), cloudlet.getVmId(),
                        cloudlet.getActualCPUTime(), cloudlet.getExecStartTime(), cloudlet.getFinishTime());
            }
        }

        // Bar/Scatter Plot for Cloudlet Execution Data over Time
        String filePath = "./cloudletExecutionTime.csv";
        CloudletExecutionData.exportCloudletExecutionTime(cloudlets, filePath);
    }

    private static DatacenterBroker createBroker() throws Exception {
        return new DatacenterBroker("Broker");
    }
}
