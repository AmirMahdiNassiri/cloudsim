package org.cloudbus.cloudsim;

import java.util.*;

import org.apache.commons.lang3.time.StopWatch;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.lists.CloudletList;

public class GeneticDataCenterBroker extends EtcDataCenterBroker {

    Random random = new Random(1000);

    long timeConstraint;
    boolean isTimeConstrained = false;

    List<LinkedHashMap<Integer, Integer>> initialFedChromosomes;

    public GeneticDataCenterBroker(String name) throws Exception {
        super(name);
    }

    protected void submitCloudlets() {

        StopWatch watch = new StopWatch();
        watch.start();

        // Maximum of all task completions e.g. makespan
        double maximumOfTaskCompletions = Double.MIN_VALUE;

        List<? extends Cloudlet> cloudletList = getCloudletList();
        List<? extends Vm> vmList = getVmsCreatedList();

        CreateVmAvailabilityTimes(getVmsCreatedList());
        CreateEtcMatrix(cloudletList, vmList);
        List<Cloudlet> successfullySubmitted = new ArrayList<Cloudlet>();

        LinkedHashMap<Integer, Integer> selectedSolution = getSchedulingByGeneticAlgorithm(cloudletList, vmList);

        for (Map.Entry<Integer, Integer> entry : selectedSolution.entrySet()){

            int cloudLetId = entry.getKey();
            int vmId = entry.getValue();

            double taskCompletionTime = EtcMatrix.get(cloudLetId).get(vmId);

            if (maximumOfTaskCompletions < taskCompletionTime)
            {
                maximumOfTaskCompletions = taskCompletionTime;
            }

            Cloudlet cloudlet = CloudletList.getById(cloudletList, cloudLetId);
            Vm vm = vmList.get(vmId);
            double newTaskDuration = cloudlet.getCloudletLength() / vm.getMips();

            // Update VmAvailabilityTimes
            VmAvailabilityTimes.put(vmId, newTaskDuration + VmAvailabilityTimes.get(vmId));

            if (!Log.isDisabled()) {
                Log.printConcatLine(CloudSim.clock(), ": ", getName(), ": Sending cloudlet ",
                        cloudlet.getCloudletId(), " to VM #", vm.getId());
            }

            cloudlet.setVmId(vm.getId());
            sendNow(getVmsToDatacentersMap().get(vm.getId()), CloudSimTags.CLOUDLET_SUBMIT, cloudlet);
            cloudletsSubmitted++;
            getCloudletSubmittedList().add(cloudlet);
            successfullySubmitted.add(cloudlet);

            UpdateEtcMatrixByNewTaskDuration(vmId, newTaskDuration);
            DeleteCloudletFromEtcMatrix(cloudLetId);
        }

        MakeSpanInSeconds = maximumOfTaskCompletions;

        // remove submitted cloudlets from waiting list
        getCloudletList().removeAll(successfullySubmitted);

        watch.stop();
        ElapsedMillisecondsForScheduling = watch.getTime();
    }

    // TODO: Does VmIds and CloudLetIds start from 0 ? I am using them as simple indices...

    protected LinkedHashMap<Integer, Integer> createRandomChromosome(int cloudletCount, int vmsCount){

        // Create task/vm mappings
        ArrayList<ArrayList<Integer>> orderedMapping = new ArrayList<>();

        for (int i = 0; i < cloudletCount; i++){

            ArrayList<Integer> currentMapping = new ArrayList<>();

            // Add CloudletId
            currentMapping.add(i);
            // Add randomly mapped VmId
            currentMapping.add(random.nextInt(vmsCount));

            orderedMapping.add(currentMapping);
        }

        // Schedule random ordering between mappings
        LinkedHashMap<Integer, Integer> result = new LinkedHashMap<>();

        for (int i = 0; i < cloudletCount; i++){

            // Randomly choose one mapping
            int selectedIndex = random.nextInt(orderedMapping.size());

            ArrayList<Integer> selectedMapping = orderedMapping.get(selectedIndex);

            result.put(selectedMapping.get(0), selectedMapping.get(1));

            orderedMapping.remove(selectedIndex);
        }

        return result;
    }

    protected List<LinkedHashMap<Integer, Integer>> createInitialChromosomes(int count){

        List<LinkedHashMap<Integer, Integer>> result = new LinkedList<>();

        int cloudletCount = getCloudletList().size();
        int vmsCount = getVmsCreatedList().size();

        if (initialFedChromosomes != null){
            result.addAll(initialFedChromosomes);
        }

        for (int i = result.size(); i < count; i++){
            result.add(createRandomChromosome(cloudletCount, vmsCount));
        }

        return result;
    }

    protected void mutateChromosome(LinkedHashMap<Integer, Integer> chromosome, int cloudletCount, int vmsCount){

        int randomlyChosenTask = random.nextInt(cloudletCount);
        int randomlyChosenVm = random.nextInt(vmsCount);

        chromosome.put(randomlyChosenTask, randomlyChosenVm);
    }

