# Contributing

Build and test with the commands in the README. Include emulator or physical-device results for changes to navigation, rendering or launcher selection. Update all three locale resources when adding UI text. Keep commits focused and describe observable behavior. For hardware reports, use the [compatibility template](docs/compatibility.md) and remove private identifiers.

## Branches and pull requests

Start from the default branch and use a focused branch. Run local checks, review the staged diff and open a pull request against `main`. Integrate after the hosted checks pass. Use Conventional Commits and preserve historical attribution.

Record the checks run, failures, limitations and next validation step. Keep captures with device or account data and local signing material out of Git. See the [release process](docs/release.md) for public-readiness checks.