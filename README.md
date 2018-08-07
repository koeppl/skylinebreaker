Skyline Breaker 
===============

This is an implementation of the algorithm [1] computing the Pareto front of a given sequential input connection.
It uses the LSD-tree [2], a spatial tree structure for indexing fine-grained feature vectors.
The implementation applies concurrency whenever possible.
It can be used with the benchmark tool located at https://github.com/sven-wi/SkylineCompare

[1]: Dominik Köppl: Breaking skyline computation down to the metal: the skyline breaker algorithm. IDEAS 2013: 132-141
[2]: Andreas Henrich, Hans-Werner Six, Peter Widmayer: The LSD tree: Spatial Access to Multidimensional Point and Nonpoint Objects. VLDB 1989: 45-53

