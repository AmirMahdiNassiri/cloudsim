package org.cloudbus.cloudsim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.lists.CloudletList;
import org.cloudbus.cloudsim.lists.VmList;

public class EtcDataCenterBroker extends DatacenterBroker {

    // The calculated ETC matrix
    protected Map<Integer, List<Double>> EtcMatrix;

    public EtcDataCenterBroker(String name) throws Exception {
        super(name);
    }

    protected Map<Integer, List<Double>> CreateEtcMatrix(List<? extends Cloudlet> cloudletList, List<? extends Vm> vmList){

        Map<Integer, List<Double>> matrix = new HashMap<>();

        // Iterating through rows, for each cloudlet (task heterogeneity)
        for (int i = 0; i < cloudletList.size(); i++){

            Cloudlet currentCloudlet = cloudletList.get(i);

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

    protected void UpdateEtcMatrixByNewTaskDuration(int vmIndex, double newTaskDuration){

        for(List<Double> row : EtcMatrix.values()){
            row.set(vmIndex, row.get(vmIndex) + newTaskDuration);
        }
    }

    protected void DeleteCloudletFromEtcMatrix(int cloudletId){

        EtcMatrix.remove(cloudletId);
    }
}
