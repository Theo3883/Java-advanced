package org.vros.worker.solver;

/**
 * Great-circle distance between two WGS-84 coordinates.
 *
 * <p>Used by the greedy solver as a cheap proxy for road distance. The earth radius is the
 * mean radius (6 371.0088 km), which keeps errors well below 0.5 % for homework-sized scenarios.</p>
 */
public final class HaversineDistance {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    private HaversineDistance() {
    }

    public static double between(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
