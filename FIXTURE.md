# IronJava acceptance fixture

This repository is the deliberately vulnerable Maven fixture for the IronJava pilot. It is a single-module Java 21 service with a root `pom.xml` only. There is no parent POM, no Maven wrapper, and no `.mvn` directory. The default branch stays on the vulnerable dependency versions. Evidence from a local patched rescan is recorded below and is not merged.

The preselected fixable finding is:

| Field | Value |
| --- | --- |
| Coordinate | `org.apache.commons:commons-text` |
| Vulnerable version | `1.9` |
| Patched version | `1.10.0` |
| Advisory | `GHSA-599f-7c49-w659` |
| Alias | `CVE-2022-42889` |
| OSV ecosystem range | introduced `1.5`, fixed `1.10.0` |
| Listed affected versions | `1.5`, `1.6`, `1.7`, `1.8`, `1.9` |

## Repository layout

| Path | Role |
| --- | --- |
| `pom.xml` | Root build. Direct versions, one BOM import, pinned plugins. |
| `src/main/java/dev/ironjava/fixture/` | HTTP health service using Commons Text, SnakeYAML, and Guava. |
| `src/test/java/dev/ironjava/fixture/` | Unit tests, including a JDK HTTP call and a test-scoped Gson round trip. |
| `evidence/` | Maven logs, OSV-Scanner JSON, and trimmed OSV API responses captured on 2026-10-25. |
| Branch `scenario/test-failure` | Same service plus `LegacyDefaultLookupsTest`, which fails when Commons Text is upgraded to `1.10.0`. |

`mvn -B -ntp verify` is the build and test command. It does not skip tests. JDK 21 is required (`maven.compiler.release` is 21 and the enforcer rule is `[21,22)`).

## Toolchain used for this evidence

Captured in `evidence/toolchain.txt`.

| Tool | Pin |
| --- | --- |
| JDK | Ubuntu OpenJDK `21.0.12.1` (build `21.0.12.1+1-1-24.04.4-Ubuntu`) |
| Maven | Apache Maven `3.9.16` (`2bdd9fddda4b155ebf8000e807eb73fd829a51d5`), SHA-512 `831a8591fe20c8243b1dbe7d71e3244f31d1665b0804b2e825e38cbbe5ce0cafb8338851f90780735568773e0a6cd07bbec107cda0b896b008b861075358b6f6` |
| OSV-Scanner | `2.7.0`, commit `bfe2f84abd2f09fe0b83ba7f6dad40d296762f40`, built `2026-10-12T00:39:39Z`, osv-scalibr `0.5.3`, SHA-256 `160c6253e2922e9e0a6a9771c5c4c40a4b3c6090b34a3cdd5e80c96cc2e4218b` |
| Compiler / Surefire / Enforcer / Jar | `3.14.0` / `3.5.3` / `3.5.0` / `3.4.2` |
| JUnit Jupiter | `5.12.2` |

OSV-Scanner `2.7.0` resolves Maven transitive dependencies by default through the deps.dev API (`--data-source=deps.dev`). `--no-resolve` turns that off. `--data-source=native` reads Maven Central instead. The primary scans below use the default. A repeat with `--data-source=native` produced the same five GHSA/coordinate/version tuples (`evidence/osv-scanner-baseline-native.json`).

Documented scanner behavior for this release: transitive Maven resolution is on by default, and the v2.7.0 docs say test dependencies are not supported yet in the computed Maven graph. This run still reported the direct test-scoped artifacts and marked them with `dependency_groups: ["test"]`. The table renderer prints that scope as `(dev)`. JUnit's own transitive test graph appears in the `--all-packages` output. Policy should read Maven scope from the POM and from `dependency_groups`, and should treat a test-scoped finding as manual review.

## Fixable finding

`org.apache.commons:commons-text:1.9` is a direct `compile` dependency with an explicit `<version>` in the root POM.

OSV query `POST https://api.osv.dev/v1/query` for Maven `org.apache.commons:commons-text` `1.9` on 2026-10-25 returned one vulnerability. The trimmed response is `evidence/osv-api/query-org.apache.commons_commons-text-1.9.json`.

