# Chunk placement plan

Raster masks now become immutable per-chunk placement plans. Roads receive an asphalt placement at the generated terrain surface. Building footprints receive foundations, perimeter walls and roofs, producing bounded functional shells rather than solid filled volumes.

Both road width (3 m) and building height (3 m walls plus roof) are explicitly marked as inferred because the pilot runtime model does not yet expose sufficiently complete verified width/height tags. Geometry coordinates and the one-metre world scale are unchanged.

The plan is pure data and can be prepared outside the world thread. Applying its placements to Minecraft chunks, safe-spawn checks, generated-block ownership and client loading remain separate verification steps.
