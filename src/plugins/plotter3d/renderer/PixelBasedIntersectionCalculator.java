package plugins.plotter3d.renderer;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

import plugins.plotter3d.model.Plot3DModel;
import plugins.plotter3d.model.Plot3DPoint;
import plugins.plotter3d.view.Plot3DView;
import parser.Function3DParser;

/**
 * Optimized pixel-based intersection calculator for 3D functions
 * Focuses on visible points only with adaptive precision based on zoom level
 */
public class PixelBasedIntersectionCalculator {
    // Constants for performance tuning
    private static final double BASE_TOLERANCE = 1e-6;
    private static final int MAX_CACHE_SIZE = 50;
    private static final Color INTERSECTION_COLOR = Color.RED;

    // Cache for calculated intersections - using LRU cache for memory management
    private static final Map<String, List<List<Plot3DPoint>>> intersectionCache = new LinkedHashMap<String, List<List<Plot3DPoint>>>(
            MAX_CACHE_SIZE + 1, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, List<List<Plot3DPoint>>> eldest) {
            return size() > MAX_CACHE_SIZE;
        }
    };

    // Thread pool for parallel calculations
    private static final ExecutorService executor = Executors
            .newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    // Cache key generator based on functions and visible area
    private static String generateCacheKey(Plot3DModel model, double xMin, double xMax, double yMin, double yMax,
            double zoom) {
        StringBuilder key = new StringBuilder();
        for (Plot3DModel.Function3DInfo func : model.getFunctions()) {
            key.append(func.getExpression()).append(":");
        }
        // Include visible range and zoom in the key with reduced precision
        key.append(String.format("%.1f:%.1f:%.1f:%.1f:%.1f", xMin, xMax, yMin, yMax, zoom));
        return key.toString();
    }

    /**
     * Calculate all visible intersections between functions with pixel-based
     * precision
     */
    public static List<List<Plot3DPoint>> calculateVisibleIntersections(
            Plot3DModel model, Plot3DView view,
            double displayScale, int screenWidth, int screenHeight) {

        // Check if we have at least two functions
        List<Plot3DModel.Function3DInfo> functions = model.getFunctions();
        if (functions.size() < 2) {
            return new ArrayList<>();
        }

        // Generate cache key including visible area and zoom level
        String cacheKey = generateCacheKey(model,
                view.getXMin(), view.getXMax(),
                view.getYMin(), view.getYMax(),
                view.getScale());

        // Check cache first
        synchronized (intersectionCache) {
            if (intersectionCache.containsKey(cacheKey)) {
                return intersectionCache.get(cacheKey);
            }
        }

        // Calculate pixel-to-world ratio for adaptive precision
        double pixelToWorldRatioX = (view.getXMax() - view.getXMin()) / screenWidth;
        double pixelToWorldRatioY = (view.getYMax() - view.getYMin()) / screenHeight;

        // Adaptive sampling based on screen size and zoom level
        // Lower resolution when zoomed out, higher resolution when zoomed in
        int baseSamples = 150; // Increased from 100 for finer resolution
        double zoomFactor = Math.min(3.0, Math.max(0.8, view.getScale()));
        int samplesX = (int) (baseSamples * zoomFactor);
        int samplesY = (int) (baseSamples * zoomFactor);

        // Adjust for screen aspect ratio
        double aspectRatio = (double) screenWidth / screenHeight;
        if (aspectRatio > 1) {
            samplesX = (int) (samplesX * aspectRatio);
        } else {
            samplesY = (int) (samplesY / aspectRatio);
        }

        // Cap at reasonable limits - higher upper limit for more precision
        samplesX = Math.min(300, Math.max(50, samplesX));
        samplesY = Math.min(300, Math.max(50, samplesY));

        // Result list for all intersection curves
        List<List<Plot3DPoint>> allIntersections = Collections.synchronizedList(new ArrayList<>());

        // For each pair of functions
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < functions.size() - 1; i++) {
            final int fi = i;
            for (int j = i + 1; j < functions.size(); j++) {
                final int fj = j;

                // Create final copies of the variables for use in the lambda
                final int finalSamplesX = samplesX;
                final int finalSamplesY = samplesY;
                final double finalPixelToWorldRatioX = pixelToWorldRatioX;
                final double finalPixelToWorldRatioY = pixelToWorldRatioY;

                futures.add(executor.submit(() -> {
                    Function3DParser function1 = functions.get(fi).function;
                    Function3DParser function2 = functions.get(fj).function;

                    // Calculate intersections for this function pair
                    List<Plot3DPoint> intersectionCurve = calculateIntersectionCurve(
                            function1, function2,
                            view.getXMin(), view.getXMax(), view.getYMin(), view.getYMax(),
                            finalSamplesX, finalSamplesY, finalPixelToWorldRatioX, finalPixelToWorldRatioY);

                    if (!intersectionCurve.isEmpty()) {
                        allIntersections.add(intersectionCurve);
                    }
                }));
            }
        }

        // Wait for all calculations to complete
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                System.err.println("Error calculating intersections: " + e.getMessage());
            }
        }

        // Cache the result
        synchronized (intersectionCache) {
            intersectionCache.put(cacheKey, allIntersections);
        }

        return allIntersections;
    }

    /**
     * Calculate intersection curve between two functions with pixel accuracy
     */
    private static List<Plot3DPoint> calculateIntersectionCurve(
            Function3DParser function1, Function3DParser function2,
            double xMin, double xMax, double yMin, double yMax,
            int samplesX, int samplesY, double pixelToWorldRatioX, double pixelToWorldRatioY) {

        // Adaptive tolerance based on zoom level/pixel ratio
        double tolerance = Math.max(BASE_TOLERANCE, Math.min(pixelToWorldRatioX, pixelToWorldRatioY));

        // Step sizes for sampling
        double stepX = (xMax - xMin) / samplesX;
        double stepY = (yMax - yMin) / samplesY;

        // Grid for difference values
        double[][] diffValues = new double[samplesX + 1][samplesY + 1];

        // Calculate difference values at grid points
        for (int i = 0; i <= samplesX; i++) {
            double x = xMin + i * stepX;
            for (int j = 0; j <= samplesY; j++) {
                double y = yMin + j * stepY;
                try {
                    double z1 = function1.evaluateAt(x, y);
                    double z2 = function2.evaluateAt(x, y);
                    diffValues[i][j] = z1 - z2;
                } catch (Exception e) {
                    diffValues[i][j] = Double.NaN;
                }
            }
        }

        // Find intersection points
        List<Plot3DPoint> intersectionPoints = new ArrayList<>();

        // Search for zero crossings in grid cells
        for (int i = 0; i < samplesX; i++) {
            for (int j = 0; j < samplesY; j++) {
                double diff00 = diffValues[i][j];
                double diff10 = diffValues[i + 1][j];
                double diff01 = diffValues[i][j + 1];
                double diff11 = diffValues[i + 1][j + 1];

                // Skip if any value is NaN
                if (Double.isNaN(diff00) || Double.isNaN(diff10) ||
                        Double.isNaN(diff01) || Double.isNaN(diff11)) {
                    continue;
                }

                // Check for sign changes along edges
                boolean hasIntersection = false;

                // Check all four edges of the cell
                if (diff00 * diff10 <= 0)
                    hasIntersection = true; // Top edge
                if (diff00 * diff01 <= 0)
                    hasIntersection = true; // Left edge
                if (diff10 * diff11 <= 0)
                    hasIntersection = true; // Right edge
                if (diff01 * diff11 <= 0)
                    hasIntersection = true; // Bottom edge

                if (hasIntersection) {
                    // Refine intersection point using binary search
                    Plot3DPoint refined = refineIntersectionPoint(
                            function1, function2,
                            xMin + i * stepX, xMin + (i + 1) * stepX,
                            yMin + j * stepY, yMin + (j + 1) * stepY,
                            tolerance);

                    if (refined != null) {
                        intersectionPoints.add(refined);
                    }
                }
            }
        }

        // Connect points into a curve
        return connectIntersectionPoints(intersectionPoints, stepX, stepY);
    }

    /**
     * Refine an intersection point using gradient descent method
     * This method produces much more accurate intersection points
     */
    private static Plot3DPoint refineIntersectionPoint(
            Function3DParser function1, Function3DParser function2,
            double x1, double x2, double y1, double y2, double tolerance) {

        // Initial point at center of the cell
        double x = (x1 + x2) / 2;
        double y = (y1 + y2) / 2;

        // Use adaptive iterations - more for higher precision areas
        int maxIterations = 8;
        double stepSize = Math.min(x2 - x1, y2 - y1) * 0.1;
        double minStepSize = tolerance * 0.01;

        for (int i = 0; i < maxIterations && stepSize > minStepSize; i++) {
            try {
                // Evaluate function difference
                double z1 = function1.evaluateAt(x, y);
                double z2 = function2.evaluateAt(x, y);
                double diff = z1 - z2;

                // Intersection found with sufficient precision
                if (Math.abs(diff) < tolerance) {
                    return new Plot3DPoint(x, y, z1);
                }

                // Calculate gradient of the difference function
                double h = Math.max(1e-6, (x2 - x1) * 0.01);

                // Numerical gradient in x direction
                double diffX1 = function1.evaluateAt(x + h, y) - function2.evaluateAt(x + h, y);
                double diffX2 = function1.evaluateAt(x - h, y) - function2.evaluateAt(x - h, y);
                double gradX = (diffX1 - diffX2) / (2 * h);

                // Numerical gradient in y direction
                double diffY1 = function1.evaluateAt(x, y + h) - function2.evaluateAt(x, y + h);
                double diffY2 = function1.evaluateAt(x, y - h) - function2.evaluateAt(x, y - h);
                double gradY = (diffY1 - diffY2) / (2 * h);

                // Calculate magnitude of gradient for normalization
                double gradMagnitude = Math.sqrt(gradX * gradX + gradY * gradY);

                // Avoid division by zero
                if (gradMagnitude < 1e-10) {
                    // Try another point if gradient is too small
                    x = x1 + Math.random() * (x2 - x1);
                    y = y1 + Math.random() * (y2 - y1);
                    continue;
                }

                // Normalize gradient and move in the opposite direction
                double moveX = -gradX / gradMagnitude * diff * stepSize;
                double moveY = -gradY / gradMagnitude * diff * stepSize;

                // Update position
                double newX = x + moveX;
                double newY = y + moveY;

                // Ensure we stay within the cell
                newX = Math.max(x1, Math.min(x2, newX));
                newY = Math.max(y1, Math.min(y2, newY));

                // Check if we've moved significantly
                double moveDist = Math.sqrt((newX - x) * (newX - x) + (newY - y) * (newY - y));
                if (moveDist < tolerance * 0.01) {
                    // Reduce step size if we're not making progress
                    stepSize *= 0.5;
                }

                x = newX;
                y = newY;

            } catch (Exception e) {
                // In case of numerical errors, try a slightly different point
                x = x1 + Math.random() * (x2 - x1);
                y = y1 + Math.random() * (y2 - y1);
                stepSize *= 0.5;
            }
        }

        // Use the best approximation found
        try {
            double z1 = function1.evaluateAt(x, y);
            double z2 = function2.evaluateAt(x, y);

            // Use average z-value for better visual results
            double z = (z1 + z2) / 2;
            return new Plot3DPoint(x, y, z);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Connect intersection points into a continuous curve
     * Enhanced version with improved curve detection and connectivity
     */
    private static List<Plot3DPoint> connectIntersectionPoints(
            List<Plot3DPoint> points, double stepX, double stepY) {

        if (points.size() < 2) {
            return points;
        }

        // Cluster the points into separate curves based on proximity
        List<List<Plot3DPoint>> curves = clusterPointsIntoCurves(points, stepX, stepY);
        List<Plot3DPoint> connectedPoints = new ArrayList<>();

        // For each curve, organize the points and add to the result
        for (List<Plot3DPoint> curve : curves) {
            if (curve.size() < 2)
                continue;

            // Organize points along the curve for better continuity
            List<Plot3DPoint> organizedCurve = organizePointsAlongCurve(curve);

            // Add curve separator if not the first curve
            if (!connectedPoints.isEmpty()) {
                connectedPoints.add(new Plot3DPoint(Double.NaN, Double.NaN, Double.NaN));
            }

            // Add curve points
            connectedPoints.addAll(organizedCurve);
        }

        // Apply smoothing to reduce jaggedness
        return smoothCurve(connectedPoints);
    }

    /**
     * Cluster points into separate curves based on spatial proximity
     */
    private static List<List<Plot3DPoint>> clusterPointsIntoCurves(
            List<Plot3DPoint> points, double stepX, double stepY) {

        List<List<Plot3DPoint>> clusters = new ArrayList<>();
        boolean[] assigned = new boolean[points.size()];

        // Calculate adaptive clustering distance
        double clusterDistance = Math.sqrt(stepX * stepX + stepY * stepY) * 2.0;

        // Process each unassigned point
        for (int i = 0; i < points.size(); i++) {
            if (assigned[i])
                continue;

            // Start a new cluster
            List<Plot3DPoint> cluster = new ArrayList<>();
            cluster.add(points.get(i));
            assigned[i] = true;

            // Grow cluster using breadth-first search
            Queue<Integer> queue = new LinkedList<>();
            queue.add(i);

            while (!queue.isEmpty()) {
                int current = queue.poll();
                Plot3DPoint currentPoint = points.get(current);

                // Check all other unassigned points
                for (int j = 0; j < points.size(); j++) {
                    if (assigned[j])
                        continue;

                    Plot3DPoint candidate = points.get(j);
                    double dist = Math.sqrt(
                            Math.pow(currentPoint.getX() - candidate.getX(), 2) +
                                    Math.pow(currentPoint.getY() - candidate.getY(), 2));

                    // If close enough, add to cluster
                    if (dist <= clusterDistance) {
                        cluster.add(candidate);
                        assigned[j] = true;
                        queue.add(j);
                    }
                }
            }

            // Add cluster to result
            if (!cluster.isEmpty()) {
                clusters.add(cluster);
            }
        }

        return clusters;
    }

    /**
     * Organize points along a curve for better continuity
     */
    private static List<Plot3DPoint> organizePointsAlongCurve(List<Plot3DPoint> curvePoints) {
        if (curvePoints.size() <= 2)
            return curvePoints;

        List<Plot3DPoint> organized = new ArrayList<>();
        List<Plot3DPoint> remaining = new ArrayList<>(curvePoints);

        // Detect if this might be a closed curve (like a circle)
        boolean potentialClosedCurve = isPotentialClosedCurve(curvePoints);

        // For open curves (most cases), find best starting point
        if (!potentialClosedCurve) {
            // Try to find endpoints first
            findEndpoints(remaining, organized);
            if (!organized.isEmpty()) {
                remaining.removeAll(organized);
            } else {
                // If no clear endpoints, start with leftmost or rightmost point
                if (Math.random() < 0.5) {
                    // Start from left
                    remaining.sort((p1, p2) -> Double.compare(p1.getX(), p2.getX()));
                } else {
                    // Start from right
                    remaining.sort((p1, p2) -> Double.compare(p2.getX(), p1.getX()));
                }
                organized.add(remaining.remove(0));
            }
        } else {
            // For closed curves, start with rightmost point
            remaining.sort((p1, p2) -> Double.compare(p2.getX(), p1.getX()));
            organized.add(remaining.remove(0));
        }

        // If still empty (fallback), start with first point
        if (organized.isEmpty() && !remaining.isEmpty()) {
            organized.add(remaining.remove(0));
        }

        // Build curve using nearest neighbor
        while (!remaining.isEmpty()) {
            Plot3DPoint current = organized.get(organized.size() - 1);
            int nearestIndex = findNearestPointIndex(current, remaining);
            organized.add(remaining.remove(nearestIndex));
        }

        // For closed curves, explicitly close the loop if needed
        if (potentialClosedCurve) {
            Plot3DPoint first = organized.get(0);
            Plot3DPoint last = organized.get(organized.size() - 1);

            // Calculate distance between first and last points
            double dist = Math.sqrt(
                    Math.pow(first.getX() - last.getX(), 2) +
                            Math.pow(first.getY() - last.getY(), 2));

            // Calculate average distance between adjacent points in the curve
            double totalDist = 0;
            int count = 0;
            for (int i = 0; i < organized.size() - 1; i++) {
                Plot3DPoint p1 = organized.get(i);
                Plot3DPoint p2 = organized.get(i + 1);
                totalDist += Math.sqrt(
                        Math.pow(p1.getX() - p2.getX(), 2) +
                                Math.pow(p1.getY() - p2.getY(), 2));
                count++;
            }
            double avgDist = count > 0 ? totalDist / count : 0;

            // Only close if endpoints are very close relative to average point spacing
            if (dist < avgDist * 1.5) {
                organized.add(new Plot3DPoint(first.getX(), first.getY(), first.getZ()));
            }
        }

        return organized;
    }

    /**
     * Detect if a set of points might form a closed curve (like a circle)
     * More conservative implementation that won't force closure for boundary curves
     */
    private static boolean isPotentialClosedCurve(List<Plot3DPoint> points) {
        if (points.size() < 12)
            return false; // Need more points for reliable closed curve detection

        // Check if first and last points are very close to each other
        Plot3DPoint first = points.get(0);
        Plot3DPoint last = points.get(points.size() - 1);

        double firstLastDist = Math.sqrt(
                Math.pow(first.getX() - last.getX(), 2) +
                        Math.pow(first.getY() - last.getY(), 2));

        // Calculate approximate "center of mass"
        double sumX = 0, sumY = 0;
        for (Plot3DPoint p : points) {
            sumX += p.getX();
            sumY += p.getY();
        }
        double centerX = sumX / points.size();
        double centerY = sumY / points.size();

        // Calculate average distance to center
        double sumDist = 0;
        double maxDist = 0;
        double minDist = Double.MAX_VALUE;

        for (Plot3DPoint p : points) {
            double dist = Math.sqrt(
                    Math.pow(p.getX() - centerX, 2) +
                            Math.pow(p.getY() - centerY, 2));
            sumDist += dist;
            maxDist = Math.max(maxDist, dist);
            minDist = Math.min(minDist, dist);
        }
        double avgDist = sumDist / points.size();

        // Calculate variance of distance to center
        double variance = 0;
        for (Plot3DPoint p : points) {
            double dist = Math.sqrt(
                    Math.pow(p.getX() - centerX, 2) +
                            Math.pow(p.getY() - centerY, 2));
            variance += Math.pow(dist - avgDist, 2);
        }
        variance /= points.size();
        double stdDev = Math.sqrt(variance);

        // Check for points near the boundary that would indicate an open curve
        boolean hasBoundaryPoints = false;
        double boundaryTolerance = 0.05; // 5% from edge is considered boundary

        // Get the bounds of all points
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

        for (Plot3DPoint p : points) {
            minX = Math.min(minX, p.getX());
            maxX = Math.max(maxX, p.getX());
            minY = Math.min(minY, p.getY());
            maxY = Math.max(maxY, p.getY());
        }

        double xRange = maxX - minX;
        double yRange = maxY - minY;

        // Check for boundary points
        for (Plot3DPoint p : points) {
            if (Math.abs(p.getX() - minX) < boundaryTolerance * xRange ||
                    Math.abs(p.getX() - maxX) < boundaryTolerance * xRange ||
                    Math.abs(p.getY() - minY) < boundaryTolerance * yRange ||
                    Math.abs(p.getY() - maxY) < boundaryTolerance * yRange) {
                hasBoundaryPoints = true;
                break;
            }
        }

        // Strict conditions for a curve to be considered closed:
        // 1. First and last points must be very close
        // 2. Standard deviation must be low compared to average (indicating circular
        // shape)
        // 3. The ratio of max to min distance must be reasonably low (indicates
        // roundness)
        // 4. No points should be very close to the boundary of the dataset
        boolean isRoundShape = stdDev / avgDist < 0.2;
        boolean isUniformRadius = maxDist / minDist < 2.0;
        boolean endpointsClose = firstLastDist < avgDist * 0.3;

        return isRoundShape && isUniformRadius && endpointsClose && !hasBoundaryPoints;
    }

    /**
     * Attempt to find endpoints of an open curve
     */
    private static void findEndpoints(List<Plot3DPoint> points, List<Plot3DPoint> endpoints) {
        if (points.size() < 4)
            return;

        // For each point, count neighbors within a certain distance
        double neighborDist = 0;

        // Calculate average distance between points
        double totalDist = 0;
        int count = 0;
        for (int i = 0; i < Math.min(points.size(), 100); i++) {
            for (int j = i + 1; j < Math.min(points.size(), 100); j++) {
                totalDist += Math.sqrt(
                        Math.pow(points.get(i).getX() - points.get(j).getX(), 2) +
                                Math.pow(points.get(i).getY() - points.get(j).getY(), 2));
                count++;
            }
        }
        if (count > 0) {
            neighborDist = (totalDist / count) * 1.5;
        } else {
            return; // Can't determine neighborhood size
        }

        // Count neighbors for each point
        int[] neighborCount = new int[points.size()];
        for (int i = 0; i < points.size(); i++) {
            for (int j = 0; j < points.size(); j++) {
                if (i == j)
                    continue;

                double dist = Math.sqrt(
                        Math.pow(points.get(i).getX() - points.get(j).getX(), 2) +
                                Math.pow(points.get(i).getY() - points.get(j).getY(), 2));

                if (dist <= neighborDist) {
                    neighborCount[i]++;
                }
            }
        }

        // Find points with least neighbors (likely endpoints)
        int minNeighbors = Integer.MAX_VALUE;
        for (int count1 : neighborCount) {
            if (count1 > 0 && count1 < minNeighbors) {
                minNeighbors = count1;
            }
        }

        // Find at most 2 endpoints
        for (int i = 0; i < points.size() && endpoints.size() < 2; i++) {
            if (neighborCount[i] == minNeighbors) {
                endpoints.add(points.get(i));
            }
        }
    }

    /**
     * Apply smoothing to reduce jaggedness in curves
     */
    private static List<Plot3DPoint> smoothCurve(List<Plot3DPoint> points) {
        if (points.size() < 4)
            return points;

        List<Plot3DPoint> smoothed = new ArrayList<>();
        boolean inSegment = false;
        List<Plot3DPoint> currentSegment = new ArrayList<>();

        // Process points by segments (separated by NaN)
        for (Plot3DPoint p : points) {
            if (Double.isNaN(p.getX())) {
                // End of segment, smooth and add to result
                if (!currentSegment.isEmpty()) {
                    smoothed.addAll(smoothSegment(currentSegment));
                    currentSegment.clear();
                }
                smoothed.add(p); // Add the separator
                inSegment = false;
            } else {
                // Add point to current segment
                currentSegment.add(p);
                inSegment = true;
            }
        }

        // Handle last segment
        if (!currentSegment.isEmpty()) {
            smoothed.addAll(smoothSegment(currentSegment));
        }

        return smoothed;
    }

    /**
     * Find the index of the nearest point to a reference point
     */
    private static int findNearestPointIndex(Plot3DPoint reference, List<Plot3DPoint> points) {
        int nearest = 0;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < points.size(); i++) {
            Plot3DPoint candidate = points.get(i);
            double distance = Math.sqrt(
                    Math.pow(reference.getX() - candidate.getX(), 2) +
                            Math.pow(reference.getY() - candidate.getY(), 2));

            if (distance < minDistance) {
                minDistance = distance;
                nearest = i;
            }
        }

        return nearest;
    }

    /**
     * Apply Gaussian smoothing to a single curve segment
     */
    private static List<Plot3DPoint> smoothSegment(List<Plot3DPoint> segment) {
        if (segment.size() < 4)
            return segment; // Not enough points to smooth

        List<Plot3DPoint> smoothed = new ArrayList<>();

        // Always keep first and last points unchanged
        smoothed.add(segment.get(0));

        // Apply Gaussian smoothing to interior points
        for (int i = 1; i < segment.size() - 1; i++) {
            // Center point
            Plot3DPoint p0 = segment.get(i);

            // Neighboring points
            Plot3DPoint p1 = segment.get(Math.max(0, i - 1));
            Plot3DPoint p2 = segment.get(Math.min(segment.size() - 1, i + 1));

            // Gaussian weights (higher weight to center point)
            double w0 = 0.6;
            double w1 = 0.2;
            double w2 = 0.2;

            // Weighted average of coordinates
            double x = w0 * p0.getX() + w1 * p1.getX() + w2 * p2.getX();
            double y = w0 * p0.getY() + w1 * p1.getY() + w2 * p2.getY();
            double z = w0 * p0.getZ() + w1 * p1.getZ() + w2 * p2.getZ();

            smoothed.add(new Plot3DPoint(x, y, z));
        }

        // Add last point
        smoothed.add(segment.get(segment.size() - 1));

        return smoothed;
    }

    /**
     * Simplify point list by removing redundant points
     */
    private static List<Plot3DPoint> simplifyPointList(List<Plot3DPoint> points, double stepX, double stepY) {
        if (points.size() <= 3) {
            return points;
        }

        List<Plot3DPoint> simplified = new ArrayList<>();
        simplified.add(points.get(0)); // Always keep first point

        // Minimum squared distance between points
        double minDistSq = Math.min(stepX, stepY) * Math.min(stepX, stepY) * 0.25;

        // Keep track of whether current point is part of a separator section
        boolean inSeparator = false;

        for (int i = 1; i < points.size() - 1; i++) {
            Plot3DPoint prev = simplified.get(simplified.size() - 1);
            Plot3DPoint curr = points.get(i);
            Plot3DPoint next = points.get(i + 1);

            // Always add NaN separator points
            if (Double.isNaN(curr.getX())) {
                simplified.add(curr);
                inSeparator = true;
                continue;
            }

            // Reset separator flag after NaN point
            if (inSeparator) {
                simplified.add(curr);
                inSeparator = false;
                continue;
            }

            // Distance to previous point
            double distSq = Math.pow(prev.getX() - curr.getX(), 2) +
                    Math.pow(prev.getY() - curr.getY(), 2);

            // Angle formed by prev-curr-next
            double angle = 0;
            if (!Double.isNaN(next.getX())) {
                double dx1 = curr.getX() - prev.getX();
                double dy1 = curr.getY() - prev.getY();
                double dx2 = next.getX() - curr.getX();
                double dy2 = next.getY() - curr.getY();

                // Calculate angle using dot product
                double dot = dx1 * dx2 + dy1 * dy2;
                double mag1 = Math.sqrt(dx1 * dx1 + dy1 * dy1);
                double mag2 = Math.sqrt(dx2 * dx2 + dy2 * dy2);

                if (mag1 > 0 && mag2 > 0) {
                    angle = Math.acos(dot / (mag1 * mag2));
                    if (Double.isNaN(angle))
                        angle = 0;
                }
            }

            // Keep point if it's far enough away or forms a significant angle
            if (distSq >= minDistSq || Math.abs(angle) >= 0.15) {
                simplified.add(curr);
            }
        }

        simplified.add(points.get(points.size() - 1)); // Always keep last point

        return simplified;
    }

    /**
     * Draw intersection curves on the graphics context with enhanced rendering
     */
    public static void drawIntersectionCurves(
            Graphics2D g2d, Plot3DTransformer transformer,
            List<List<Plot3DPoint>> intersections,
            Plot3DModel model, Plot3DView view,
            double displayScale, int xOffset, int yOffset) {

        if (intersections.isEmpty()) {
            return;
        }

        // Save original stroke and rendering hints
        Stroke originalStroke = g2d.getStroke();
        Object originalAntialias = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);

        // Enable antialiasing for smoother curves
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Set color and stroke for intersection lines
        g2d.setColor(INTERSECTION_COLOR);
        g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        // Transform parameters
        double xCenter = (view.getXMax() + view.getXMin()) / 2;
        double yCenter = (view.getYMax() + view.getYMin()) / 2;
        double zCenter = (model.getZMax() + model.getZMin()) / 2;

        // Calculate normalization factor
        double xRange = view.getXMax() - view.getXMin();
        double yRange = view.getYMax() - view.getYMin();
        double zRange = model.getZMax() - model.getZMin();
        double maxRange = Math.max(xRange, Math.max(yRange, zRange));
        double factor = 1.0 / maxRange;

        // Rotation parameters
        double angleX = Math.toRadians(view.getRotationX());
        double angleY = Math.toRadians(view.getRotationY());
        double angleZ = Math.toRadians(view.getRotationZ());

        // Precalculate sin and cos values
        double sinX = Math.sin(angleX);
        double cosX = Math.cos(angleX);
        double sinY = Math.sin(angleY);
        double cosY = Math.cos(angleY);
        double sinZ = Math.sin(angleZ);
        double cosZ = Math.cos(angleZ);

        // Draw each intersection curve using paths for better quality
        for (List<Plot3DPoint> curve : intersections) {
            Path2D path = new Path2D.Double();
            boolean pathStarted = false;

            // Transform all points first to prepare for depth sorting
            List<int[]> screenPoints = new ArrayList<>();
            List<Double> zValues = new ArrayList<>();

            for (Plot3DPoint point : curve) {
                // Skip NaN points (curve separators)
                if (Double.isNaN(point.getX())) {
                    // Add a marker for path breaks
                    screenPoints.add(null);
                    zValues.add(Double.NaN);
                    continue;
                }

                // Transform point
                Plot3DPoint transformedPoint = transformer.transformPoint(
                        point.getX(), point.getY(), point.getZ(),
                        xCenter, yCenter, zCenter,
                        factor, view.getScale(),
                        sinX, cosX, sinY, cosY, sinZ, cosZ,
                        view.getPanX(), view.getPanY());

                // Store z-value for potential depth sorting
                zValues.add(transformedPoint.getZ());

                // Project to screen coordinates
                int[] screenPos = transformer.projectToScreen(
                        transformedPoint, displayScale, xOffset, yOffset);

                screenPoints.add(screenPos);
            }

            // Build the path
            for (int i = 0; i < screenPoints.size(); i++) {
                int[] screenPos = screenPoints.get(i);

                // Handle separators (null points)
                if (screenPos == null) {
                    pathStarted = false;
                    continue;
                }

                // Start a new path segment
                if (!pathStarted) {
                    path.moveTo(screenPos[0], screenPos[1]);
                    pathStarted = true;
                } else {
                    path.lineTo(screenPos[0], screenPos[1]);
                }
            }

            // Draw the complete path
            g2d.draw(path);
        }

        // Restore original graphics settings
        g2d.setStroke(originalStroke);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, originalAntialias);
    }

    /**
     * Clear the intersection cache
     */
    public static void clearCache() {
        synchronized (intersectionCache) {
            intersectionCache.clear();
        }
    }

    /**
     * Get the color used for intersection lines
     */
    public static Color getIntersectionColor() {
        return INTERSECTION_COLOR;
    }

    /**
     * Shutdown the executor service
     */
    public static void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}