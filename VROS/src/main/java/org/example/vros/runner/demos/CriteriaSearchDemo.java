package org.example.vros.runner.demos;

import lombok.extern.slf4j.Slf4j;
import org.example.vros.dto.DeliveryFilterDto;
import org.example.vros.model.Delivery;
import org.example.vros.service.interfaces.DeliverySearchService;

import java.time.LocalDate;
import java.util.List;

@Slf4j
public final class CriteriaSearchDemo {

    private CriteriaSearchDemo() {}

    public static void run(DeliverySearchService deliverySearchService) {
        log.info("\n------------------------------------------------------------------");
        log.info(" [DEMO 2] Dynamic Criteria API Delivery Search");
        log.info("------------------------------------------------------------------");

        // 1. Filter by Demand Threshold
        DeliveryFilterDto thresholdFilter = DeliveryFilterDto.builder()
                .minQuantity(100)
                .build();
        List<Delivery> highDemand = deliverySearchService.search(thresholdFilter);
        log.info("Criteria Search [MinQuantity >= 100]: found {} deliveries", highDemand.size());

        // 2. Filter by Geographic Area (Bucharest Center coords +/- 10 km)
        DeliveryFilterDto areaFilter = DeliveryFilterDto.builder()
                .centerLat(44.4323)
                .centerLng(26.1063)
                .radiusKm(10.0)
                .build();
        List<Delivery> areaDeliveries = deliverySearchService.search(areaFilter);
        log.info("Criteria Search [Area radius 10km around Bucharest]: found {} deliveries", areaDeliveries.size());

        // 3. Combined Multi-Parameter Filter: Area + Min Demand + Date Range
        DeliveryFilterDto combinedFilter = DeliveryFilterDto.builder()
                .centerLat(44.4323)
                .centerLng(26.1063)
                .radiusKm(20.0)
                .minQuantity(40)
                .dateFrom(LocalDate.now().minusDays(7))
                .dateTo(LocalDate.now())
                .build();
        List<Delivery> combinedResults = deliverySearchService.search(combinedFilter);
        log.info("Criteria Search [Combined Multi-Criteria (Area + Demand >= 40 + Last 7 Days)]: found {} deliveries",
                combinedResults.size());
    }
}
