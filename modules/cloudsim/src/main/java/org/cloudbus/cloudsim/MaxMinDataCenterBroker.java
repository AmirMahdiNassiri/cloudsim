package org.cloudbus.cloudsim;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.lists.CloudletList;

public class MaxMinDataCenterBroker extends EtcDataCenterBroker {

    public MaxMinDataCenterBroker(String name) throws Exception {
        super(name);
    }

    protected void submitCloudlets() {

        // Maximum of all task completions e.g. makespan
        double maximumOfTaskCompletions = Double.MIN_VALUE;
        int vmIndex = -1;
        int cloudletCount = getCloudletList().size();

        CreateEtcMatrix(getCloudletList(), getVmsCreatedList());
        List<Cloudlet> successfullySubmitted = new ArrayList<Cloudlet>();

        for (int i = 0; i < cloudletCount; i++){

            Vm vm;

            // Define MaxMin values
            double maxMinTaskCompletion = Double.MIN_VALUE;
            int maxMinCloudletId = -1;

            for (Map.Entry<Integer, List<Double>> entry : EtcMatrix.entrySet()) {

                Integer cloudLetId = entry.getKey();
                List<Double> row = entry.getValue();

                // Find each row minimum value
                double rowMinTaskCompletion = Double.MAX_VALUE;
                int rowMinCloudletId = -1;
                int rowMinVmIndex = -1;

                for(int j=0; j < row.size(); j++){

                    if(row.get(j) < rowMinTaskCompletion)
                    {
                        rowMinVmIndex = j;
                        rowMinTaskCompletion = row.get(j);
                        rowMinCloudletId = cloudLetId;
                    }
                }

                // Change the MaxMin if new Min is bigger than previous
                if (rowMinTaskCompletion > maxMinTaskCompletion){
                    maxMinTaskCompletion = rowMinTaskCompletion;
                    maxMinCloudletId = rowMinCloudletId;
                    vmIndex = rowMinVmIndex;
                }
            }

            Cloudlet cloudlet = CloudletList.getById(getCloudletList(), maxMinCloudletId);
            vm = getVmsCreatedList().get(vmIndex);
            double newTaskDuration = cloudlet.getCloudletLength() / vm.getMips();

            if (!Log.isDisabled()) {
                Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": Sending cloudlet ",
                        cloudlet.getCloudletId(), " to VM #", vm.getId());
            }

            cloudlet.setVmId(vm.getId());
            sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
            cloudletsSubmitted++;
            getCloudletSubmittedList().add(cloudlet);
            successfullySubmitted.add(cloudlet);

            if (maximumOfTaskCompletions < maxMinTaskCompletion){
                maximumOfTaskCompletions = maxMinTaskCompletion;
            }

            UpdateEtcMatrixByNewTaskDuration(vmIndex, newTaskDuration);
            DeleteCloudletFromEtcMatrix(maxMinCloudletId);
        }

        // remove submitted cloudlets from waiting list
        getCloudletList().removeAll(successfullySubmitted);
    }
}
