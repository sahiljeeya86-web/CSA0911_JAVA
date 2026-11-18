# CSA09-192525200-Assignment2

- **Name:** Sahil J.S
- **Register number:** 192525200
- **Course:** CSA0911 - Programming in Java (CO2)
- **Java version:** OpenJDK 21.0.12 (javac 21.0.12)

## How to compile and run

Each question folder is self-contained. From a question folder:

    javac -d out $(find . -name "*.java")
    java -cp out <MainClass>

| Question | Main class | Raw results |
|---|---|---|
| Q1 String, StringBuilder, StringBuffer | TicketBench, Concurrency (in experiments/) | results_q1.txt, results_q1_concurrency.txt |
| Q2 ArrayList, LinkedList, ArrayDeque | ListBench (in experiments/) | results_q2.txt |
| Q3 HashSet and hashCode-equals | Attendance (complete_code_after/); ContractTests (experiments/) | output_before.txt, output_after.txt, results_q3.txt |
| Q4 HashMap, TreeMap, LinkedHashMap | PharmacyBench (experiments/) | results_q4.txt |
| Q5 Generics and Comparator | LeaderboardDemo (complete_code/); RankingLab (experiments/) | output.txt, results_q5.txt |
| Q6 PriorityQueue | Triage (complete_code/); TriageLab (experiments/) | output.txt, results_q6.txt |
| Q7 Redesign | RedesignLab (experiments/) | results_q7.txt |

Results are from one run on the machine used; timings vary between runs by a few tens of per cent, so the report relies on trends and operation counts. The experiments take up to several minutes each.

## Result of each task

- **Q1:** + concatenation is the slowest for long tickets; StringBuilder is fastest for per-ticket text; unsynchronised StringBuilder lost data in 8 of 10 shared-log trials, StringBuffer in none.
- **Q2:** ArrayList is appropriate for playlists up to about 10,000 songs (front insertion 378 ns); ArrayDeque for the Up Next queue.
- **Q3:** the completed Attendance program prints 2 (3 before the fix); the four designed tests confirm the hashCode-equals contract predictions.
- **Q4:** LinkedHashMap is the chosen map for the stock (28 ns per lookup at 1,000,000 medicines, added-order report 67 ms, code-order report by sorting, 398 ms).
- **Q5:** BY_RANK passes the tie data set and the property checks; the random-tie comparator fails the TimSort contract in 448 of 1,000 sorts.
- **Q6:** the PriorityQueue serves the 8-patient instance exactly as worked by hand; the aging rule removes starvation.
- **Q7:** the sorted-insertion leaderboard returns identical top-10 lists and cuts comparisons per request from 1,536,673 to 16.7 at 100,000 teams; one prediction (full-ranking speed-up) was wrong and is discussed in the report.
