package org.cloudbus.cloudsim;

import java.util.*;

import org.apache.commons.lang3.time.StopWatch;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.lists.CloudletList;

public class MaxMinDataCenterBroker extends EtcDataCenterBroker {

    public MaxMinDataCenterBroker(String name) throws Exception {
        super(name);
    }

    protected void submitCloudlets() {

        StopWatch watch = new StopWatch();
        watch.start();

        Solution = new LinkedHashMap<>();

        // Maximum of all task completions e.g. makespan
        double maximumOfTaskCompletions = Double.MIN_VALUE;
        int vmId = -1;
        int cloudletCount = getCloudletList().size();

        CreateVmAvailabilityTimes(getVmsCreatedList());
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
                    vmId = rowMinVmIndex;
                }
            }

            Cloudlet cloudlet = CloudletList.getById(getCloudletList(), maxMinCloudletId);
            vm = getVmsCreatedList().get(vmId);
            double newTaskDuration = cloudlet.getCloudletLength() / vm.getMips();

            // Update VmAvailabilityTimes
            VmAvailabilityTimes.put(vmId, newTaskDuration + VmAvailabilityTimes.get(vmId));

            Solution.put(maxMinCloudletId, vmId);

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

            UpdateEtcMatrixByNewTaskDuration(vmId, newTaskDuration);
            DeleteCloudletFromEtcMatrix(maxMinCloudletId);
        }

        MakeSpanInSeconds = maximumOfTaskCompletions;

        // remove submitted cloudlets from waiting list
        getCloudletList().removeAll(successfullySubmitted);

        watch.stop();
        ElapsedMillisecondsForScheduling = watch.getTime();
    }
}
