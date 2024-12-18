##  Build process

Walnut is currently built with the [Gradle](https://gradle.org)
build system, and supports:
- Unit testing via [JUnit](https://junit.org)
  - Unit tests are stored in `src/test/`
  - Integration testing is done via the unit testing process, to show code coverage
  - Integration testing runs by default. Be careful; it has the ability to overwrite some Automata text files.
  - To rebuild integration test files, uncomment the `@Test` attribute before `createTestCases()` in IntegrationTests.java
- Code coverage reports via [JaCoCo](https://jacoco.org/jacoco)
- Static analysis via [SpotBugs](https://spotbugs.github.io)
  - Disabled by default, since it flags a few issues which fail the build. Uncomment the SpotBugs code in the `build.gradle` file to enable. 

To build Walnut, run `build.sh`.
(Note additional comments in the file.)
To run Walnut, run `walnut.sh.`
(Note additional comments in the file.)

## Generated files

`build.sh` will create a `build/` directory.
There are many files there, but in particular:
- build/libs contains the "thin" and "fat" [JAR](https://en.wikipedia.org/wiki/JAR_(file_format)) files. The fat JAR (Walnut-all.jar) packages up dependent libraries which are necessary for execution.
- build/reports/tests/test/index.html shows output of unit tests.
- build/jacocoHtml/index.html shows code coverage results.
- build/reports/spotbugs/main.html shows SpotBugs output (if enabled).

Integration tests will potentially alter several other files.
It's recommended that the integration test code be refactored,
such that all its files are generated at `src/test/resources`.

## Libraries

Walnut uses the following libraries:
- [fastutil](https://github.com/vigna/fastutil): memory-efficient collections
- [dk.brics.automaton](https://brics.dk/automaton) - DFA/NFA implementation
  - Much of this is not used; this library uses `char` primitives for states, which would mean a limit of 65536 states

## Optimizations

[Heap dump analysis](https://eclipse.org/mat/) and [profiling](
https://www.oracle.com/java/technologies/jdk-mission-control.html)
show that Walnut consumes its memory and time in `subsetConstruction`.
Memory optimizations that improve both:
- Prefer primitives to wrappers (e.g., `int` vs `Integer`) when possible. This is a ~2x reduction in memory usage, and saves [boxing](https://docs.oracle.com/javase/tutorial/java/data/autoboxing.html) time. Objects like `ArrayList` only support wrappers, so instead objects like fastutil's `IntArrayList` are used.
- `d` (the transition function) is very large. This is because it has a `List` instance for every state. During determinizing, there's only one state in the `List`, so we're able to use primitives instead for a ~8x memory saving.
- The set of states in `subsetConstruction` is trimmed, to avoid storing empty space.

## Documentation
- Documentation/ is an old Javadoc directory. This could be rebuilt, even automatically via Gradle.
- Manual.pdf
- Walnut 5 Documentation.txt
- README.md
- developer_notes.md

## IDEs

Gradle is supported by several IDEs including
[Eclipse](https://eclipse.org) and
[IntelliJ](https://www.jetbrains.com/idea).
In particular, in IntelliJ you can simply open the Gradle file
to create a project.

