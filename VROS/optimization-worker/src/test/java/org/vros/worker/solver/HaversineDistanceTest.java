package org.vros.worker.solver;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HaversineDistanceTest {

    @Test
    void samePointIsZero() {
        assertThat(HaversineDistance.between(44.4268, 26.1025, 44.4268, 26.1025)).isZero();
    }

    @Test
    void oneDegreeOfLongitudeAtTheEquatorIsAbout111Km() {
        double distance = HaversineDistance.between(0, 0, 0, 1);
        assertThat(distance).isCloseTo(111.195, org.assertj.core.data.Offset.offset(0.5));
    }

    @Test
    void shortBucharestHopIsPlausible() {
        // Piata Unirii -> Piata Universitatii, roughly 1.6 km as the crow flies.
        double distance = HaversineDistance.between(44.4268, 26.1025, 44.4355, 26.1020);
        assertThat(distance).isBetween(0.5, 2.5);
    }

    @Test
    void isSymmetric() {
        double forward = HaversineDistance.between(44.4268, 26.1025, 44.4355, 26.1020);
        double backward = HaversineDistance.between(44.4355, 26.1020, 44.4268, 26.1025);
        assertThat(forward).isEqualTo(backward);
    }
}
