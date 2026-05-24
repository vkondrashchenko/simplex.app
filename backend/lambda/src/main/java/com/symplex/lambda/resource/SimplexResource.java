package com.symplex.lambda.resource;

import com.symplex.core.parser.Polynomial;
import com.symplex.core.parser.PolynomialParser;
import com.symplex.core.renderer.RenderParams;
import com.symplex.core.renderer.SimplexGrid;
import com.symplex.core.renderer.SvgRenderer;
import com.symplex.lambda.dto.ParseRequest;
import com.symplex.lambda.dto.ParseResponse;
import com.symplex.lambda.dto.RenderRequest;
import com.symplex.lambda.dto.RenderResponse;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.*;
import java.util.stream.IntStream;

@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SimplexResource {

    @GET
    @Path("/health")
    public Response health() {
        return Response.ok(Map.of("status", "UP")).build();
    }

    @POST
    @Path("/parse")
    public ParseResponse parse(ParseRequest req) {
        if (req == null || req.equation() == null || req.equation().isBlank()) {
            throw new BadRequestException("equation must not be blank");
        }
        Polynomial poly = PolynomialParser.parse(req.equation());
        List<String> names = IntStream.rangeClosed(1, poly.maxVarIndex())
                .mapToObj(i -> "x" + i)
                .toList();
        return new ParseResponse(poly.maxVarIndex(), poly.monomials().size(), names);
    }

    @POST
    @Path("/render")
    public RenderResponse render(RenderRequest req) {
        if (req == null || req.equation() == null || req.equation().isBlank()) {
            throw new BadRequestException("equation must not be blank");
        }

        Polynomial poly = PolynomialParser.parse(req.equation());
        int varCount = poly.maxVarIndex();

        // Determine which variables are fixed vs free (1-based)
        Map<Integer, Double> fixedVars = req.fixedVars() != null ? req.fixedVars() : Map.of();

        // Collect free variable indices (1-based) — those not in fixedVars
        List<Integer> freeList = new ArrayList<>();
        for (int i = 1; i <= varCount; i++) {
            if (!fixedVars.containsKey(i)) {
                freeList.add(i);
            }
        }

        if (freeList.size() != 3) {
            throw new BadRequestException(
                    "Exactly 3 free variables required for simplex rendering, but got " + freeList.size()
                    + " (total vars: " + varCount + ", fixed: " + fixedVars.size() + ")");
        }

        // Build x[] (0-indexed), pre-populate fixed values; free positions will be overwritten per pixel
        double[] xFixed = new double[varCount];
        double fixedSum = 0.0;
        for (Map.Entry<Integer, Double> entry : fixedVars.entrySet()) {
            int idx0 = entry.getKey() - 1;  // convert 1-based to 0-based
            xFixed[idx0] = entry.getValue();
            fixedSum += entry.getValue();
        }
        double ll = 1.0 - fixedSum;  // total weight available for the 3 free variables

        // freeIdx: 0-based positions of the 3 free variables
        int[] freeIdx = freeList.stream().mapToInt(i -> i - 1).toArray();

        // Free variable names for vertex labels (e.g. ["x2", "x3", "x4"])
        String[] freeVarNames = freeList.stream().map(i -> "x" + i).toArray(String[]::new);

        // Build render params
        RenderParams params = switch (req.mode()) {
            case REGION -> RenderParams.region(req.regionMin(), req.regionMax(), req.pixelStep());
            case ISOLINE_SINGLE -> RenderParams.isolineSingle(req.isoValue(), req.precision(), req.pixelStep());
            case ISOLINE_RANGE -> RenderParams.isolineRange(
                    req.isoFrom(), req.isoTo(), req.isoStep(), req.precision(), req.pixelStep());
        };

        // Sweep + render
        SimplexGrid grid = SimplexGrid.sweep(poly, xFixed, freeIdx, ll, params.pixelStep());
        String svg = SvgRenderer.render(grid, params, freeVarNames, ll);

        return new RenderResponse(svg, grid.min(), grid.max());
    }
}