    protected LinkedHashMap<Integer, Integer> crossover(LinkedHashMap<Integer, Integer> parent1,
                                                        LinkedHashMap<Integer, Integer> parent2){

        LinkedHashMap<Integer, Integer> child = new LinkedHashMap<>();

        int randomlyChosenIndex = random.nextInt(parent1.size());
        int counter = 0;

        // Add parent1 part
        for (Map.Entry<Integer, Integer> entry : parent1.entrySet()){

            child.put(entry.getKey(), entry.getValue());

            counter++;

            if(randomlyChosenIndex < counter)
                break;
        }

        // Add the remaining from parent2
        for (Map.Entry<Integer, Integer> entry : parent2.entrySet()){

            if (child.containsKey(entry.getKey()))
                continue;

            child.put(entry.getKey(), entry.getValue());
        }

        return child;
    }

    protected int binaryTournamentSelection(List<LinkedHashMap<Integer, Integer>> currentPopulation,
                                            List<Double> fitnessValues){

        if (currentPopulation.size() > 1){

            List<LinkedHashMap<Integer, Integer>> dividedPopulation1 = new ArrayList<>();
            List<LinkedHashMap<Integer, Integer>> dividedPopulation2 = new ArrayList<>();
            List<Double> fitnessValues1 = new ArrayList<>();
            List<Double> fitnessValues2 = new ArrayList<>();

            for (int i = 0; i < currentPopulation.size(); i++){

                if (i < currentPopulation.size() / 2) {
                    dividedPopulation1.add(currentPopulation.get(i));
                    fitnessValues1.add(fitnessValues.get(i));
                }
                else {
                    dividedPopulation2.add(currentPopulation.get(i));
                    fitnessValues2.add(fitnessValues.get(i));
                }
            }

            int winner1Index = binaryTournamentSelection(dividedPopulation1, fitnessValues1);
            int winner2Index = binaryTournamentSelection(dividedPopulation2, fitnessValues2);

            // Each iteration returns index of the whole input
            if (fitnessValues.get(winner1Index) > fitnessValues.get(fitnessValues1.size() + winner2Index))
                return winner1Index;
            else
                return dividedPopulation1.size() + winner2Index;
        }

        return 0;
    }

    protected double fitnessFunction(LinkedHashMap<Integer, Integer> chromosome, List<? extends Cloudlet> cloudletList,
                                     List<? extends Vm> vmList){

        int vmsCount = vmList.size();

        double vmAvailabilityTimes[] = new double[vmsCount];
        Arrays.fill(vmAvailabilityTimes, 0);

        // Maximum of all machine availability times, e.g. makespan
        double maximumOfVmAvailabilityTimes = Double.MIN_VALUE;

        for (Map.Entry<Integer, Integer> mapping : chromosome.entrySet()) {

            int currentCloudletId = mapping.getKey();
            int assignedVmId = mapping.getValue();

            double currentTaskDuration = cloudletList.get(currentCloudletId).getCloudletLength() /
                    vmList.get(assignedVmId).getMips();

            // Update assigned vm availability time
            vmAvailabilityTimes[assignedVmId] += currentTaskDuration;

            if (maximumOfVmAvailabilityTimes < vmAvailabilityTimes[assignedVmId]){
                maximumOfVmAvailabilityTimes = vmAvailabilityTimes[assignedVmId];
            }
        }

        return 1.0/maximumOfVmAvailabilityTimes;
    }

    protected List<Double> fitnessFunction(List<LinkedHashMap<Integer, Integer>> chromosomes,
                                     List<? extends Cloudlet> cloudletList,
                                     List<? extends Vm> vmList){

        LinkedList<Double> result = new LinkedList<>();

        for (int i=0; i<chromosomes.size(); i++){
            result.add(fitnessFunction(chromosomes.get(i), cloudletList, vmList));
        }

        return result;
    }

