# Chunk terrain sampling increment

The verified Copernicus DSM can now be sampled into immutable 16×16 Minecraft chunk-column plans. Each block column is inverse-projected from the bounded pilot metre system to EPSG:4326 and bilinearly sampled from the verified grid.

Horizontal scale remains exactly one block per metre. Vertical values receive only a constant Minecraft sea-level translation (`Y = 63 + round(source metres)`); there is no vertical compression or exaggeration. Source metre samples are retained beside the resulting Y values for provenance and diagnostics.

This class performs pure off-thread preparation and does not mutate chunks or saves. The next generator increment must apply these plans on the Minecraft generation thread, enforce dimension height limits, handle coast/water policy, and persist package/version ownership before world creation is enabled.
