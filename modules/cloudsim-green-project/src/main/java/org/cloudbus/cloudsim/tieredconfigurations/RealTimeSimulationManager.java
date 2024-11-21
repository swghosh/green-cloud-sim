package org.cloudbus.cloudsim.tieredconfigurations;

import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class RealTimeSimulationManager extends SimEntity {
    protected enum RealTimeSimulationTags implements CloudSimTags {
        CREATE_SIMULATOR, // Tag for creating simulation steps
        UPDATE_DATACENTER // Tag for updating datacenter selection
    }

    private Datacenter currentDatacenter;
    private final PowerData powerData;
    private Queue<Double> fossilFreePercentages; // Holds fossil-free percentages from CSV

    public RealTimeSimulationManager(String name, PowerData initialPowerData) {
        super(name);
        this.powerData = initialPowerData;
        this.fossilFreePercentages = loadFossilFreePercentages("powerBreakdownData.csv");
        System.out.println(fossilFreePercentages);
    }

    // Load fossil-free percentages from the CSV file
    public static Queue<Double> loadFossilFreePercentages(String filePath) {
        Queue<Double> percentages = new LinkedList<>();
        try (InputStream inputStream = RealTimeSimulationManager.class.getClassLoader().getResourceAsStream(filePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            if (inputStream == null) {
                throw new FileNotFoundException("File not found: " + filePath);
            }

            String line;
            boolean isHeader = true;
            while ((line = reader.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }
                String[] columns = line.split(",");
                double fossilFreePercentage = Double.parseDouble(columns[28].trim());
                percentages.add(fossilFreePercentage);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return percentages;
    }


    @Override
    public void startEntity() {
        // Schedule the first update at time 0
        super.startEntity();
        schedule(getId(), 0, RealTimeSimulationTags.CREATE_SIMULATOR);
    }

    @Override
    public void processEvent(SimEvent ev) {
        synchronized (fossilFreePercentages) {
            switch (ev.getTag()) {
                case RealTimeSimulationTags.CREATE_SIMULATOR:
                    // Simulate power data change
                    if (!fossilFreePercentages.isEmpty()) {
                        simulatePowerDataChange();

                        schedule(getId(), 1800, RealTimeSimulationTags.CREATE_SIMULATOR);
                    } else {
                        System.out.println("End of simulation data.");
                        return;
                    }
                    break;

                case RealTimeSimulationTags.UPDATE_DATACENTER:
                    try {
                        // Update datacenter selection based on power data
                        currentDatacenter = PowerMain.selectDatacenterBasedOnPowerData(powerData);
                        System.out.println(CloudSim.clock() + ": Hour " + (float)(CloudSim.clock()/3600) +
                                ": Changed Datacenter (" + currentDatacenter.getName() +
                                ") selection based on fossil-free percentage: " +
                                powerData.getFossilFreePercentage());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;

                default:
                    System.out.println("Unknown event tag: " + ev.getTag());
            }
        }
    }

    private void simulatePowerDataChange() {
        // Simulate fossil-free percentage change for testing based on data gathered over 24h in California
        // Update the fossil-free percentage for the current hour
        if (fossilFreePercentages.isEmpty()) {
            System.out.println("No more data to process. Ending simulation.");
            return;
        }
        double newFossilFreePercentage = fossilFreePercentages.poll();
        powerData.setFossilFreePercentage(newFossilFreePercentage);

        // Queue the datacenter update event
        schedule(getId(), CloudSim.getMinTimeBetweenEvents(), RealTimeSimulationTags.UPDATE_DATACENTER);
    }
    @Override
    public void shutdownEntity() {
        System.out.println("RealTimeSimulationManager is shutting down.");
    }

    public Datacenter getCurrentDatacenter() {
        return currentDatacenter;
    }
}

//    public void stopRealTimeUpdates() {
//        scheduler.shutdown();
//    }

//    private void simulatePowerDataChange() {
//        if (currentHourIndex < fossilFreePercentages.size()) {
//            double newFossilFreePercentage = fossilFreePercentages.get(currentHourIndex);
//            powerData.setFossilFreePercentage(newFossilFreePercentage);
//
//            System.out.println("Simulated Power Data Update: Fossil-Free Percentage = " + newFossilFreePercentage);
//            currentHourIndex++;
//        } else {
//            System.out.println("End of 23-hour simulation data.");
//            stopRealTimeUpdates();
//        }
//    }

/*    // Check every 1 hour
    public void startRealTimeUpdates() {
        scheduler.scheduleAtFixedRate(this::updatePowerDataAndReselectDatacenter, 0, 1, TimeUnit.HOURS);
    }

    private void updatePowerDataAndReselectDatacenter() {
        try {
            // Fetch or simulate new power data
            System.out.println("Updating power data");
            simulatePowerDataChange();
            // double fossilFreePercentage = powerData.getFossilFreePercentage();

            // Reselect the datacenter based on updated power data
            double fossilFreePercentage = powerData.getFossilFreePercentage();
            currentDatacenter = PowerMain.selectDatacenterBasedOnPowerData(powerData);
            System.out.println("Changed Datacenter(" + getCurrentDatacenter().getName() + ") selection based on fossil-free percentage: " + fossilFreePercentage);

        } catch (Exception e) {
            e.printStackTrace();
        }
    } */