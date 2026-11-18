# CSA09-192525200-Assignment3

- **Name:** Sahil J.S
- **Register number:** 192525200
- **Course:** CSA0911 - Programming in Java (CO3, pair work)
- **Java version:** OpenJDK 21.0.12 (javac 21.0.12)

## How to compile and run

Each question folder is self-contained. From a folder that has a `src` directory:

    cd Q1_ATM_Exceptions/src && javac -d ../out *.java && java -cp ../out AtmTests

For programs without a `src` folder, compile the single file in that folder:

    javac -d out Foo.java && java -cp out Foo

| Question | Main program(s) | Raw results |
|---|---|---|
| Q1 ATM exceptions | AtmTests (src/); AlternativeCost (experiments/); BroaderFirst (demo_broader_first/) | results_atm.txt, results_q1_alt.txt, javac_output.txt |
| Q2 Portal exceptions | StorageDemo (complete_code_after/); PortalDemo (portal/src/); ChainCost (experiments/) | output_expected_check.txt, portal/results_portal.txt, results_nocause.txt, results_chain_cost.txt |
| Q3 Resources and debugging | DemoRun (src/); ReportCost (experiments/, run with args n badAt version) | results_q3.txt, results_report_cost_25000_fresh.txt |
| Q4 Threads and stations | ScanExam (src/, modes: main, run-vs-start, daemon, race, priority); PoolCost (experiments/) | results_order_runs.txt, results_race_runs.txt, results_priority_runs.txt, results_q4_behaviour.txt, results_pool_cost.txt |
| Q5 Race conditions | SeatBooking (complete_code_unsync/ and complete_code_sync/); Race (experiments/) | results_unsync_10runs.txt, results_sync_10runs.txt, results_race.txt |
| Q6 Deadlock and wait/notify | TokenCounter (complete_code_while/); WaitVariants, NotifyTwoProducers, AltQueueLock (experiments/); Transfers (partA_transfers/, run with arg unsafe or ordered) | results_token_while.txt, results_wait_variants.txt, results_notify_two_producers.txt, results_transfers.txt, results_alt_queue_lock.txt |
| Q7 Fault-tolerant processor | Processor (src/, modes: handset; full N threads io_ms); AltStreams (experiments/) | results_handset.txt, results_bench_io1.txt, results_bench_io1_more.txt, results_bench_cpu.txt, results_alt_streams.txt |

The generated test files (for example the 10,000 score files in Q7 and the store files in Q1 and Q3) are not included; the programs recreate them when run.

## Results summary

- **Q1:** all ten ATM inputs follow their designed path; the broader-first catch order is rejected by the compiler.
- **Q2:** the completed StorageDemo prints the expected three lines; without the cause the program fails with a NullPointerException on the Cause line.
- **Q3:** the leaky version holds every opened file until the end, reaches "Too many open files" at 19,996 handles on 25,000 stores; the finally and try-with-resources versions never hold more than one file.
- **Q4:** the stations finish in a fixed order when their sleep times differ and in varying orders when their work is equal; the daemon thread is lost when main ends.
- **Q5:** the unsynchronised booking did not double-book in 10 runs on this one-core machine; with a widened race window it double-booked in 1,995 of 2,000 trials; both synchronised versions never did.
- **Q6:** the completed TokenCounter prints a sum of 210 in every run; with `if` instead of `while` the counter gives wrong sums in about half of the trials; the unsafe transfers deadlock and the lock-ordered transfers do not.
- **Q7:** the processor's counts equal the generator's counts for 10,000 files (ok 8,992, corrupt 514, missing 91, internal 403, unhandled 0); the time falls from 11.8 s with one thread to 1.6 s with eight (simulated I/O).
