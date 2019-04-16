package org.cloudbus.cloudsim;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.lists.CloudletList;

public class MinMinDataCenterBroker extends EtcDataCenterBroker {

    public MinMinDataCenterBroker(String name) throws Exception {
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

            double minTaskCompletion = Double.MAX_VALUE;
            int minCloudletId = -1;

            for (Map.Entry<Integer, List<Double>> entry : EtcMatrix.entrySet()) {

                Integer cloudLetId = entry.getKey();
                List<Double> row = entry.getValue();

                for(int j=0; j < row.size(); j++){

                    if(row.get(j) < minTaskCompletion)
                    {
                        vmIndex = j;
                        minTaskCompletion = row.get(j);
                        minCloudletId = cloudLetId;
                    }
                }
            }

            Cloudlet cloudlet = CloudletList.getById(getCloudletList(), minCloudletId);
            vm = getVmsCreatedList().get(vmIndex);
            double newTaskDuration = cloudlet.getCloudletLength() / vm.getMips();

            if (!Log.isDisabled()) {
                Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": Sending cloudlet ",
                        cloudlet.getCloudletId(), " to VM #", vm.getId());
            }

            cloudlet.setVmId(vm.getId());
            sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
            cloudletsSubmitted++;
            vmIndex = (vmIndex + 1) % getVmsCreatedList().size();
            getCloudletSubmittedList().add(cloudlet);
            successfullySubmitted.add(cloudlet);

            if (maximumOfTaskCompletions < minTaskCompletion){
                maximumOfTaskCompletions = minTaskCompletion;
            }

            UpdateEtcMatrixByNewTaskDuration(vmIndex, newTaskDuration);
            DeleteCloudletFromEtcMatrix(minCloudletId);
        }

        // remove submitted cloudlets from waiting list
        getCloudletList().removeAll(successfullySubmitted);
    }
}
