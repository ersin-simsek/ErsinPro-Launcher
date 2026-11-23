# Prototype baseline

Phase 0 audited commit `c551493` and preserved it under the local tag `prototype-x5-baseline`. The ignored 12,696-byte prototype APK stays at `build/x5-launcher-debug.apk`.

The baseline contained one Java Activity, a native ListView and a PowerShell SDK build. It queried MAIN/LAUNCHER and MAIN/LEANBACK_LAUNCHER, deduplicated by component, launched explicit intents and handled initial remote OK without a selected row. Labels and settings were hardcoded in Turkish. There were no launcher preferences or application dependencies.

JDK 17, Android Platform 34, Build Tools 34.0.0 and ADB were verified. Android Studio contains a Kotlin IDE plugin. Standalone Gradle and Kotlin commands were absent from PATH. An Android 33 emulator profile was available.

The project owner reports that the prototype was installed and tested on the X5. Reference hardware reported by the projector includes Android 9/API 28, 1 GB RAM, 8 GB storage and V1.0.0.7 firmware dated 2022-12-28. Projector ADB access and a model-specific vendor recovery procedure remain unverified. [Yasomi support](https://www.yasomi.com.tr/teknik-destek)

Phase 1 retains the SDK toolchain and builds ErsinPro as a separate app. Phase 2 covers physical controls, HOME selection, rollback and repeatable benchmarks. Phase 3 evaluates optional diagnostics and adaptive behavior against a measured baseline.
