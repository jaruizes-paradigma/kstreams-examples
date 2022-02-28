package com.jaruizes.kstreams.examples.fraudsimulator.business;

import com.jaruizes.kstreams.examples.fraudsimulator.business.ports.eventbus.EventBusPort;
import com.jaruizes.kstreams.examples.fraudsimulator.business.model.CardFraudConfig;
import com.jaruizes.kstreams.examples.fraudsimulator.business.model.SimulationData;
import com.jaruizes.kstreams.examples.fraudsimulator.business.model.SimulationDataResults;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.tuples.Tuple;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

@ApplicationScoped
public class FraudSimulator {

    public static final String POTENTIAL_FRAUD_SITE_PREFIX = "fraud-";
    public static final String MERCHANT_PREFIX = "merchant-";
    public static final String ATM_PREFIX = "atm-";
    public static final String ONLINE_PREFIX = "online-";
    private final Logger logger = Logger.getLogger(FraudSimulator.class);

    @Inject
    EventBusPort eventBusPort;

    private final ExecutorService executor;

    public FraudSimulator() {
        this.executor = Executors.newFixedThreadPool(10, r -> new Thread(r, "executors"));
    }

    public int calculatePotentialFraudsMovements(SimulationData simulationData) {
        final List<CardFraudConfig> cardFraudConfigs = simulationData.getCardsFraudConfig();
        int totalFraudCasesInOneIteration = cardFraudConfigs.stream().mapToInt(cardFraudConfig -> cardFraudConfig.getFraudTypes().size()).sum();

        return totalFraudCasesInOneIteration * simulationData.getIterations();
    }

    public long createFraudSimulationProcess(SimulationData simulationData) {
        long id = new Date().getTime();
        simulationData.setId(id);

        this.logger.info("[Business] Starting fraud simulation process.....ID: " + simulationData.getId());
        this.eventBusPort.publishSimulationData(simulationData);

        return id;
    }

    public void executeFraudSimulationProcess(SimulationData simulationData) {
        this.logger.info("[Business] Executing fraud simulation process.....ID: " + simulationData.getId());

        int totalMovementsCreated = this.runFraudSimulation(simulationData);
        this.publishSimulationDataResults(simulationData, totalMovementsCreated);
    }

    private int runFraudSimulation(SimulationData simulationData) {
        return IntStream.range(0, simulationData.getIterations()).map((iteration) -> {
            this.logger.info("[Business] [ID: " + simulationData.getId() + " / Iteration: " + iteration + "]");

            int movementsCreatedPerCard = simulationData.getCardsFraudConfig().parallelStream()
                    .mapToInt(cardConfig -> simulateFraudMovementsForACard(cardConfig, simulationData.getId(), iteration))
                    .sum();

            this.logger.info("[Business] [ID: " + simulationData.getId() + " / Iteration: " + iteration + " / Movements created: " + movementsCreatedPerCard + " ]");
            sleep(simulationData.getMsBetweenIterations());

            return movementsCreatedPerCard;
        }).sum();
    }

    private void publishSimulationDataResults(SimulationData simulationData, int totalMovementsCreated) {
        this.logger.info("[Business] Publishing fraud simulation process results.....ID: " + simulationData.getId());
        this.eventBusPort.publishSimulationDataResults(SimulationDataResults.builder()
                .iterations(simulationData.getIterations())
                .msBetweenIterations(simulationData.getMsBetweenIterations())
                .id(simulationData.getId())
                .movementsGenerated(totalMovementsCreated)
                .potentialFraudMovements(this.calculatePotentialFraudsMovements(simulationData))
                .cardsFraudConfig(simulationData.getCardsFraudConfig())
                .build());
    }

    private int simulateFraudMovementsForACard(CardFraudConfig cardFraudConfig, long processId, int iteration) {
        String card = cardFraudConfig.getCard();
        List<Integer> fraudTypes = cardFraudConfig.getFraudTypes();

        this.logger.info("[Business] [ID: " + processId + " / Iteration: " + iteration + " / Card: " + card + "] Init movements creation....");
        Uni<Integer> multipleATM = this.caseMultipleATM(card, fraudTypes, processId, iteration);
        Uni<Integer> multipleMerchant = this.caseMultipleMerchant(card, fraudTypes, processId, iteration);
        Uni<Integer> multipleOnline = this.caseMultipleOnline(card, fraudTypes, processId, iteration);
        Uni<Integer> suspiciousOnline = this.casePotentialFraudSite(card, fraudTypes, processId, iteration);
        Uni<Integer> merchantAndATM = this.caseATMAndMerchant(card, fraudTypes, processId, iteration);

        Tuple res = Uni.combine().all()
                .unis(multipleATM, multipleMerchant, multipleOnline, suspiciousOnline, merchantAndATM)
                .asTuple()
                .await()
                .atMost(Duration.ofSeconds(60));

        int total = res.asList().stream().mapToInt((n) -> (int) n).sum();

        this.logger.info("[Business] [ID: " + processId + " / Iteration: " + iteration + " / Card: " + card + "] Total movements created: " + total);

        return total;
    }

