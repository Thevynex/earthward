# Bounded safe-spawn validation

Earthward now has a deterministic safe-spawn search contract. Starting from the requested map coordinate, it searches square rings up to a hard limit of 256 metres and accepts the nearest candidate only when:

- the full 3×3 footprint contains no rasterized road or building;
- all nine terrain columns are available from the installed elevation coverage;
- local relief across the footprint differs by at most one block;
- the surface and two-block player headroom fit inside the supported Minecraft height range.

The result records the final X/Y/Z coordinate, examined candidate count and displacement from the requested location. An absent result remains a hard failure; Earthward does not silently invent or flatten a spawn.

This validator is now tested as pure Java. It still must be connected to the map selection and actual world-creation context before the create button may be enabled.
