# Changelog

All notable changes to the Walnut project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased] - Author: John Nicol

- Source: [https://github.com/jn1z/Walnut]

### Added

- Versioning output to Walnut.
- Session functionality. Each run of Walnut writes all of its results to a new session, which makes it much easier to organize and not overwrite previous results.
- Dead states are removed (trim) before determinization.
- (INCOMPLETE) Bisimulation reduction before determinization.
- (INCOMPLETE) Determinization "strategy". It's now possible to determinize with Subset Construction (default), Brzozwski's reversal construction, OTF, or OTF (with Brzozwski reversal).

### Fixed

- Major performance improvements, particularly in product automata construction.
- Unnecessary determinizations in fix leading/trailing zeros have been removed.
- Increased testing and code coverage.

### Changed

- Almost total rewrite.
- "eval" and "def" commands are now the same. Before, "eval" would not write files to "Automata Library", which was confusing.
- Help documentation organized into topics.

### Removed

- JVM backwards compatibility. Walnut now requires JDK 17 or higher.

## [Walnut 6] - 2024-03-30 - Author: Anatoly Zavyalov

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
- Performance improvements, particularly in Subset Construction and multiplication

## [Walnut 5] - 2023-11-26 - Author: Anatoly Zavyalov

- Source: [https://github.com/firetto/Walnut/tree/walnut5]
- Additional documentation: [https://cs.uwaterloo.ca/~shallit/walnut-5-doc.txt]

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
