# Release process

Run the workstation build and tests. Review the debug artifact and GitHub Actions logs, including Android lint and both emulator API levels. Verify the physical acceptance checklist and preserve the results. Record measured startup and memory with conditions and sample count.

Update version and changelog after validation. For a future release build, remove the SDK linker's debug flag and sign with a dedicated release key stored outside the source tree. Document artifact hashes and the installation and rollback routes. Debug and release builds signed by different keys require separate application IDs or a documented migration.

External publication and repository visibility require owner approval. Share only reviewed source, synthetic screenshots and sanitized benchmark results.

## Repository visibility review

Audit the proposed public tree and all reachable branches and tags, including commit metadata, before requesting final owner approval. Check credentials and signing material, ADB details, local paths, device dumps and third-party assets. Confirm redistribution rights for all screenshots, including historical captures and icons visible in them. Check the Apache 2.0 license and preserve required attribution. Record findings by category without copying sensitive values. Removing a file in a new commit leaves its earlier versions in history.

Promote validated changes through reviewed pull requests into develop and then main. Review the entire main comparison and verify required checks before merging. Verify README navigation, capture dimensions and privacy, and the distinction between emulator and X5 results. Confirm the visibility decision separately after the audit.

## Portfolio source snapshot

The public history starts with the reviewed v0.2.7-alpha source and its technical records. The two README captures show the launcher running in an Android 13 emulator at 1920 × 1080 and 1280 × 720. Focus Mode shows only the project test activities, packaged locally with original geometric icons. Historical development captures remain in the private development archive.
