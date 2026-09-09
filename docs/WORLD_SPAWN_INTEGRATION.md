# Server spawn integration

When an Earthward pilot world creates its initial spawn, the server now independently reloads and verifies both installed package parts, rebuilds the local geometry index, samples the real DSM, and runs the bounded safe-spawn validator. Only a successful result replaces vanilla spawn selection.

If either package is absent/modified, elevation coverage is missing, relief is too steep, or no clear 3×3 area exists within 256 metres, the event is left untouched. The code does not claim an unsafe coordinate is valid and does not flatten terrain.

The current requested coordinate is the local pilot origin. The map selection must next persist the user's requested X/Z into the world-creation contract so this same validator can search from that point.
