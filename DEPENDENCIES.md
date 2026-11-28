# Clocklingsin riippuvuudet ja versiot

**Tarkistettu 24.11.2026.** Versiot kuvaavat Clocklingsin (`com.finnvek.clocklings`) tämän työpuun tilannetta. Sovelluksen julkaisu- ja debug-versioiden riippuvuudet tarkistettiin Gradlelta. Versiopäivitysten lähtötiedot löytyvät Gradlen versiokatalogista ja moduulien build-tiedostoista. [Versiokatalogi](gradle/libs.versions.toml), [sovellus](app/build.gradle.kts), [benchmark](benchmark/build.gradle.kts)

## Androidin työkaluketju

| Osa | Versio |
| --- | --- |
| Gradle Wrapper | `9.9.2` |
| Android Gradle Plugin (sovellus ja testimoduuli) | `9.5.2` |
| Kotlin ja Kotlin Gradle Plugin | `2.4.21` |
| Compose-kääntäjä ja Kotlin Serialization -kääntäjäliitännäinen | `2.4.21` |
| JDK, jota CI käyttää | Eclipse Temurin `21` |
| JDK, jolla tämä luettelo tarkistettiin | Eclipse Temurin `21.0.13.1+2` |
| Java-lähdekoodi ja tavukoodin kohde | `17` |
| Android `minSdk` | API `26` |
| Android `targetSdk` | API `37` |
| Android `compileSdk` | API `37.2` |
| Android SDK Build Tools, Gradlen valitsema | `36.0.0` |

[Gradle Wrapper](gradle/wrapper/gradle-wrapper.properties), [juuriprojektin build](build.gradle.kts), [Android CI](.github/workflows/android-checks.yml)

## Sovelluksen suorat riippuvuudet

Compose BOM yhdenmukaistaa Compose-kirjastojen versiot. Kotlinin standardikirjasto ja Compose Stability Analyzerin runtime tulevat Gradle-liitännäisten kautta suoriksi riippuvuuksiksi.

| Kirjasto tai BOM | Gradlen valitsema versio |
| --- | --- |
| `androidx.activity:activity-compose` | `1.13.0` |
| `androidx.compose:compose-bom` | `2026.11.01` |
| `androidx.compose.animation:animation` | `1.13.0` |
| `androidx.compose.foundation:foundation` | `1.13.0` |
| `androidx.compose.material3:material3` | `1.5.0` |
| `androidx.compose.ui:ui` | `1.13.0` |
| `androidx.compose.ui:ui-graphics` | `1.13.0` |
| `androidx.compose.ui:ui-text` | `1.13.0` |
| `androidx.compose.ui:ui-tooling-preview` | `1.13.0` |
| `androidx.core:core-splashscreen` | `1.2.0` |
| `androidx.datastore:datastore-preferences` | `1.3.0` |
| `androidx.lifecycle:lifecycle-runtime-compose` | `2.11.1` |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | `2.11.1` |
| `androidx.lifecycle:lifecycle-viewmodel-navigation3` | `2.11.1` |
| `androidx.navigation3:navigation3-runtime` | `1.2.2` |
| `androidx.navigation3:navigation3-ui` | `1.2.2` |
| `com.android.billingclient:billing` | `9.1.0` |
| `com.github.skydoves:compose-stability-runtime` | `0.15.0` |
| `org.jetbrains.kotlin:kotlin-stdlib` | `2.4.21` |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | `1.11.0` |
| `org.jetbrains.kotlinx:kotlinx-serialization-core` | `1.13.0` |

### Debug-version lisäriippuvuudet

| Kirjasto | Versio |
| --- | --- |
| `androidx.compose.ui:ui-test-manifest` | `1.13.0` |
| `androidx.compose.ui:ui-tooling` | `1.13.0` |
| `androidx.compose.ui:ui-tooling-android` | `1.13.0` |
| `androidx.compose.ui:ui-tooling-data` | `1.13.0` |
| `androidx.compose.ui:ui-tooling-data-android` | `1.13.0` |
| `org.jacoco:org.jacoco.agent` | `0.8.16` |

### Julkaisun epäsuorat riippuvuudet

Alla ovat kirjastojen mukana tulevat moduulit ja Gradlen valitsemat versiot ennen julkaisukoontiversion optimointia. Sarakkeessa **Kyllä** tarkoittaa projektin omaa versiorajoitetta. Muut versiot tulevat kirjastojen riippuvuuksista tai BOMeista. [Versiokatalogi](gradle/libs.versions.toml), [sovelluksen versiorajoitteet](app/build.gradle.kts)

