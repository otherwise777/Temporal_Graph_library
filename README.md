# Temporal_Graph_library Tink
This repository contains the library Tink, a library to do temporal graph analytics with the data stream enging Flink. This readme contains the basic setup of the library. This is still work in progress, it will likely be expanded later on.

![alt text](https://github.com/otherwise777/Temporal_Graph_library/blob/master/img/logo3.png?raw=true)

# project information
Since everything in this library is about graphs we use the ![Gelly library](https://ci.apache.org/projects/flink/flink-docs-release-1.3/dev/libs/gelly/index.html) extensively. This is a library that lets us create/ modify and delete notes and vertices from the graph, all in a ditributed manner.
There are different definitions available about temporal graphs, for this project we define a temporal graph as a set of edges E and a set of vertices V, a vertice consists of the node ID and a label (K,L). An edge consists of a source node, target node, label, start and ending time (V_1,V_2,L,T_1,T_2). As you can see, I only consider the temporal aspects on edges.
One of the more important aspects of this library is a temporal shortest path. A shortest temporal path can be defined in different ways depending on the way that you interpret the data. In this project we differentiate 2 kinds of shortest temporal paths:
* EAT, earliest arrival time. In a temporal graph, the shortest temporal path EAT from v to w is the path which has the earliest arrival time. If your temporal graph is the road network then your shortest path EAT would be the path that brings you from source to destination the fastest.
* FP, fastest pathIn a temporal graph, the shortest temporal path FP from v to w is the path which has the shortest travel time, meaning if you can leave at different times, which time is the best time to leave if you want the least amount of travel time.
Many more definitions can be thought of when reasoning about time and shortest paths, like the least amount of transits, the shortest average time per edge, latest departure time etc. I worked out 2 of these and implemented them in this library, The formal mathematical definitions of these paths can be found in my * ![thesis](http://www.win.tue.nl/~gfletche/ligtenberg2017.pdf)

# Setup
After you've pulled the project let maven import the projects dependancies and simply run the file examples/analyze_rita.java
This should run and give you some results.

# Dataset Rita
One of the datasets available in this repository is the dataset Rita, this dataset contains information about all the flights from februari 2017. This dataset contains many columns under which the starting date, the starting time, the arrival time, the carrier ID and airports. Using this dataset we can model a temporal graph with airports as nodes and the departure and arrival time as the temporal edge. Before we can do some analytics on the dataset we first have to clean it, We want to be able to calculate the travel time for example, and the dataset has the time in the format hhmm, meaning we cannot simply contract one from another, luckely this is easily done in Flink.

# example
This is the example code found in \src\main\java\Tgraphs\analyze_rita.java, this section explains line by line what is happening.


# background
I made Tink in 2017 for my master thesis for graduation computer science and engineering at the Eindhoven University of Technology. I will present Tink at the Flink forward conference in september 2017 in Berlin. Some sources:
* ![Thesis 2017](http://www.win.tue.nl/~gfletche/ligtenberg2017.pdf)
* ![Video presentation of Tink](https://www.youtube.com/watch?v=ZBvtqLyjPqE)
* ![Presentation at Flink forward conference(not available yet)](https://berlin.flink-forward.org/)

# roadmap
* Configure Maven in such a way that Flink doesn't has to be pulled as a library
* Complete all the comments
* Clean up the project with the testing code

