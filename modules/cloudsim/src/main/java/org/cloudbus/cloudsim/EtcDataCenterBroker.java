package org.cloudbus.cloudsim;

import java.util.*;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.lists.CloudletList;
import org.cloudbus.cloudsim.lists.VmList;

public class EtcDataCenterBroker extends DatacenterBroker {

    // The calculated ETC matrix
    protected Map<Integer, List<Double>> EtcMatrix;

    protected Map<Integer, Double> VmAvailabilityTimes;

    public double MakeSpanInSeconds;

    public LinkedHashMap<Integer, Integer> Solution;

    public long ElapsedMillisecondsForScheduling;

    public EtcDataCenterBroker(String name) throws Exception {
        super(name);
    }

    protected Map<Integer, List<Double>> CreateEtcMatrix(List<? extends Cloudlet> cloudletList, List<? extends Vm> vmList){

        Map<Integer, List<Double>> matrix = new HashMap<>();

        // Iterating through rows, for each cloudlet (task heterogeneity)
        for (int i = 0; i < cloudletList.size(); i++){

            Cloudlet currentCloudlet = cloudletList.get(i);
            assert i == currentCloudlet.getVmId();

            List<Double> row = new ArrayList<>();

            // Iterating through columns, for each vm (machine heterogeneity)
            for (int j = 0; j < vmList.size(); j++){

                // Adding each estimated time to complete
                row.add(currentCloudlet.getCloudletLength() / vmList.get(j).getMips());
            }

            matrix.put(currentCloudlet.getCloudletId(), row);
        }

        EtcMatrix = matrix;
        return matrix;
    }

    protected Map<Integer, Double> CreateVmAvailabilityTimes (List<? extends Vm> vmList){

        VmAvailabilityTimes = new HashMap<>();

        for (Vm vm : vmList){
            VmAvailabilityTimes.put(vm.getId(), 0.0);
        }

        return VmAvailabilityTimes;
    }

    protected void UpdateEtcMatrixByNewTaskDuration(int vmIndex, double newTaskDuration){

        for(List<Double> row : EtcMatrix.values()){
            row.set(vmIndex, row.get(vmIndex) + newTaskDuration);
        }
    }

    protected void DeleteCloudletFromEtcMatrix(int cloudletId){

        EtcMatrix.remove(cloudletId);
    }

    public double getArur() {

        double sum = 0;

        for (Map.Entry<Integer, Double> entry : VmAvailabilityTimes.entrySet()) {
            sum += entry.getValue();
        }

        sum /= VmAvailabilityTimes.size();

        return sum / MakeSpanInSeconds;
    }
}