- ID `GHSA-599f-7c49-w659`, alias `CVE-2022-42889`
- Summary: Arbitrary code execution in Apache Commons Text
- Published `2022-10-13T19:00:17Z`, modified `2024-02-16T08:09:06.872889Z`
- CVSS `CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:H`, GitHub review severity `CRITICAL`
- Ecosystem range for `org.apache.commons:commons-text`: introduced `1.5`, fixed `1.10.0`
- Explicit affected version list for that package: `1.5` through `1.9`

The same query for `1.10.0` and for the current latest `1.15.0` returned no vulnerabilities (`evidence/osv-api/query-org.apache.commons_commons-text-1.10.0.json` and `query-org.apache.commons_commons-text-1.15.0.json`). `1.10.0` is outside the affected range and stays on major `1`.

Maven `ComparableVersion` (Maven Artifact `3.9.16`) orders the upgrade correctly, and `String.compareTo` does not:

```text
1.9 compareTo 1.10.0 = -1; string compareTo = 8
```

`-1` means `1.9` is older than `1.10.0`. The positive string result means lexicographic order treats `1.9` as greater than `1.10.0`.

The production code calls `StringSubstitutor` with a caller-supplied map, `StringEscapeUtils.escapeHtml4`, `WordUtils.capitalizeFully`, and `CaseUtils.toCamelCase`. It does not call `StringSubstitutor.createInterpolator()`. Tests cover substitution, HTML escaping, title and route formatting, YAML config parsing, the HTTP server (200, 404, 405), and a Gson round trip.

## Negative cases

### Major upgrade only: SnakeYAML 1.33

`org.yaml:snakeyaml:1.33` is a direct `compile` dependency with an explicit version. OSV query on 2026-10-25 (`evidence/osv-api/query-org.yaml_snakeyaml-1.33.json`):

- `GHSA-mjmj-j48q-9wg2` / `CVE-2022-1471`
- Ecosystem range: introduced `0`, fixed `2.0`
- `1.33` is in the affected version list. Querying `2.0` returned no vulnerabilities.

`1.33` compareTo `2.0` is `-1`. The published fix leaves major `1`. This finding stays in the patched rescan. Manual review.

### BOM-managed, and the fix is a major bump: Guava 31.1-jre

`com.google.guava:guava` has no `<version>` child. `guava-bom` `31.1-jre` is imported in `dependencyManagement`. That import is not a `<parent>`.

OSV query for `31.1-jre` returned two advisories (`evidence/osv-api/query-com.google.guava_guava-31.1-jre.json`):

| Advisory | Alias | OSV fixed event |
| --- | --- | --- |
| `GHSA-5mg8-w23w-74h3` | `CVE-2020-8908` | `32.0.0-android` |
| `GHSA-7g45-4rm6-3mm3` | `CVE-2023-2976` | `32.0.0-android` |

Queries for `32.0.0-jre` and `32.0.1-jre` returned no vulnerabilities. Maven ordering:

```text
31.1-jre compareTo 32.0.0-android = -1
31.1-jre compareTo 32.0.0-jre = -1
32.0.0-android compareTo 32.0.0-jre = -9
```

Both published fixes are major `32`. The version is owned by the BOM import, so a pilot patch that rewrites only an explicit dependency version does not apply. Manual review.

OSV-Scanner emits this finding in a second result whose source type is `unknown`, with resolved version `31.1-jre`. The `--all-packages` output also contains `com.google.guava:guava` with an empty version and zero vulnerabilities in the `lockfile` result. The empty version is the manifest entry. The resolved version is the other result. Guava's transitive jars (`failureaccess`, `listenablefuture`, `jsr305`, `checker-qual`, `error_prone_annotations`, `j2objc-annotations`) had no advisories in this scan.

### Test scope, same-major fix: Gson 2.8.6

`com.google.code.gson:gson:2.8.6` is `test` scope. `NoticeJsonTest` round-trips a payload through `Gson.toJson` / `fromJson`.

OSV query (`evidence/osv-api/query-com.google.code.gson_gson-2.8.6.json`):

- `GHSA-4jrv-ppp4-jm57` / `CVE-2022-25647` / `SNYK-JAVA-COMGOOGLECODEGSON-1730327`
- Ecosystem range: introduced `0`, fixed `2.8.9`
- Querying `2.8.9` returned no vulnerabilities

