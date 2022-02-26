package com.paradigma.rt.streaming.fraudsimulator.business;

import com.paradigma.rt.streaming.fraudsimulator.business.ports.eventbus.EventBusPort;
import com.paradigma.rt.streaming.fraudsimulator.model.SimulationData;
import com.paradigma.rt.streaming.fraudsimulator.model.SimulationDataResults;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@ApplicationScoped
public class FraudSimulator {

    public static final int FRAUDS_MOVEMENTS_PER_CARD = 5;
    private final Logger logger = Logger.getLogger(FraudSimulator.class);

    @Inject
    EventBusPort eventBusPort;

    private ExecutorService executor;

    public static int calculatePotentialFraudsMovements(int numCards, int iterations) {
        return numCards * iterations * FRAUDS_MOVEMENTS_PER_CARD;
    }

    public FraudSimulator() {
        this.executor = Executors.newFixedThreadPool(10, r -> new Thread(r, "executors"));
    }

    public long createFraudSimulationProcess(SimulationData simulationData) {
        long id = new Date().getTime();
        simulationData.setId(id);

        this.logger.info("[Business] Starting fraud simulation process.....ID: " + simulationData.getId());
        this.eventBusPort.publishSimulationData(simulationData);

        return id;
    }

    public int executeFraudSimulationProcess(SimulationData simulationData) {
        this.logger.info("[Business] Executing fraud simulation process.....ID: " + simulationData.getId());

        int totalMovementsCreated = this.runFraudSimulation(simulationData, this.generateCardsList(simulationData.getCards()));
        this.publishSimulationDataResults(simulationData, totalMovementsCreated);

        return totalMovementsCreated;
    }

    private List<String> generateCardsList(int numCards) {
        List<String> cards = new ArrayList<>();
        for (int i = 0; i < numCards; i++) {
            cards.add("card-" + (i+1));
        }

        return cards;
    }

    private int runFraudSimulation(SimulationData simulationData, List<String> cards) {
        return IntStream.range(0, simulationData.getIterations()).map((iteration) -> {
            this.logger.info("[Business] [ID: " + simulationData.getId() + " / Iteration: " + iteration + "]");
            int movementsCreatedPerCard = cards.parallelStream()
                    .mapToInt((card) -> simulateFraudMovementsForACard(card, simulationData.getId(), iteration))
                    .sum();

            this.logger.info("[Business] [ID: " + simulationData.getId() + " / Iteration: " + iteration + " / Movements created: " + movementsCreatedPerCard + " ]");
            sleep(simulationData.getMsBetweenIterations());

            return movementsCreatedPerCard;
        }).sum();
    }

    private void publishSimulationDataResults(SimulationData simulationData, int totalMovementsCreated) {
        this.logger.info("[Business] Publishing fraud simulation process results.....ID: " + simulationData.getId());
        this.eventBusPort.publishSimulationDataResults(SimulationDataResults.builder()
                .cards(simulationData.getCards())
                .iterations(simulationData.getIterations())
                .msBetweenIterations(simulationData.getMsBetweenIterations())
                .id(simulationData.getId())
                .movementsGenerated(totalMovementsCreated)
                .potentialFraudMovements(calculatePotentialFraudsMovements(simulationData.getCards(), simulationData.getIterations()))
                .build());
    }

    private int simulateFraudMovementsForACard(String card, long processId, int iteration) {
        this.logger.info("[Business] [ID: " + processId + " / Iteration: " + iteration + " / Card: " + card + "] Init movements creation....");
        Uni<Integer> multipleATM = Uni.createFrom().item(() -> this.multipleATMMovements(card, processId, iteration)).runSubscriptionOn(this.executor);
        Uni<Integer> multipleMerchant = Uni.createFrom().item(() ->this.multipleMerchantMovements(card, processId, iteration)).runSubscriptionOn(this.executor);
        Uni<Integer> multipleOnline = Uni.createFrom().item(() -> this.multipleOnlineMovements(card, processId, iteration)).runSubscriptionOn(this.executor);
        Uni<Integer> suspiciousOnline = Uni.createFrom().item(() -> this.onlineMovementFromSuspiciousSite(card, processId, iteration)).runSubscriptionOn(this.executor);
//        Uni<Integer> merchantAndAtm = Uni.createFrom().item(() -> this.merchantAndATMMovement(card)).runSubscriptionOn(this.executor);

        Tuple res = Uni.combine().all()
                .unis(multipleATM, multipleMerchant, multipleOnline, suspiciousOnline)
                .asTuple()
                .await()
                .atMost(Duration.ofSeconds(120));

        int total = res.asList().stream().mapToInt((n) -> (int) n).sum();

        this.logger.info("[Business] [ID: " + processId + " / Iteration: " + iteration + " / Card: " + card + "] Total movements created: " + total);

        return total;
    }


    /**
     * Generates three atm movements associated to the same card
     * @param card
     */
    private int multipleATMMovements(String card, long processId, int iteration) {
        int total = 0;
        for (int i=0; i<3; i++) {
            this.eventBusPort.publishATMMovement(card, "atm-" + (i+1), 100f * (i+1), processId, iteration);
            this.sleep(2000,500);
            total++;
        }
        return total;
    }

    /**
     * Generates two atm movements associated to the same card
     * @param card
     */
    private int multipleMerchantMovements(String card, long processId, int iteration)  {
        int total = 0;
        for (int i=0; i<2; i++) {
            this.eventBusPort.publishMerchantMovement(card, "merchant-" + (i+1), 100f * (i+1), processId, iteration);
            this.sleep(2000,500);
            total++;
        }
        return total;
    }

    /**
     * Generates four online movements associated to the same card
     * during a short period of time and exceeding the amount limit (100)
     * @param card
     */
    private int multipleOnlineMovements(String card, long processId, int iteration) {
        float initAmount = 30f;
        int total = 0;

        for (int i = 0; i< FRAUDS_MOVEMENTS_PER_CARD; i++) {
            this.eventBusPort.publishOnlineMovement(card, "online" + (i+1), (i+1) * initAmount, processId, iteration);
            this.sleep(2000, 500);
            total++;
        }
        return total;
    }

    /**
     * Generates an online movement from a suspicious site associated to the card
     * @param card
     */
    private int onlineMovementFromSuspiciousSite(String card, long processId, int iteration) {
        this.eventBusPort.publishOnlineMovement(card, "fraud-1", 100f, processId, iteration);
        return 1;
    }

//    private int merchantAndATMMovement(String card) {
//        this.publishMerchantMovement(card, "merchant-1", 100f);
//        this.sleep(2000, 1000);
//        this.publishATMMovement(card, "atm-1", 200f);
//
//        return 2;
//    }

    private void sleep(int max, int min) {
        Random random = new Random();
        try {
            Thread.sleep(random.nextInt(max) + min);
        } catch (InterruptedException e) {
            this.logger.error(e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            this.logger.error(e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

}