    protected LinkedHashMap<Integer, Integer> getSchedulingByGeneticAlgorithm(List<? extends Cloudlet> cloudletList,
                                                                              List<? extends Vm> vmList,
                                                                              int steps,
                                                                              int populationCount,
                                                                              int elitesCount,
                                                                              double mutationRate){

        List<LinkedHashMap<Integer, Integer>> population = createInitialChromosomes(populationCount);

        for (int i = 0; i < steps; i++){

            // Calculate fitness values
            List<Double> fitnessValues = fitnessFunction(population, cloudletList, vmList);

            List<LinkedHashMap<Integer, Integer>> eliteChromosomes = new ArrayList<>();

            List<Double> fitnessValuesCopy = new ArrayList<>(fitnessValues);

            for (int e=0; e<elitesCount; e++){

                double eliteFitness = Collections.max(fitnessValuesCopy);
                int eliteIndex = fitnessValuesCopy.indexOf(eliteFitness);
                LinkedHashMap<Integer, Integer> eliteChromosome = population.get(eliteIndex);

                if (i == steps -1)
                {
                    Solution = eliteChromosome;
                    return eliteChromosome;
                }

                eliteChromosomes.add(eliteChromosome);

                fitnessValuesCopy.remove(eliteIndex);
            }

            // Initialize next generation list and put elites in the next generation directly
            List<LinkedHashMap<Integer, Integer>> nextGeneration = new LinkedList<>(eliteChromosomes);

            // Put crossover children
            for (int c=0; c<populationCount - elitesCount; c++){

                LinkedHashMap<Integer, Integer> parent1 = population.get(binaryTournamentSelection(population, fitnessValues));
                LinkedHashMap<Integer, Integer> parent2 = population.get(binaryTournamentSelection(population, fitnessValues));

                LinkedHashMap<Integer, Integer> child = crossover(parent1, parent2);

                if (random.nextDouble() < mutationRate)
                    mutateChromosome(child, cloudletList.size(), vmList.size());

                nextGeneration.add(child);
            }

            assert nextGeneration.size() == populationCount;

            population = nextGeneration;
        }

        throw new UnsupportedOperationException();
    }

    protected LinkedHashMap<Integer, Integer> getSchedulingByGeneticAlgorithm(List<? extends Cloudlet> cloudletList,
                                                                              List<? extends Vm> vmList,
                                                                              long timeConstraint,
                                                                              int populationCount,
                                                                              int elitesCount,
                                                                              double mutationRate){

        StopWatch watch = new StopWatch();
        watch.start();

        List<LinkedHashMap<Integer, Integer>> population = createInitialChromosomes(populationCount);
        LinkedHashMap<Integer, Integer> currentBestChromosome = null;

        long elapsedTime = watch.getTime();

        while(elapsedTime < timeConstraint){

            // Calculate fitness values
            List<Double> fitnessValues = fitnessFunction(population, cloudletList, vmList);

            List<LinkedHashMap<Integer, Integer>> eliteChromosomes = new ArrayList<>();

            List<Double> fitnessValuesCopy = new ArrayList<>(fitnessValues);

            for (int e = 0; e < elitesCount; e++) {

                double eliteFitness = Collections.max(fitnessValuesCopy);
                int eliteIndex = fitnessValuesCopy.indexOf(eliteFitness);
                LinkedHashMap<Integer, Integer> eliteChromosome = population.get(eliteIndex);

                if(e == 0)
                    currentBestChromosome = eliteChromosome;

                eliteChromosomes.add(eliteChromosome);

                fitnessValuesCopy.remove(eliteIndex);
            }

            // Initialize next generation list and put elites in the next generation directly
            List<LinkedHashMap<Integer, Integer>> nextGeneration = new LinkedList<>(eliteChromosomes);

            // Put crossover children
            for (int c = 0; c < populationCount - elitesCount; c++) {

                LinkedHashMap<Integer, Integer> parent1 = population.get(binaryTournamentSelection(population, fitnessValues));
                LinkedHashMap<Integer, Integer> parent2 = population.get(binaryTournamentSelection(population, fitnessValues));

                LinkedHashMap<Integer, Integer> child = crossover(parent1, parent2);

                if (random.nextDouble() < mutationRate)
                    mutateChromosome(child, cloudletList.size(), vmList.size());

                nextGeneration.add(child);
            }

            assert nextGeneration.size() == populationCount;

            population = nextGeneration;

            elapsedTime = watch.getTime();
        }

        Solution = currentBestChromosome;
        return currentBestChromosome;
    }

    protected LinkedHashMap<Integer, Integer> getSchedulingByGeneticAlgorithm(List<? extends Cloudlet> cloudletList,
                                                                              List<? extends Vm> vmList){

        int defaultSteps = 512;
        int defaultPopulationCount = 64;
        int defaultElitesCount = 2;
        double defaultMutationRate = 0.1;

        if (isTimeConstrained){
            return getSchedulingByGeneticAlgorithm(cloudletList, vmList,
                    timeConstraint, defaultPopulationCount, defaultElitesCount, defaultMutationRate);
        }
        else
        {
            return getSchedulingByGeneticAlgorithm(cloudletList, vmList,
                    defaultSteps,defaultPopulationCount, defaultElitesCount, defaultMutationRate);
        }
    }

    public void setTimeConstrained(long constraint) {
        isTimeConstrained = true;
        timeConstraint = constraint;
    }

    public void feedInitialChromosome(LinkedHashMap<Integer, Integer> chromosome){

        if (initialFedChromosomes == null)
        {
            initialFedChromosomes = new ArrayList<>();
        }

        initialFedChromosomes.add(chromosome);
    }
}