`2.8.6` compareTo `2.8.9` is `-1`, so a same-major fix exists. The scanner still reports it, with `dependency_groups: ["test"]` and a table label of `(dev)`. Test scope is manual review. The finding remains after the Commons Text upgrade.

`org.junit.jupiter:junit-jupiter:5.12.2` is also test scope. This scan reported no advisories for it.

### Why Commons Lang is pinned

Commons Text `1.9` depends on `commons-lang3` `3.11`, and `1.10.0` depends on `3.12.0`. Both of those Lang versions are affected by `GHSA-j288-q9x7-2f5v` / `CVE-2025-48924` (introduced `3.0`, fixed `3.18.0`). Leaving Lang transitive would move `3.11` to `3.12.0` when Text is upgraded, and both versions would still carry that advisory. The root POM pins `org.apache.commons:commons-lang3:3.20.0`, which the OSV query reported as clean. Maven resolves `3.20.0` on both sides of the Text upgrade (`evidence/mvn-dependency-tree-baseline.txt`, `evidence/mvn-dependency-tree-patched.txt`). The `--all-packages` scans show `commons-lang3@3.20.0` with zero vulnerabilities before and after.

## Baseline build and scan

Command, run from the repository root on the vulnerable POM:

```bash
mvn -B -ntp verify
mvn -B -ntp org.apache.maven.plugins:maven-dependency-plugin:3.9.0:tree
osv-scanner scan source -r --format json --output-file evidence/osv-scanner-baseline.json .
osv-scanner scan source -r --format json --all-packages --output-file evidence/osv-scanner-baseline-all-packages.json .
```

`mvn verify` exit code `0`. Surefire: **Tests run: 17, Failures: 0, Errors: 0, Skipped: 0**. Log: `evidence/mvn-verify-baseline.txt`, finished `2026-10-25T16:03:17Z`. Dependency tree: `evidence/mvn-dependency-tree-baseline.txt`, finished `2026-10-25T15:59:14Z`.

OSV-Scanner exit code `1` (vulnerabilities found; the JSON file was written). Stderr (`evidence/osv-scanner-baseline.stderr.txt`):

```text
Scanning dir /workspace
Starting filesystem walk for root: /
Scanned /workspace/pom.xml file and found 6 packages
Filtered 1 local/unscannable package/s from the scan.
Warning: enricher transitivedependency/pomxml may be risky when run on untrusted artifacts. Please ensure you trust the source code and artifacts.
```

Table (`evidence/osv-scanner-baseline.table.txt`):

```text
Total 4 packages affected by 5 known vulnerabilities (1 Critical, 2 High, 1 Medium, 1 Low, 0 Unknown) from 1 ecosystem.
5 vulnerabilities can be fixed.

GHSA-4jrv-ppp4-jm57  7.7  Maven  com.google.code.gson:gson (dev)  2.8.6     fixed 2.8.9
GHSA-599f-7c49-w659  9.8  Maven  org.apache.commons:commons-text  1.9       fixed 1.10.0
GHSA-mjmj-j48q-9wg2  8.3  Maven  org.yaml:snakeyaml               1.33      fixed 2.0
GHSA-5mg8-w23w-74h3  3.3  Maven  com.google.guava:guava           31.1-jre  fixed 32.0.0-android
GHSA-7g45-4rm6-3mm3  5.5  Maven  com.google.guava:guava           31.1-jre  fixed 32.0.0-android
```

The verbatim scanner JSON is `evidence/osv-scanner-baseline.json`. Resolved packages, including those with no advisory, are in `evidence/osv-scanner-baseline-all-packages.json`.

## Patched rescan

On a scratch tree, the only POM edit was the Commons Text version element, from `1.9` to `1.10.0`. The dependency tree diff against the baseline is that coordinate plus the Maven timing lines (`diff evidence/mvn-dependency-tree-baseline.txt evidence/mvn-dependency-tree-patched.txt`). `commons-lang3` stayed `3.20.0`. Guava stayed `31.1-jre`. SnakeYAML stayed `1.33`. Gson stayed `2.8.6`.

`mvn -B -ntp verify` exit code `0`. **Tests run: 17, Failures: 0, Errors: 0, Skipped: 0**. Log: `evidence/mvn-verify-patched.txt` (finished `2026-10-25T15:59:20Z`).

