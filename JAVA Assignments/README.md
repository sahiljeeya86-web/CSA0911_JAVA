# CSA09-192525200-Assignment1

- **Name:** Sahil J.S
- **Register number:** 192525200
- **Course:** CSA0911 - Programming in Java (CO1)
- **Java version:** OpenJDK 21.0.12 (javac 21.0.12)

## How to compile and run

Each task folder is self-contained. From the task folder:

    javac -d out $(find . -name "*.java")
    java -cp out <main class>

| Folder | Main class | Output file(s) |
|---|---|---|
| Task03_ParkingFacility | ParkingFacility | run.txt |
| Task06_ConstructorTrace | CtorTrace | ctor_trace.txt |
| Task08_FeeDemo | FeeDemo | run.txt |
| Task11_Day1_System/src | parking.app.TestSuite, parking.app.CampusApp | test_output.txt, campus_output.txt |
| Task12_Day4_System/src | parking.app.TestSuite, parking.app.CampusApp, parking.app.Analysis | test_output.txt, campus_output.txt, analysis_output.txt, wrapper_demo.txt |

For the packaged systems, compile from inside the `src` folder:

    cd Task12_Day4_System/src
    javac -d ../out $(find . -name "*.java")
    java -cp ../out parking.app.TestSuite

## Result of each task

- **Task 3 (Complete the Code):** output matches the expected output exactly.
- **Task 8 (Complete the Code, FeeDemo):** output matches the expected output exactly (last line `true`).
- **Day 1 system:** 16 of 16 tests pass; the scenario collection is Rs 236.
- **Day 4 system:** 27 of 27 tests pass; the scenario collection is Rs 516.
- **Analysis:** timings in `analysis_output.txt` are from one run on the machine used; values vary slightly between runs.