<details>
<summary>Näytä 128 epäsuoraa riippuvuutta</summary>

| Gradle-moduuli | Valittu versio | Oma versiorajoite |
| --- | --- | --- |
| `androidx.activity:activity` | `1.13.0` |  |
| `androidx.activity:activity-ktx` | `1.13.0` |  |
| `androidx.annotation:annotation` | `1.11.0` | Kyllä |
| `androidx.annotation:annotation-experimental` | `1.6.0` | Kyllä |
| `androidx.annotation:annotation-jvm` | `1.11.0` |  |
| `androidx.appcompat:appcompat-resources` | `1.8.0` | Kyllä |
| `androidx.arch.core:core-common` | `2.2.0` |  |
| `androidx.arch.core:core-runtime` | `2.2.0` |  |
| `androidx.autofill:autofill` | `1.3.0` | Kyllä |
| `androidx.collection:collection` | `1.7.0` |  |
| `androidx.collection:collection-jvm` | `1.7.0` |  |
| `androidx.collection:collection-ktx` | `1.7.0` | Kyllä |
| `androidx.compose.animation:animation-android` | `1.13.0` |  |
| `androidx.compose.animation:animation-core` | `1.13.0` |  |
| `androidx.compose.animation:animation-core-android` | `1.13.0` |  |
| `androidx.compose.foundation:foundation-android` | `1.13.0` |  |
| `androidx.compose.foundation:foundation-layout` | `1.13.0` |  |
| `androidx.compose.foundation:foundation-layout-android` | `1.13.0` |  |
| `androidx.compose.material3:material3-android` | `1.5.0` |  |
| `androidx.compose.material3:material3-ripple` | `1.5.0` |  |
| `androidx.compose.material3:material3-ripple-android` | `1.5.0` |  |
| `androidx.compose.material:material-ripple` | `1.13.0` |  |
| `androidx.compose.material:material-ripple-android` | `1.13.0` |  |
| `androidx.compose.runtime:runtime` | `1.13.0` |  |
| `androidx.compose.runtime:runtime-android` | `1.13.0` |  |
| `androidx.compose.runtime:runtime-annotation` | `1.13.0` |  |
| `androidx.compose.runtime:runtime-annotation-android` | `1.13.0` |  |
| `androidx.compose.runtime:runtime-retain` | `1.13.0` |  |
| `androidx.compose.runtime:runtime-retain-android` | `1.13.0` |  |
| `androidx.compose.runtime:runtime-saveable` | `1.13.0` |  |
| `androidx.compose.runtime:runtime-saveable-android` | `1.13.0` |  |
| `androidx.compose.ui:ui-android` | `1.13.0` |  |
| `androidx.compose.ui:ui-geometry` | `1.13.0` |  |
| `androidx.compose.ui:ui-geometry-android` | `1.13.0` |  |
| `androidx.compose.ui:ui-graphics-android` | `1.13.0` |  |
| `androidx.compose.ui:ui-text-android` | `1.13.0` |  |
| `androidx.compose.ui:ui-tooling-preview-android` | `1.13.0` |  |
| `androidx.compose.ui:ui-unit` | `1.13.0` |  |
| `androidx.compose.ui:ui-unit-android` | `1.13.0` |  |
| `androidx.compose.ui:ui-util` | `1.13.0` |  |
| `androidx.compose.ui:ui-util-android` | `1.13.0` |  |
| `androidx.concurrent:concurrent-futures` | `1.3.0` | Kyllä |
| `androidx.core:core` | `1.19.1` |  |
| `androidx.core:core-ktx` | `1.19.1` | Kyllä |
| `androidx.core:core-viewtree` | `1.0.0` |  |
| `androidx.customview:customview` | `1.2.0` | Kyllä |
| `androidx.customview:customview-poolingcontainer` | `1.1.0` | Kyllä |
| `androidx.datastore:datastore` | `1.3.0` |  |
| `androidx.datastore:datastore-android` | `1.3.0` |  |
| `androidx.datastore:datastore-core` | `1.3.0` |  |
| `androidx.datastore:datastore-core-android` | `1.3.0` |  |
| `androidx.datastore:datastore-core-okio` | `1.3.0` |  |
| `androidx.datastore:datastore-core-okio-jvm` | `1.3.0` |  |
| `androidx.datastore:datastore-preferences-android` | `1.3.0` |  |
| `androidx.datastore:datastore-preferences-core` | `1.3.0` |  |
| `androidx.datastore:datastore-preferences-core-android` | `1.3.0` |  |
| `androidx.datastore:datastore-preferences-external-protobuf` | `1.3.0` |  |
| `androidx.datastore:datastore-preferences-proto` | `1.3.0` |  |
| `androidx.emoji2:emoji2` | `1.7.0` |  |
| `androidx.fragment:fragment` | `1.9.1` | Kyllä |
| `androidx.graphics:graphics-path` | `1.1.0` |  |
| `androidx.graphics:graphics-shapes` | `1.1.0` | Kyllä |
| `androidx.graphics:graphics-shapes-android` | `1.1.0` |  |
| `androidx.interpolator:interpolator` | `1.0.0` |  |
| `androidx.lifecycle:lifecycle-common` | `2.11.1` |  |
| `androidx.lifecycle:lifecycle-common-java8` | `2.11.1` |  |
| `androidx.lifecycle:lifecycle-common-jvm` | `2.11.1` |  |
| `androidx.lifecycle:lifecycle-livedata-core` | `2.11.1` |  |
| `androidx.lifecycle:lifecycle-process` | `2.11.1` |  |
| `androidx.lifecycle:lifecycle-runtime` | `2.11.1` |  |
| `androidx.lifecycle:lifecycle-runtime-android` | `2.11.1` |  |
| `androidx.lifecycle:lifecycle-runtime-compose-android` | `2.11.1` |  |
| `androidx.lifecycle:lifecycle-runtime-ktx` | `2.11.1` |  |
| `androidx.lifecycle:lifecycle-runtime-ktx-android` | `2.11.1` |  |
| `androidx.lifecycle:lifecycle-viewmodel` | `2.11.1` |  |
| `androidx.lifecycle:lifecycle-viewmodel-android` | `2.11.1` |  |
| `androidx.lifecycle:lifecycle-viewmodel-compose-android` | `2.11.1` |  |
| `androidx.lifecycle:lifecycle-viewmodel-ktx` | `2.11.1` |  |
| `androidx.lifecycle:lifecycle-viewmodel-navigation3-android` | `2.11.1` |  |
| `androidx.lifecycle:lifecycle-viewmodel-savedstate` | `2.11.1` |  |
| `androidx.lifecycle:lifecycle-viewmodel-savedstate-android` | `2.11.1` |  |
| `androidx.loader:loader` | `1.2.0` | Kyllä |
| `androidx.navigation3:navigation3-runtime-android` | `1.2.2` |  |
| `androidx.navigation3:navigation3-ui-android` | `1.2.2` |  |
| `androidx.navigationevent:navigationevent` | `1.2.0` |  |
| `androidx.navigationevent:navigationevent-android` | `1.2.0` |  |
| `androidx.navigationevent:navigationevent-compose` | `1.2.0` | Kyllä |
| `androidx.navigationevent:navigationevent-compose-android` | `1.2.0` |  |
| `androidx.profileinstaller:profileinstaller` | `1.4.1` | Kyllä |
| `androidx.savedstate:savedstate` | `1.5.0` |  |
| `androidx.savedstate:savedstate-android` | `1.5.0` |  |
| `androidx.savedstate:savedstate-compose` | `1.5.0` |  |
| `androidx.savedstate:savedstate-compose-android` | `1.5.0` |  |
| `androidx.savedstate:savedstate-ktx` | `1.5.0` |  |
| `androidx.startup:startup-runtime` | `1.2.0` | Kyllä |
| `androidx.tracing:tracing` | `2.0.5` | Kyllä |
| `androidx.tracing:tracing-android` | `2.0.5` |  |
| `androidx.vectordrawable:vectordrawable` | `1.2.0` |  |
| `androidx.vectordrawable:vectordrawable-animated` | `1.2.0` | Kyllä |
| `androidx.versionedparcelable:versionedparcelable` | `1.2.1` | Kyllä |
| `androidx.viewpager:viewpager` | `1.1.0` | Kyllä |
| `androidx.window:window` | `1.5.1` | Kyllä |
| `androidx.window:window-core` | `1.5.1` |  |
| `androidx.window:window-core-android` | `1.5.1` |  |
| `com.github.skydoves:compose-stability-runtime-android` | `0.15.0` |  |
| `com.google.android.datatransport:transport-api` | `5.0.0` |  |
| `com.google.android.datatransport:transport-backend-cct` | `5.0.0` | Kyllä |
| `com.google.android.datatransport:transport-runtime` | `5.0.0` |  |
| `com.google.android.gms:play-services-base` | `18.11.1` | Kyllä |
| `com.google.android.gms:play-services-basement` | `18.12.1` | Kyllä |
| `com.google.android.gms:play-services-location` | `21.4.0` | Kyllä |
| `com.google.android.gms:play-services-tasks` | `18.4.1` |  |
| `com.google.firebase:firebase-encoders` | `18.0.0` |  |
| `com.google.firebase:firebase-encoders-json` | `19.0.0` |  |
| `com.google.firebase:firebase-encoders-proto` | `17.0.0` |  |
| `com.google.guava:listenablefuture` | `1.0` |  |
| `com.squareup.okio:okio` | `3.18.2` | Kyllä |
| `com.squareup.okio:okio-jvm` | `3.18.2` |  |
| `javax.inject:javax.inject` | `1` |  |
| `org.jetbrains.kotlinx:atomicfu` | `0.33.0` | Kyllä |
| `org.jetbrains.kotlinx:atomicfu-jvm` | `0.33.0` |  |
| `org.jetbrains.kotlinx:kotlinx-coroutines-bom` | `1.11.0` |  |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | `1.11.0` |  |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm` | `1.11.0` |  |
| `org.jetbrains.kotlinx:kotlinx-serialization-bom` | `1.13.0` |  |
| `org.jetbrains.kotlinx:kotlinx-serialization-core-jvm` | `1.13.0` |  |
| `org.jetbrains:annotations` | `26.1.0` | Kyllä |
| `org.jspecify:jspecify` | `1.0.1` | Kyllä |

</details>

## Testit ja suorituskykymittaus

Sovelluksen kirjastojen lisäksi Gradle-määritykset sisältävät nämä testikirjastot ja versiorajoitteet.

| Riippuvuus | Versio | Käyttö |
| --- | --- | --- |
| `junit:junit` | `4.13.2` | JVM-testit |
| `org.robolectric:robolectric` | `4.18.1` | JVM-testit |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | `1.11.0` | JVM-testit |
| `androidx.compose.ui:ui-test-junit4` | `1.13.0` | Compose-testit JVM:llä ja laitteella |
| `androidx.test.ext:junit` | `1.3.0` | JVM-testit, laitetestit ja benchmark |
| `androidx.test:runner` | `1.7.0` | JVM-testit, laitetestit ja benchmark |
| `androidx.test.espresso:espresso-core` | `3.7.0` | JVM- ja laitetestit |
| `androidx.benchmark:benchmark-macro-junit4` | `1.5.0` | Macrobenchmark |
| `com.google.guava:guava` | `33.8.0-jre` | Robolectricin riippuvuuden versiorajoite |
| `com.squareup.moshi:moshi` | `1.15.2` | Benchmarkin versiorajoite |
| `com.squareup.wire:wire-runtime` | `7.2.2` | Benchmarkin versiorajoite |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | `1.11.0` | Benchmarkin versiorajoite |

## Gradle-liitännäiset ja tarkistustyökalut

| Työkalu | Gradle-tunnus tai kirjasto | Versio |
| --- | --- | --- |
| Android Gradle Plugin | `com.android.application`, `com.android.test` | `9.5.2` |
| Kotlin Gradle Plugin | `org.jetbrains.kotlin:kotlin-gradle-plugin` | `2.4.21` |
| Compose-kääntäjäliitännäinen | `org.jetbrains.kotlin.plugin.compose` | `2.4.21` |
| Kotlin Serialization -liitännäinen | `org.jetbrains.kotlin.plugin.serialization` | `2.4.21` |
| Baseline Profile -liitännäinen | `androidx.baselineprofile` | `1.5.0` |
| JaCoCo-testikattavuus | `org.jacoco:org.jacoco.agent` | `0.8.16` |
| ktlint Gradle -liitännäinen | `org.jlleitschuh.gradle.ktlint` | `14.2.0` |
| ktlint | `com.pinterest.ktlint:ktlint-cli` | `1.8.0` |
| Compose-säännöt ktlintille | `io.nlopez.compose.rules:ktlint` | `0.6.8` |
| Detekt | `io.gitlab.arturbosch.detekt` | `1.23.8` |
| Compose-säännöt Detektille | `io.nlopez.compose.rules:detekt` | `0.4.28` |
| Androidin tietoturvan lint-säännöt | `com.android.security.lint:lint` | `1.0.4` |
| OWASP Dependency-Check | `org.owasp.dependencycheck` | `13.0.0` |
| Compose Stability Analyzer | `com.github.skydoves.compose.stability.analyzer` | `0.15.0` |
| Sonarin Gradle-liitännäinen | `org.sonarqube` | `7.6.0.18749` |

### Koonti- ja tarkistustyökalujen versiorajoitteet

| Kirjasto tai BOM | Määritetty versio | Käyttö |
| --- | --- | --- |
| `com.fasterxml.jackson:jackson-bom` | `2.23.0` | Koontityökalujen Jackson-versioiden yhdenmukaistus |
| `com.google.guava:guava` | `33.8.0-jre` | Koontityökalujen classpath |
| `org.bouncycastle:bcpkix-jdk18on` | `1.87` | Koontityökalujen classpath |
| `org.bitbucket.b_c:jose4j` | `0.9.7` | Koontityökalujen classpath |
| `org.jdom:jdom2` | `2.0.6.1` | Koontityökalujen classpath |
| `org.json:json` | `20260814` | Koontityökalujen classpath |
| `org.jsoup:jsoup` | `1.24.1` | Koontityökalujen classpath |
| `ch.qos.logback:logback-classic` | `1.6.8` | ktlint |

## CI, DeepSec ja kuvatuotanto

| Osa | Määritys tai tarkistettu versio |
| --- | --- |
| GitHub Actions `actions/checkout` | `v7.0.1` |
| GitHub Actions `actions/setup-java` | `v6.3.0` |
| GitHub Actions `android-actions/setup-android` | `v4.0.4` |
| GitHub Actions `gradle/actions/setup-gradle` | `v6.4.1` |
| DeepSec | `2.3.16`, projektikohtainen korjaustiedosto |
| pnpm, DeepSec | `12.23.0` |
| 3D-kuvien renderöintiohjelma | `5.3` |
| NumPy, renderöintiohjelman mukana | `2.3.4` |
| Pillow, kuvien optimointi | `13.0.0` |
| Cloudflare Worker, analytiikka | `compatibility_date` `2026-11-15` |

Analytiikka käyttää Androidin HTTPS-rajapintaa ja välittää viikkolaskurit Cloudflare Workerille. Worker vie kootut luvut PostHogiin. [Androidin HTTPS-toteutus](app/src/main/java/fi/clocklings/analytics/UsageUploadService.kt), [analytiikan kuvaus](docs/implementation/POSTHOG.md), [Workerin määritys](analytics-worker/wrangler.jsonc)

[CI:n tarkat commit-tunnisteet](.github/workflows/android-checks.yml), [DeepSec-paketit](.deepsec/package.json), [DeepSecin versio-ohitukset ja korjaukset](.deepsec/pnpm-workspace.yaml), [pnpm-lukitustiedosto](.deepsec/pnpm-lock.yaml), [kuvatuotanto](docs/implementation/artwork-version.md)

## Päivitä luettelo riippuvuusmuutoksen jälkeen

Tarkista ensin [versiokatalogi](gradle/libs.versions.toml). Katso Compose BOMin, Gradlen liitännäisten ja muiden kirjastojen mukana tulevat versiot Gradlen raporteista. Komennot ajetaan projektin juurikansiosta.

```powershell
.\gradlew.bat --no-daemon :app:dependencies --configuration releaseRuntimeClasspath
.\gradlew.bat --no-daemon :app:dependencies --configuration debugRuntimeClasspath
.\gradlew.bat --no-daemon :app:dependencies --configuration debugUnitTestRuntimeClasspath
.\gradlew.bat --no-daemon :app:dependencies --configuration debugAndroidTestRuntimeClasspath
.\gradlew.bat --no-daemon :benchmark:dependencies --configuration benchmarkReleaseRuntimeClasspath
.\gradlew.bat --no-daemon buildEnvironment
```

Gradlen raportissa `vanha -> uusi` kertoo, minkä version Gradle valitsi. [Dependabotin määritys](.github/dependabot.yml) tarkistaa Gradle-päivityksiä viikoittain seitsemän päivän odotusajalla.
