import javax.swing.*;
import java.awt.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class mainForm extends JFrame {

    private JPanel rootPanel;
    private JTextField txtUserName;
    private JTextField txtUserJobsCount;
    private JTextField txtUserDeadlines;
    private JTextField txtUserBudget;
    private JTextField txtJobOutputSize;
    private JTextField txtJobInputSize;
    private JTextField txtJobLength;
    private JTextField txtJobJobsCount;
    private JTextField txtJobName;
    private JTextField txtJobRequiredMemory;
    private JTextField txtJobRequiredExecution;
    private JTextField txtJobRequiredStorage;
    private JTextField txtDatacenterCount;
    private JTextField txtVmCount;
    private JTextField txtVmCoreMips;
    private JTextArea txtOutput;
    private JButton runSimulationButton;
    private JTextField txtHostCountInDatacenter;
    private JTextField txtHostCoreCount;
    private JTextField txtHostCoreMips;
    private JTextField txtVmCoreCount;
    private JTextField txtHostRam;
    private JTextField txtVmRam;
    private JRadioButton radioBtnMinMin;
    private JRadioButton radioBtnMaxMin;
    private JRadioButton radioBtnGenetic;
    private JRadioButton radioBtnSameVms;
    private JRadioButton radioBtnIndividualVms;
    private JTextArea txtIndividualVmSpecification;
    private JRadioButton radioBtnManualTask;
    private JRadioButton radioBtnTasksFile;
    private JTextField txtTasksFilePath;
    private JRadioButton radioBtnGocjGenerator;
    private JTextField txtGocjOriginalDatasetPath;
    private JTextField txtGocjJobsCount;
    private JTextField txtGeneticTimeConstraint;
    private JTextField txtGeneticTimeConstraintMultiplier;
    private JCheckBox chkBoxTimeConstrainedGenetic;
    private JButton btnResetTimeConstraint;


    private java.util.List<Cloudlet> cloudletList;
    private java.util.List<Vm> vmlist;
    private DatacenterBroker broker;

    // User
    public int UserCount = 1;
    public String UserName = "Amir Mahdi Nassiri";

    // Resources
    public int DatacenterCount = 1;
    public int HostCount = 1;
    public int HostCoreCount = 20; //number of cores (Processing Elements (PE) count)
    public int HostCoreMips = 10000;
    public int HostRam = 16384;
    public long HostStorage = 1000000;
    public int HostBandwidth = 10000;
    public int VmCount = 4;
    public int VmCoreCount = 1;
    public int VmCoreMips = 250;
    public int VmRam = 1024;
    public long VmImageSize = 10000;
    public int VmBandwidth = 100;

    // Tasks
    public int CloudletCount = 10;
    public long CloudletLength = 40000;
    public int CloudletCoreCount = 1;
    public long CloudletFileSize = 300;
    public long CloudletOutputSize = 300;


    public mainForm()
    {
        add(rootPanel);
        setTitle("CloudSim Experiments");
        setSize(400,500);
        setMinimumSize(new Dimension(1000,500));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        runSimulationButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                runSimulation();
            }
        });

        radioBtnIndividualVms.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    txtVmCount.setEnabled(false);
                    txtVmCoreCount.setEnabled(false);
                    txtVmCoreMips.setEnabled(false);
                    txtVmRam.setEnabled(false);
                    txtIndividualVmSpecification.setEnabled(true);
                }
                else if (e.getStateChange() == ItemEvent.DESELECTED) {
                    txtVmCount.setEnabled(true);
                    txtVmCoreCount.setEnabled(true);
                    txtVmCoreMips.setEnabled(true);
                    txtVmRam.setEnabled(true);
                    txtIndividualVmSpecification.setEnabled(false);
                }
            }
        });

        radioBtnManualTask.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    txtJobJobsCount.setEnabled(true);
                    txtJobLength.setEnabled(true);
                }
                else if (e.getStateChange() == ItemEvent.DESELECTED) {
                    txtJobJobsCount.setEnabled(false);
                    txtJobLength.setEnabled(false);
                }
            }
        });

        radioBtnTasksFile.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    txtTasksFilePath.setEnabled(true);
                }
                else if (e.getStateChange() == ItemEvent.DESELECTED) {
                    txtTasksFilePath.setEnabled(false);
                }
            }
        });

        radioBtnGocjGenerator.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    txtGocjOriginalDatasetPath.setEnabled(true);
                    txtGocjJobsCount.setEnabled(true);
                }
                else if (e.getStateChange() == ItemEvent.DESELECTED) {
                    txtGocjOriginalDatasetPath.setEnabled(false);
                    txtGocjJobsCount.setEnabled(false);
                }
            }
        });

        chkBoxTimeConstrainedGenetic.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    txtGeneticTimeConstraint.setEnabled(true);
                    txtGeneticTimeConstraintMultiplier.setEnabled(true);
                }
                else if (e.getStateChange() == ItemEvent.DESELECTED) {
                    txtGeneticTimeConstraint.setEnabled(false);
                    txtGeneticTimeConstraintMultiplier.setEnabled(false);
                }
            }
        });

        btnResetTimeConstraint.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                txtGeneticTimeConstraint.setText("0");
            }
        });

        vmlist = new ArrayList<Vm>();
        cloudletList = new ArrayList<Cloudlet>();

        updateView();

        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    private Datacenter createDatacenter(String name){

        // Here are the steps needed to create a PowerDatacenter:
        // 1. We need to create a list to store our machine
        List<Host> hostList = new ArrayList<Host>();

        for (int i=0; i<HostCount; i++){

            // 2. A Machine contains one or more PEs or CPUs/Cores.
            List<Pe> peList = new ArrayList<Pe>();

            // 3. Create PEs and add these into a list.
            for (int j=0; j<HostCoreCount; j++){
                peList.add(new Pe(j, new PeProvisionerSimple(HostCoreMips)));
            }


            //in this example, the VMAllocatonPolicy in use is SpaceShared. It means that only one VM
            //is allowed to run on each Pe. As each Host has only one Pe, only one VM can run on each Host.
            hostList.add(
                    new Host(
                            i,
                            new RamProvisionerSimple(HostRam),
                            new BwProvisionerSimple(HostBandwidth),
                            HostStorage,
                            peList,
                            new VmSchedulerSpaceShared(peList)
                    )
            );
        }


        // 5. Create a DatacenterCharacteristics object that stores the
        //    properties of a data center: architecture, OS, list of
        //    Machines, allocation policy: time- or space-shared, time zone
        //    and its price (G$/Pe time unit).
        String arch = "x86";      // system architecture
        String os = "Linux";          // operating system
        String vmm = "Xen";
        double time_zone = 10.0;         // time zone this resource located
        double cost = 3.0;              // the cost of using processing in this resource
        double costPerMem = 0.05;		// the cost of using memory in this resource
        double costPerStorage = 0.001;	// the cost of using storage in this resource
        double costPerBw = 0.0;			// the cost of using bw in this resource
        LinkedList<Storage> storageList = new LinkedList<Storage>();	//we are not adding SAN devices by now

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);

        // 6. Finally, we need to create a PowerDatacenter object.
        Datacenter datacenter = null;

        try {
            datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return datacenter;
    }

    private DatacenterBroker createBroker(){

        DatacenterBroker broker = null;

        try {

            if (radioBtnMinMin.isSelected())
                broker = new MinMinDataCenterBroker("MinMinBroker");

            else if (radioBtnMaxMin.isSelected())
                broker = new MaxMinDataCenterBroker("MaxMinBroker");

            else if (radioBtnGenetic.isSelected()){

                GeneticDataCenterBroker geneticBroker = new GeneticDataCenterBroker("GeneticAlgorithmBroker");

                if (chkBoxTimeConstrainedGenetic.isSelected()){

                    long timeConstraint = Long.parseLong(txtGeneticTimeConstraint.getText());
                    timeConstraint *= Long.parseLong(txtGeneticTimeConstraintMultiplier.getText());
                    geneticBroker.setTimeConstrained(timeConstraint);
                }

                broker = geneticBroker;
            }

            else
                broker = new EtcDataCenterBroker("EtcBroker");

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return broker;
    }

    private void createVM(int vmid){

        String vmm = "Xen"; //VMM name

        Vm newVm = new Vm(vmid, broker.getId(), VmCoreMips, VmCoreCount, VmRam, VmBandwidth, VmImageSize, vmm,
                new CloudletSchedulerSpaceShared());

        vmlist.add(newVm);
    }

    private void createVM(int vmid, int cores, int coreMips, int ram){

        String vmm = "Xen"; //VMM name

        Vm newVm = new Vm(vmid, broker.getId(), coreMips, cores, ram, VmBandwidth, VmImageSize, vmm,
                new CloudletSchedulerSpaceShared());

        vmlist.add(newVm);
    }

    private void createCloudlet(int cloudletid){

        UtilizationModel utilizationModel = new UtilizationModelFull();

        Cloudlet newCloudLet = new Cloudlet(cloudletid, CloudletLength, CloudletCoreCount, CloudletFileSize, CloudletOutputSize,
                utilizationModel, utilizationModel, utilizationModel);

        newCloudLet.setUserId(broker.getId());

        cloudletList.add(newCloudLet);

        //broker.bindCloudletToVm(newCloudLet.getCloudletId(),vmid);
    }

    private void createCloudlet(int cloudletid, int cloudletLength){

        UtilizationModel utilizationModel = new UtilizationModelFull();

        Cloudlet newCloudLet = new Cloudlet(cloudletid, cloudletLength, CloudletCoreCount, CloudletFileSize, CloudletOutputSize,
                utilizationModel, utilizationModel, utilizationModel);

        newCloudLet.setUserId(broker.getId());

        cloudletList.add(newCloudLet);

        //broker.bindCloudletToVm(newCloudLet.getCloudletId(),vmid);
    }

    private void runSimulation(){

        updateModel();
        clearOutput();

        writeLineOutput("Initializing Simulation...");

        try {

            CloudSim.init(UserCount, Calendar.getInstance(), false);

            java.util.List<Datacenter> datacenterList = new ArrayList<Datacenter>();

            for (int i = 0; i < DatacenterCount; i++){
                datacenterList.add(createDatacenter("Datacenter_" + i));
            }

            broker = createBroker();

            vmlist.clear();

            if (radioBtnSameVms.isSelected()){
                for (int i = 0; i < VmCount; i++){
                    createVM(i);
                }
            }
            else {

                String specifications = txtIndividualVmSpecification.getText();

                String[] specsList = specifications.split("-");

                int currentVmId=0;

                for (String vmSpec : specsList){

                    String[] spec = vmSpec.split(",");

                    int cores = Integer.parseInt(spec[0]);
                    int coreMips = Integer.parseInt(spec[1]);
                    int ram = Integer.parseInt(spec[2]);

                    createVM(currentVmId, cores, coreMips, ram);

                    currentVmId++;
                }
            }

            broker.submitVmList(vmlist);

            cloudletList.clear();

            if (radioBtnManualTask.isSelected()){

                for (int i = 0; i < CloudletCount; i++){
                    createCloudlet(i);
                }
            }
            else if (radioBtnTasksFile.isSelected()){

                FileReader reader = new FileReader(txtTasksFilePath.getText());
                BufferedReader buffer = new BufferedReader(reader);

                String line;
                int currentCloudletId = 0;

                while ((line = buffer.readLine()) != null){

                    createCloudlet(currentCloudletId, Integer.parseInt(line));
                    currentCloudletId++;
                }

                reader.close();
            }
            else if (radioBtnGocjGenerator.isSelected()){

                long[] gocjJobs = GocjGenerator.createGoCJ(Integer.parseInt(txtGocjJobsCount.getText()),
                        txtGocjOriginalDatasetPath.getText());

                int currentCloudletId = 0;

                for (long cloudletLength : gocjJobs){

                    createCloudlet(currentCloudletId, (int)cloudletLength);
                    currentCloudletId++;
                }
            }

            broker.submitCloudletList(cloudletList);

            CloudSim.startSimulation();

            cloudletList = broker.getCloudletReceivedList();

            CloudSim.stopSimulation();

            printCloudletList();

            long elapsedMillisecondsForScheduling = ((EtcDataCenterBroker)broker).ElapsedMillisecondsForScheduling;

            writeLineOutput("");
            writeLineOutput("Makespan = " + ((EtcDataCenterBroker)broker).MakeSpan);
            writeLineOutput("Elapsed milliseconds for scheduling = " + elapsedMillisecondsForScheduling);
            writeLineOutput("Desired VM count = " + VmCount);
            writeLineOutput("VMs able to create = " + broker.getActualVmsCreatedCount());
            writeLineOutput("");
            writeLineOutput("Simulation Completed");

            if (radioBtnMinMin.isSelected())
            {
                long currentTimeConstraint = Long.parseLong(txtGeneticTimeConstraint.getText());

                if (elapsedMillisecondsForScheduling > currentTimeConstraint)
                    txtGeneticTimeConstraint.setText(Long.toString(elapsedMillisecondsForScheduling));
            }
            else if (radioBtnMaxMin.isSelected())
            {
                long currentTimeConstraint = Long.parseLong(txtGeneticTimeConstraint.getText());

                if (elapsedMillisecondsForScheduling > currentTimeConstraint)
                    txtGeneticTimeConstraint.setText(Long.toString(elapsedMillisecondsForScheduling));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            writeLineOutput("The simulation has been terminated due to an unexpected error");
        }
    }

    private void printCloudletList() throws IOException {

        int size = cloudletList.size();
        Cloudlet cloudlet;

        String indent = "\t";
        writeLineOutput("");
        writeLineOutput("CloudletID" + indent + "STATUS" + indent +
                "DataCenterID" + indent + "VmID" + indent + "Time" + indent + "StartTime" + indent + "FinishTime");

        BufferedWriter writer = new BufferedWriter(new FileWriter("output.txt"));

        DecimalFormat dft = new DecimalFormat("###.##");
        for (int i = 0; i < size; i++) {

            cloudlet = cloudletList.get(i);

            String output = "[";

            writeOutput(cloudlet.getCloudletId() + indent);

            if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS){
                writeOutput("SUCCESS");

                writeLineOutput(indent + cloudlet.getResourceId() + indent + cloudlet.getVmId() +
                        indent + dft.format(cloudlet.getActualCPUTime()) + indent + dft.format(cloudlet.getExecStartTime())+
                        indent + dft.format(cloudlet.getFinishTime()));

                output += cloudlet.getCloudletId() + "," + cloudlet.getResourceId() + ","  + cloudlet.getVmId() + ","  +
                        dft.format(cloudlet.getFinishTime() - cloudlet.getExecStartTime()) + "]";

                if(i < size - 1)
                {
                    output += ",";
                }

                writer.write(output);
            }

            writer.write(System.lineSeparator());
        }

        writer.close();
    }

    private void clearOutput(){
        txtOutput.setText("");
    }

    private void writeOutput(String output){
        txtOutput.setText(txtOutput.getText() + output);
    }

    private void writeLineOutput(String output){
        txtOutput.setText(txtOutput.getText() + output + System.lineSeparator());
    }

    private void updateView(){
        txtUserName.setText(UserName);
        txtDatacenterCount.setText(String.valueOf(DatacenterCount));
        txtHostCountInDatacenter.setText(String.valueOf(HostCount));
        txtHostCoreCount.setText(String.valueOf(HostCoreCount));
        txtHostCoreMips.setText(String.valueOf(HostCoreMips));
        txtHostRam.setText(String.valueOf(HostRam));
        txtVmCount.setText(String.valueOf(VmCount));
        txtVmCoreCount.setText(String.valueOf(VmCoreCount));
        txtVmCoreMips.setText(String.valueOf(VmCoreMips));
        txtVmRam.setText(String.valueOf(VmRam));
        txtJobJobsCount.setText(String.valueOf(CloudletCount));
        txtJobLength.setText(String.valueOf(CloudletLength));
        txtJobInputSize.setText(String.valueOf(CloudletFileSize));
        txtJobOutputSize.setText(String.valueOf(CloudletOutputSize));
    }

    private void updateModel(){
        UserName = txtUserName.getText();
        DatacenterCount = Integer.parseInt(txtDatacenterCount.getText());
        HostCount = Integer.parseInt(txtHostCountInDatacenter.getText());
        HostCoreCount = Integer.parseInt(txtHostCoreCount.getText());
        HostCoreMips = Integer.parseInt(txtHostCoreMips.getText());
        HostRam = Integer.parseInt(txtHostRam.getText());
        VmCount = Integer.parseInt(txtVmCount.getText());
        VmCoreCount = Integer.parseInt(txtVmCoreCount.getText());
        VmCoreMips = Integer.parseInt(txtVmCoreMips.getText());
        VmRam = Integer.parseInt(txtVmRam.getText());
        CloudletCount = Integer.parseInt(txtJobJobsCount.getText());
        CloudletLength = Long.parseLong(txtJobLength.getText());
        CloudletFileSize = Long.parseLong(txtJobInputSize.getText());
        CloudletOutputSize = Long.parseLong(txtJobOutputSize.getText());
    }

}