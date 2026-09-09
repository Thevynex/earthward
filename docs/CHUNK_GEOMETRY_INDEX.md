# Chunk-local geometry index

Earthward now projects a verified package once into the local 1 metre/block coordinate system and builds an immutable 16×16 metre chunk index. A later world generator can request only the road/building features intersecting one chunk instead of scanning the whole region.

Safety and resource boundaries:
- source package validation still happens before parsing/indexing;
- index and returned feature/point collections are immutable copies;
- negative coordinates use mathematical floor division;
- maximum feature memberships across chunks: 2,000,000;
- non-finite or unsupported-range coordinates are rejected;
- no save writes and no world-thread mutations are performed.

This is generation infrastructure, not generated terrain. Elevation sampling, polygon rasterization, road widths, building heights, update/version ownership and safe spawn remain required before Earthward world creation can be enabled.