    /**
     * Implements multiple ATM movements case for a card if this case is included in the fraud types
     * @param card
     * @param fraudTypes
     * @param processId
     * @param iteration
     * @return
     */
    private Uni<Integer> caseMultipleATM(String card, List<Integer> fraudTypes, long processId, int iteration) {
        return Uni.createFrom().item(() ->
                fraudTypes.contains(1) ? this.multipleATMMovements(card, processId, iteration): 0)
                .runSubscriptionOn(this.executor);
    }

    /**
     * Implements multiple Merchant movements case for a card if this case is included in the fraud types
     * @param card
     * @param fraudTypes
     * @param processId
     * @param iteration
     * @return
     */
    private Uni<Integer> caseMultipleMerchant(String card, List<Integer> fraudTypes, long processId, int iteration) {
        return Uni.createFrom().item(() ->
                fraudTypes.contains(2) ? this.multipleMerchantMovements(card, processId, iteration) : 0)
                .runSubscriptionOn(this.executor);
    }

    /**
     * Implements multiple online movements case for a card if this case is included in the fraud types
     * @param card
     * @param fraudTypes
     * @param processId
     * @param iteration
     * @return
     */
    private Uni<Integer> caseMultipleOnline(String card, List<Integer> fraudTypes, long processId, int iteration) {
        return Uni.createFrom().item(() ->
                fraudTypes.contains(3) ? this.multipleOnlineMovements(card, processId, iteration) : 0)
                .runSubscriptionOn(this.executor);
    }

    /**
     * Implements potential fraud online site case for a card if this case is included in the fraud types
     * @param card
     * @param fraudTypes
     * @param processId
     * @param iteration
     * @return
     */
    private Uni<Integer> casePotentialFraudSite(String card, List<Integer> fraudTypes, long processId, int iteration) {
        return Uni.createFrom().item(() ->
                fraudTypes.contains(4) ? this.onlineMovementFromSuspiciousSite(card, processId, iteration) : 0)
                .runSubscriptionOn(this.executor);
    }

    /**
     * Implements ATM and Merchant fraud case for a card if this case is included in the fraud types
     * @param card
     * @param fraudTypes
     * @param processId
     * @param iteration
     * @return
     */
    private Uni<Integer> caseATMAndMerchant(String card, List<Integer> fraudTypes, long processId, int iteration) {
        return Uni.createFrom().item(() -> {
            boolean isCase1And2Included = (fraudTypes.contains(1)) && (fraudTypes.contains(2));
            boolean isCase5 = fraudTypes.contains(5);
            if (isCase5 && !isCase1And2Included) {
                return this.merchantAndATMMovements(card, processId, iteration);
            }
            return 0;
        }).runSubscriptionOn(this.executor);
    }


    /**
     * Generates three atm movements associated to the same card
     * @param card
     */
    private int multipleATMMovements(String card, long processId, int iteration) {
        int total = 0;
        for (int i=0; i<3; i++) {
            this.eventBusPort.publishATMMovement(card, ATM_PREFIX + (i+1), 100f * (i+1), processId, iteration);
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
            this.eventBusPort.publishMerchantMovement(card, MERCHANT_PREFIX + (i+1), 100f * (i+1), processId, iteration);
            this.sleep(2000,500);
            total++;
        }
        return total;
    }

    /**
     * Generates a merchant and an atm movements associated to the same card
     * @param card
     */
    private int merchantAndATMMovements(String card, long processId, int iteration)  {
        this.eventBusPort.publishMerchantMovement(card, MERCHANT_PREFIX + 1, 100f, processId, iteration);
        this.sleep(2000,500);
        this.eventBusPort.publishATMMovement(card, ATM_PREFIX + 1, 100f, processId, iteration);
        return 2;
    }

    /**
     * Generates four online movements associated to the same card
     * during a short period of time and exceeding the amount limit (100)
     * @param card
     */
    private int multipleOnlineMovements(String card, long processId, int iteration) {
        float initAmount = 30f;
        int total = 0;

        for (int i = 0; i< 4; i++) {
            this.eventBusPort.publishOnlineMovement(card, ONLINE_PREFIX + (i+1), (i+1) * initAmount, processId, iteration);
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
        this.eventBusPort.publishOnlineMovement(card, POTENTIAL_FRAUD_SITE_PREFIX + processId, 100f, processId, iteration);
        return 1;
    }

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
