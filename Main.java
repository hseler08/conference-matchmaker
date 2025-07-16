import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

 class Participant {
    private int id;
    private List<String> attributes;
    private List<String> desiredAttributes;

    public Participant(int id, List<String> attributes, List<String> desiredAttributes) {
        this.id = id;
        this.attributes = attributes;
        this.desiredAttributes = desiredAttributes;
    }

    public int getId() { return id; }
    public List<String> getAttributes() { return attributes; }
    public List<String> getDesiredAttributes() { return desiredAttributes; }
}

class Chromosome {
    private List<Participant> matches;
    private int fitnessScore;

    public Chromosome(List<Participant> matches) {
        this.matches = new ArrayList<>(matches);
        this.fitnessScore = 0;
    }

    public List<Participant> getMatches() { return matches; }
    public int getFitnessScore() { return fitnessScore; }
    public void setFitnessScore(int score) { this.fitnessScore = score; }
}

class ConferenceMatcher {
    private List<Participant> participants;
    private static final int POPULATION_SIZE = 13;
    private static final int GENERATIONS = 100;
    private static final double MUTATION_RATE = 0.1;

    public ConferenceMatcher(List<Participant> participants) {
        this.participants = participants;
    }

    public Map<Participant, List<Participant>> matchParticipants() {
        Map<Participant, List<Chromosome>> populations = new HashMap<>();
        Map<Participant, List<Chromosome>> bestChromosomes = new HashMap<>();

        for (Participant participant : participants) {
            List<Chromosome> population = initializePopulation();
            evaluatePopulation(population, participant);
            populations.put(participant, population);
            bestChromosomes.put(participant, findBestChromosomes(population, 3));
        }

        for (int generation = 0; generation < GENERATIONS; generation++) {
            for (Participant participant : participants) {
                List<Chromosome> currentPopulation = populations.get(participant);
                List<Chromosome> newPopulation = new ArrayList<>();

                newPopulation.addAll(bestChromosomes.get(participant));

                while (newPopulation.size() < POPULATION_SIZE) {
                    Chromosome parent1 = selectParent(currentPopulation);
                    Chromosome parent2 = selectParent(currentPopulation);

                    Chromosome child = crossover(parent1, parent2);
                    Chromosome child2 = crossover(parent2, parent1);

                    newPopulation.add(child);
                    newPopulation.add(child2);
                }

                for (Chromosome chromosome : newPopulation) {
                    mutate(chromosome);
                }

                evaluatePopulation(newPopulation, participant);
                bestChromosomes.put(participant, findBestChromosomes(newPopulation, 3));
                populations.put(participant, newPopulation);
            }
        }

        return presentResults(bestChromosomes);
    }

    private List<Chromosome> initializePopulation() {
        List<Chromosome> population = new ArrayList<>();
        for (int i = 0; i < POPULATION_SIZE; i++) {
            List<Participant> randomMatches = new ArrayList<>(participants);
            Collections.shuffle(randomMatches);
            population.add(new Chromosome(randomMatches));
        }
        return population;
    }

    private void evaluatePopulation(List<Chromosome> population, Participant participant) {
        for (Chromosome chromosome : population) {
            int fitnessScore = (int) chromosome.getMatches().stream()
                    .filter(other -> !other.equals(participant) && participant.getDesiredAttributes().stream()
                            .anyMatch(attr -> other.getAttributes().contains(attr)))
                    .count();
            chromosome.setFitnessScore(fitnessScore);
        }
    }

    private Chromosome selectParent(List<Chromosome> population) {
        int totalFitness = 0;
        for (Chromosome chromosome : population) {
            totalFitness += chromosome.getFitnessScore();
        }
        int selectedValue = new Random().nextInt(totalFitness);

        int cumulativeFitness = 0;
        for (Chromosome chromosome : population) {
            cumulativeFitness += chromosome.getFitnessScore();
            if (cumulativeFitness >= selectedValue) {
                return chromosome;
            }
        }
        return population.get(new Random().nextInt(POPULATION_SIZE));
    }

    private Chromosome crossover(Chromosome parent1, Chromosome parent2) {
        List<Participant> matches1 = parent1.getMatches();
        List<Participant> matches2 = parent2.getMatches();

        int splitPoint1 = new Random().nextInt(matches1.size());
        int splitPoint2 = new Random().nextInt(matches2.size());

        List<Participant> childMatches = new ArrayList<>(matches1.subList(0, splitPoint1));
        childMatches.addAll(matches2.subList(splitPoint2, matches2.size()));

        List<Participant> uniqueChildMatches = new ArrayList<>();
        for (Participant participant : childMatches) {
            if (!uniqueChildMatches.contains(participant)) {
                uniqueChildMatches.add(participant);
            }
        }

        return new Chromosome(uniqueChildMatches);
    }

    private void mutate(Chromosome chromosome) {
        if (new Random().nextDouble() < MUTATION_RATE) {
            List<Participant> matches = chromosome.getMatches();
            Random random = new Random();

            Participant selectedParticipant = participants.get(random.nextInt(participants.size()));

            if (matches.contains(selectedParticipant)) {
                matches.remove(selectedParticipant);
            } else {
                matches.add(selectedParticipant);
            }
        }
    }

    private List<Chromosome> findBestChromosomes(List<Chromosome> population, int count) {
        return population.stream()
                .sorted(Comparator.comparingInt(Chromosome::getFitnessScore).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }

    private Map<Participant, List<Participant>> presentResults(Map<Participant, List<Chromosome>> bestSolutions) {
        Map<Participant, List<Participant>> matches = new HashMap<>();

        for (Participant participant : participants) {
            List<Participant> bestMatches = bestSolutions.get(participant).get(0).getMatches();
            List<Participant> potentialMatches = bestMatches.stream()
                    .filter(other -> !other.equals(participant) && participant.getDesiredAttributes().stream()
                            .anyMatch(attr -> other.getAttributes().contains(attr)))
                    .limit(5)
                    .collect(Collectors.toList());

            while (potentialMatches.size() < 5) {
                Participant randomParticipant = bestMatches.get(new Random().nextInt(bestMatches.size()));
                if (!randomParticipant.equals(participant) && !potentialMatches.contains(randomParticipant)) {
                    potentialMatches.add(randomParticipant);
                }
            }

            matches.put(participant, potentialMatches);
        }

        return matches;
    }
}

class InputParser {
    public static List<Participant> parseInputFile(String filePath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath));
        List<Participant> participants = new ArrayList<>();

        for (String line : lines) {
            String[] parts = line.split("\t");
            int id = Integer.parseInt(parts[0]);
            List<String> attributes = Arrays.asList(parts[1].split(","));
            List<String> desiredAttributes = Arrays.asList(parts[2].split(","));
            participants.add(new Participant(id, attributes, desiredAttributes));
        }

        return participants;
    }
}

public class Main {
    public static void main(String[] args) {
        try {
            if(args.length > 0) {
                for(String arg: args) {
                    List<Participant> participants = InputParser.parseInputFile(arg);

                    ConferenceMatcher matcher = new ConferenceMatcher(participants);
                    Map<Participant, List<Participant>> matches = matcher.matchParticipants();

                    for (Participant participant : participants) {
                        System.out.println("Participant " + participant.getId() + " matches:");
                        matches.get(participant).forEach(match -> System.out.println("  - Participant " + match.getId()));
                        System.out.println();
                    }
                }
            }
           else {
               System.out.println("No arguments given");
            }


        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}