The same OSV-Scanner command wrote `evidence/osv-scanner-patched.json` (exit code `1`, because the manual-review findings remain). Table (`evidence/osv-scanner-patched.table.txt`):

```text
Total 3 packages affected by 4 known vulnerabilities (0 Critical, 2 High, 1 Medium, 1 Low, 0 Unknown) from 1 ecosystem.
4 vulnerabilities can be fixed.

GHSA-4jrv-ppp4-jm57  7.7  Maven  com.google.code.gson:gson (dev)  2.8.6     fixed 2.8.9
GHSA-mjmj-j48q-9wg2  8.3  Maven  org.yaml:snakeyaml               1.33      fixed 2.0
GHSA-5mg8-w23w-74h3  3.3  Maven  com.google.guava:guava           31.1-jre  fixed 32.0.0-android
GHSA-7g45-4rm6-3mm3  5.5  Maven  com.google.guava:guava           31.1-jre  fixed 32.0.0-android
```

`--all-packages` on the patched tree shows `org.apache.commons:commons-text@1.10.0` with zero vulnerabilities (`evidence/osv-scanner-patched-all-packages.json`).

Advisory/component pairs were compared from the two JSON files as `(advisory id or alias, Maven coordinate, resolved version)`. Result in `evidence/advisory-pair-diff.txt`:

```text
Removed:
  - CVE-2022-42889 | org.apache.commons:commons-text | 1.9
  - GHSA-599f-7c49-w659 | org.apache.commons:commons-text | 1.9
Added:
  (none)
```

The same removal, and no additions, holds if the version is left out of the pair key. The POM in this branch stays on `1.9`.

That one-line upgrade was also committed on `cursor/verify-commons-text-1-10-0-68a8` at `e8d39658bc88c364ef2fbefdee33db637d87c2ef` and was not merged here. On that commit, `mvn -B -ntp verify` passed 17 tests (finished `2026-10-25T16:03:53Z`). A fresh OSV-Scanner 2.7.0 scan exited `1` and produced the same advisory/component pairs as `evidence/osv-scanner-patched.json`: `GHSA-599f-7c49-w659` and `CVE-2022-42889` for `commons-text` `1.9` were gone, and no pairs were added. Commons Text `1.10.0` did not appear in the vulnerability report.

## Branch `scenario/test-failure`

That branch adds `src/test/java/dev/ironjava/fixture/LegacyDefaultLookupsTest.java`. The test calls `StringLookupFactory.addDefaultStringLookups` and asserts that `dns`, `url`, and `script` are present. It does not invoke those lookups. Commons Text `1.10.0` drops those three keys from the default set.

On `1.9`, `mvn -B -ntp -Dtest=LegacyDefaultLookupsTest test` exited `0` (**Tests run: 1, Failures: 0**). Log: `evidence/scenario-legacy-test-on-1.9.txt`.

After changing only the Commons Text version to `1.10.0`, the same command exited `1`:

```text
[ERROR] dev.ironjava.fixture.LegacyDefaultLookupsTest.defaultLookupsIncludeDnsUrlAndScript -- Time elapsed: 0.021 s <<< FAILURE!
org.opentest4j.AssertionFailedError: expected dns in the default lookup set ==> expected: <true> but was: <false>
	at dev.ironjava.fixture.LegacyDefaultLookupsTest.defaultLookupsIncludeDnsUrlAndScript(LegacyDefaultLookupsTest.java:24)
[ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0
[INFO] BUILD FAILURE
```

Full log: `evidence/scenario-legacy-test-on-1.10.0.txt`. The scenario branch itself stays on Commons Text `1.9`, so its suite passes until that upgrade is applied.

## What this evidence does not claim

- IronJava itself was not executed against this repository. The pilot app lives in a separate repository.
- The patched POM was not merged and was not left on this branch.
- OSV records were queried and scanned on 2026-10-25. Later advisory changes would need a new scan.
- Exit code `1` from OSV-Scanner here means the JSON report contains findings. It does not mean the scanner failed to run.
- The scanner filtered one local/unscannable package. No finding was emitted for `dev.ironjava:ironjava-fixture`.
- Guava's second result uses source type `unknown`. This document records that shape and does not assign a cause beyond the BOM-managed coordinate described above.
- No integration-test profile exists. `mvn verify` runs the unit tests in this module and does not start an external service.
