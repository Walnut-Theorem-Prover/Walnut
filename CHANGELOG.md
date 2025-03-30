# Changelog

All notable changes to Walnut will be documented here. Format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased] - Author: John Nicol

- Source: [https://github.com/Walnut-Theorem-Prover/Walnut]

### Added

- Versioning output to Walnut.
- Session functionality. Each run of Walnut writes to a new session, making it easier to organize and not overwrite previous results.
- Ability to specify session and home directories from command line.
- Walnut .txt files and scripts allow comments.
- Dead states are removed (trim) before determinization.
- Leverage the [AutomataLib](https://github.com/LearnLib/automatalib) library.
- (UNPUBLISHED) Leverage the OTF library.
- Allow writing NFAs to the [BA](https://languageinclusion.org/doku.php?id=tools) format, including intermediates.
- (UNPUBLISHED) Determinization strategy choices:
  * [Brzozowski's algorithm](https://en.wikipedia.org/wiki/DFA_minimization#Brzozowski's_algorithm)
  * OTF-CCL and OTF-CCLS
  * Brzozowski-OTF-CCL and Brzozowski-OTF-CCLS
- Metacommands: "strategy" (see above) and "export", which allows exporting intermediate automata to BA format.
- Command "describe" describes an automaton.

### Fixed

- Major performance improvements, particularly in product automata construction, Walnut file I/O, and the `test` command.
- Fixed OOM error when writing large Graphviz files (reported by Pierre Ganty).
- Removed unnecessary determinizations when handling leading/trailing zeros.
- Drastically increased code re-use, testing, and code coverage.
- Fixed unexpected behavior for integer rounding when doing division with negative numbers (fixed by Jonathan Yang)
        
### Changed

- "eval" and "def" commands are now the same. Before, "eval" didn't write files to "Automata Library", which was confusing.
- "draw" command (which wrote automata to .gv files) is replaced with the more generic "export" command (which can currently write to .ba, .gv, or .txt files).
- Help documentation organized into topics.

### Removed

- JVM backwards compatibility. Walnut now requires JDK 17 or higher.

## [Walnut 6.2] - 2024-03-30 - Author: Anatoly Zavyalov

- Source: [https://github.com/firetto/Walnut]

### Added

- Help documentation
- Automata operations
- alphabet command
- Fixing leading and trailing zeroes
- Delimiters for word automata
- Drawing automata and word automata
- Reversing automata

### Fixed

- Bug fixes
- Major performance improvements, particularly in Subset Construction (~7x memory reduction, ~2x speedup) and multiplication

### Changed

- Walnut now builds with [Gradle](https://gradle.org/)

## [Walnut 5] - 2023-11-26 - Author: Anatoly Zavyalov

- Source: [https://github.com/firetto/Walnut/tree/walnut5]
- [Additional documentation](https://cs.uwaterloo.ca/~shallit/walnut-5-doc.txt)

### Added

- Transducing k-automatic sequences
- Converting number systems
- Reversing word automata
- Minimizing word automata

### Fixed

- Bug fixes
- Logging Improvements

### Changed

- Changes to the reversal (`` ` ``) operation

## [Walnut 4] - 2022-08-15 - Author: Kai Hsiang Yang

### Added

- New commands, see [https://cs.uwaterloo.ca/~shallit/Walnut4-Documentation.txt]

## [Walnut 3] - 2021-09-06 - Author: Laindon C. Burnett

### Added

- New commands, see [https://cs.uwaterloo.ca/~shallit/Walnut3-NewCommands.pdf]

## previous versions authored by Hamoon Mousavi and updated by Aseem Raj Baranwal
