package org.cloudbus.cloudsim;

import java.util.*;

import org.apache.commons.lang3.time.StopWatch;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.lists.CloudletList;

public class MinMinDataCenterBroker extends EtcDataCenterBroker {

    public MinMinDataCenterBroker(String name) throws Exception {
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

            double minTaskCompletion = Double.MAX_VALUE;
            int minCloudletId = -1;

            for (Map.Entry<Integer, List<Double>> entry : EtcMatrix.entrySet()) {

                Integer cloudLetId = entry.getKey();
                List<Double> row = entry.getValue();

                for(int j=0; j < row.size(); j++){

                    if(row.get(j) < minTaskCompletion)
                    {
                        vmId = j;
                        minTaskCompletion = row.get(j);
                        minCloudletId = cloudLetId;
                    }
                }
            }

            Cloudlet cloudlet = CloudletList.getById(getCloudletList(), minCloudletId);
            vm = getVmsCreatedList().get(vmId);
            double newTaskDuration = cloudlet.getCloudletLength() / vm.getMips();

            // Update VmAvailabilityTimes
            VmAvailabilityTimes.put(vmId, newTaskDuration + VmAvailabilityTimes.get(vmId));

            Solution.put(minCloudletId, vmId);

            if (!Log.isDisabled()) {
                Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": Sending cloudlet ",
                        cloudlet.getCloudletId(), " to VM #", vm.getId());
            }

            cloudlet.setVmId(vm.getId());
            sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
            cloudletsSubmitted++;
            getCloudletSubmittedList().add(cloudlet);
            successfullySubmitted.add(cloudlet);

            if (maximumOfTaskCompletions < minTaskCompletion){
                maximumOfTaskCompletions = minTaskCompletion;
            }

            UpdateEtcMatrixByNewTaskDuration(vmId, newTaskDuration);
            DeleteCloudletFromEtcMatrix(minCloudletId);
        }

        MakeSpanInSeconds = maximumOfTaskCompletions;

        // remove submitted cloudlets from waiting list
        getCloudletList().removeAll(successfullySubmitted);

        watch.stop();
        ElapsedMillisecondsForScheduling = watch.getTime();
    }
}
