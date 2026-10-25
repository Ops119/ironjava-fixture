# ironjava-fixture

Deliberately vulnerable single-module Maven service used as the acceptance fixture for the IronJava pilot. The default branch stays vulnerable. Pinned advisory identifiers, versions, scan evidence, and the manual-review cases are in [FIXTURE.md](FIXTURE.md).

Build and test on JDK 21:

```bash
mvn -B -ntp verify
```

There is no Maven wrapper and no `.mvn` directory. Do not add a parent POM.
