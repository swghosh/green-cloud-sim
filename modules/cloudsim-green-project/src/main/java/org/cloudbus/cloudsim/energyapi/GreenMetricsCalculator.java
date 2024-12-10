package org.cloudbus.cloudsim.energyapi;

public class GreenMetricsCalculator {
    /**
     * Carbon Emission Reduction (CER).
     * Formula: CER = (CarbonIntensityConventional - CarbonIntensityRenewable) * WorkloadDuration
     */
    public double calculateCER(double conventionalIntensity, double renewableIntensity, double workloadDuration) {
        return (conventionalIntensity - renewableIntensity) * workloadDuration;
    }

    /**
     * Energy Efficiency Score (EES).
     * Formula: EES = WorkloadOutput / TotalEnergyConsumed
     * Not very useful actually in this simulation as every tasks get completed
     */
    public double calculateEES(double workloadOutput, double totalEnergyConsumed) {
        return workloadOutput / totalEnergyConsumed;
    }

    /**
     * Renewable Energy Utilization (REU).
     * Formula: REU = (WorkloadsOnRenewableEnergy / TotalWorkloads) * 100
     */
    public double calculateREU(double workloadsOnRenewable, double totalWorkloads) {
        return (workloadsOnRenewable / totalWorkloads) * 100;
    }

    /**
     * Cost-Carbon Tradeoff Score (CCTS).
     * Formula: CCTS = CostSavings + CarbonSavings
     */
    public double calculateCCTS(double costSavings, double carbonSavings) {
        return costSavings + carbonSavings;
    }


    public static void main(String[] args) {
        GreenMetricsCalculator calculator = new GreenMetricsCalculator();
    }
}
