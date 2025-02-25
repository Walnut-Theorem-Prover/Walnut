# Changelog

All notable changes to the Walnut project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased] - Author: John Nicol

- Source: [https://github.com/Walnut-Theorem-Prover/Walnut]

### Added

- Versioning output to Walnut.
- Session functionality. Each run of Walnut writes to a new session, making it easier to organize and not overwrite previous results.
- Walnut .txt files now allow comments.
- Dead states are removed (trim) before determinization.
- (INCOMPLETE) Bisimulation reduction before determinization.
- Leveraging the [AutomataLib](https://github.com/LearnLib/automatalib) library.
- Allow reading and writing NFAs to/from the [BA](https://languageinclusion.org/doku.php?id=tools) format, including intermediates.
- (INCOMPLETE, UNPUBLISHED) Leveraging the OTF library.
- (INCOMPLETE, UNPUBLISHED) Additional determinization strategy choices:
  * [Brzozowski's algorithm](https://en.wikipedia.org/wiki/DFA_minimization#Brzozowski's_algorithm)
  * OTF
  * Brzozowski-OTF

### Fixed

- Major performance improvements, particularly in product automata construction and Walnut file I/O.
- Fixed OOM error when writing large Graphview files (reported by Pierre Ganty).
- Removed unnecessary determinizations when handling leading/trailing zeros.
- Increased testing and code coverage.
- Fixed unexpected behavior for integer rounding when doing division with negative numbers (fixed by Jonathan Yang)
        
### Changed

- Near-total rewrite.
- "eval" and "def" commands are now the same. Before, "eval" didn't write files to "Automata Library", which was confusing.
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
