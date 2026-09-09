# Pilot road and building rasterization

Verified OSM geometry can now be converted deterministically into a 16×16 per-chunk surface mask at one block per metre. Building polygons are filled and take precedence over road cells. Road centerlines are buffered to a temporary inferred width of 3 metres because reliable width tags are not yet represented by the runtime geometry model.

Every output mask records that road width was inferred. No source geometry is changed, and returned mask arrays are defensive copies. Distant chunks remain empty rather than receiving invented features.

This increment creates placement masks only. Applying asphalt, foundations, walls and roofs to generated chunks comes next. Before geographic package updates can safely touch existing worlds, generated block ownership/version metadata must distinguish Earthward-owned blocks from player edits.
