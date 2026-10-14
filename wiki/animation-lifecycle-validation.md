<!--
@author ArcAnc
Created at: 14.10.2026
Copyright (c) 2026
This code is licensed under "Arc's License of Common Sense".
Details can be found in the license file in the root folder of this project.
-->

# PulseLib 1.21.1 — L1/L4: реализация и проверка

Проверено 14.10.2026 на ветке `1.21.1-multiloader`, Java 21, версия `1.2.0`. Рабочая база: `768fb56`. Реализация выполнена последовательно: сначала L1 с отдельным red/green запуском, затем L4 с отдельным red/green запуском. Изменения находятся в рабочем дереве.

## L1 — Overlay Lifecycle

Active storage — `LinkedHashMap<Integer, OverlayPlayback>`, ключ — graph state index. Порядок первого включения сохраняется; restart существующей definition не меняет позицию и не создаёт новый Playback. Definitions с одинаковым trigger обрабатываются все, затем trigger потребляется. Ожидающий trigger сохраняет экземпляр даже в tick автоматического завершения или конца cancellation fade.

`OneShotOverlay` остаётся immutable record. Mutable runtime хранит скорость, интегрированное время клипа в double, отдельные fade clocks и frozen pose при паузе/отмене. Default speed берётся только при новом включении. Установка скорости не переинтерпретирует всё elapsed time; `1 → 0 → 0.5 → 1` продолжает с текущего времени. При нуле clip time и event windows стоят, а fade clocks продолжаются. Вычисленная paused поза копируется независимо от mutable/procedural sources; resume/restart освобождает frozen source.

При nonzero speed clip завершается на endpoint текущего направления и экземпляр освобождается: positive — конец, negative — ноль. Автоматический fade-out учитывает distance до этого endpoint и абсолютную скорость. Пауза уже начатого fade не останавливает fade clock; сама пауза не освобождает экземпляр, даже если вес дошёл до нуля. Явная отмена всегда освобождает его после fade-out. Отмена замораживает clip time и затухает от текущего веса. Zero fade-out удаляет сразу. Повторная отмена не перезапускает timer. Restart отменяет старое завершение, сбрасывает clip/fade clocks и сохраняет runtime speed. `reset()` освобождает все active overlays и их pose caches.

| API `PAnimationGraphRuntime` | Поведение |
| --- | --- |
| `overlaySpeed(String)` / `overlaySpeed(int)` | скорость активной definition |
| `setOverlaySpeed(String, float)` / `setOverlaySpeed(int, float)` | finite signed speed; ноль — pause |
| `restartOverlay(String)` / `restartOverlay(int)` | restart активной definition, `false` при inactive |
| `stopOverlay(String)` / `stopOverlay(int)` | отмена с fade-out, `false` при inactive |

Имя означает graph state name. Unknown, non-overlay или ambiguous state — `IllegalArgumentException`; при одинаковых именах используйте index. Get/set inactive speed — `IllegalStateException`. NaN/Infinity speed отвергаются в definition и runtime API; negative speed поддерживает reverse Overlay. Missing clip проверяется до activation; ошибка содержит state и clip name, невидимый бессмертный экземпляр не создаётся. Доступ через существующий `controller.graphRuntime()`; граф и controller пересоздавать не нужно.

Покрыты обычное воспроизведение/fades, zero-speed activation, 100 000 paused ticks, 10 000 triggers с count=1, identity экземпляра, пауза на 65%, resume и speed changes, разные definitions/shared trigger/ambiguous names, оба fades при retrigger, retrigger в tick завершения, paused cancellation, cancellation из fade-in, independent fade interpolation, reset, missing clip, invalid speeds и stationary event windows. Реальная dispatch-проверка паузы/возобновления также входит в graph suite.

## L4 — Pose-Snapshot RESTART

Граф захватывает snapshot в `beginTransition`, после продвижения tick и перед заменой transition, при `partialTick = 1`. `sampleBasePose` вычисляет текущий результат живых base layers, state blend spaces и действующих blend weights. Controller учитывает текущие times/context и preceding controllers для Molang this-values. Кеш последнего рендера не используется. `PPoseMixer.Transition.transitionTo` захватывает `mixer.mix(reference, layers())` непосредственно при запросе.

`PPoseSnapshot` глубоко копирует все TRS vectors/quaternions, valid/dirty flags, presence translation/rotation/scale, scalar values/presence и visibility. Его private storage не выдаётся наружу: sample/layers получают копии. Snapshot локален runtime/transition, не хранит clip/model/resource references, не продвигает время и не отправляет события.

Общий `PPoseMixer.blend` смешивает рассчитанный source с живым target: translation/scale — linear interpolation, rotation — существующий shortest-path quaternion slerp. При alpha=0 возвращается независимая копия source; при alpha=1 — target. Missing channels сохраняют присутствие/отсутствие и используют reference для интерполяции; явно заданный identity остаётся присутствующим. Старый mutable getter API сохраняет вклад nonidentity значений; explicit identity следует объявлять writer setters.

Independent overlays исключены из base capture, их instance/time/speed/cancellation сохраняются и применяются после base один раз. Это проверено для движущегося, paused и нескольких overlays, включая translation/rotation/scale.

RESTART заменяет base sources одним frozen source. Повторное прерывание вычисляет актуальный результат и заменяет старую копию. После completion/reset graph transition удалён; mixer очищает source list. Проверены 3000 последовательных прерываний в каждой реализации: один stored source, отсутствие роста outgoing playback list, сохранение позы и освобождение после окончания. FROM_CURRENT сохраняет живые weighted sources; живой target старого snapshot transition продолжает sampling при новом FROM_CURRENT. COMPLETE_CURRENT сохраняет свой gate. Zero duration переключает сразу без snapshot.

Новые additive sampling API: `sampleBasePose`, `samplePose`, `layers(model, partialTick)`, `tickPrecise(model, delta, snapshotSampler)`, `PPoseMixer.blend`, `PPoseMixer.Transition(mixer, reference, initial)`, `PPose.copy()` и metadata accessors. `PAnimationController.setGraphSnapshotContexts(provider)` и overload `PAnimationRuntime.captureGraphBase(..., contexts)` дают актуальные game queries в момент capture.

## Совместимость и границы

Сохранены исходные constructors `OneShotOverlay`, `PAnimationGraph`, `PPoseMixer.Transition(initial)`, record shapes `Layer`/`EventTrack`/`PreciseEventTrack`, прежние tick/layers/controller signatures. Обычные FROM_CURRENT/COMPLETE_CURRENT остаются живыми layer transitions, не переключены автоматически на frozen blending. Double event precision/C1 regressions сохранены. Gecko/glTF parsers, версия, publication, CI, MultiLoader и H14/H17 lifecycle/render fixes не изменены. Изначальные удаления `docs/reviews` оставлены как были.

Custom consumer, самостоятельно вычисляющий позу только через `layers()`, должен использовать `samplePose()` для frozen sources: snapshot не является clip и не может быть представлен прежним record слоёв. Встроенный runtime/resolver уже адаптирован.

Для mixer с внешней reference pose или parent hierarchy используйте новый explicit constructor: прежний constructor использует initial layer reference, иначе identity, и mixer без hierarchy. Дополнительные game queries, обычно заполняемые custom renderer-ом, необходимо передать через `setGraphSnapshotContexts`/snapshot sampler из текущего game state; без такого provider controller сохраняет ранее переданные custom frame queries, обновляя playback time/anim_time. Создавать отдельный runtime не требуется.

Root-motion extraction не изменён; проверены численные translation/quaternion delta и local/model matrices при нескольких controllers. Smoke сравнивает реальные рассчитанные transforms на загруженной модели, а не interruption screenshots. Multiplayer, in-world transition pixels и arbitrary renderer-specific queries автоматически не проверялись. Smoke остался opt-in и не включён в CI.

## Результаты

Исходные witnesses: L1 — оба теста провалились (дубликаты и shared trigger); L4 — все три провалились (mixer без предыдущего transition, interruption на 40%, graph interruption). После исправлений проходят.

| Проверка | Результат |
| --- | --- |
| `POverlayLifecycleTest` | 17/17 PASS |
| `PPoseMixerTransitionTest` | 13/13 PASS |
| `PGraphRestartTest` | 18/18 PASS |
| `POverlayReverseTest` | 14/14 PASS |
| `PCyclicAnimationEventsTest` | 25/25 PASS |
| отдельный запуск новых suites + C1 | 87/87 PASS |
| `./gradlew build --no-daemon` (Java 21) | BUILD SUCCESSFUL; Common 120/120, Fabric 1/1, no failures/errors/skips |
| Fabric client, glTF | PASS, exit 0; L1/L4 + CLIENT markers |
| Fabric client, Gecko | PASS, exit 0; L1/L4 + CLIENT markers |
| NeoForge client, glTF | PASS, exit 0; L1/L4 + CLIENT markers |
| NeoForge client, Gecko | PASS, exit 0; L1/L4 + CLIENT markers |

Во всех игровых запусках требуются и получены `PULSE TEST L1/L4 PASS`, `PULSE TEST CLIENT PASS`, successful process exit. Исходная entity-модель: 11 bones, `idle`/`walk`. В каждом smoke проверены forward/reverse/paused overlays, 40% interruption, 1000 retriggers с identity/count assertions, paused cancellation/fade-out и reset. Общий graphics/explicit cleanup smoke также прошёл. Исходные четыре запуска выполнены после исправления raw-controller Molang compatibility; повторная проверка reverse описана ниже.

Команды проверки:

```sh
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :common:test --tests '*POverlayLifecycleTest' --tests '*POverlayReverseTest' --tests '*PPoseMixerTransitionTest' --tests '*PGraphRestartTest' --tests '*PCyclicAnimationEventsTest' --no-daemon
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew build --no-daemon
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :fabric:runClient -PpulseTestMod -PpulseSmokeAuto --no-daemon
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :fabric:runClient -PpulseTestMod -PpulseSmokeAuto -PpulseGeckoDemo --no-daemon
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :neoforge:runClient -PpulseTestMod -PpulseSmokeAuto --no-daemon
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :neoforge:runClient -PpulseTestMod -PpulseSmokeAuto -PpulseGeckoDemo --no-daemon
```

Логи текущей сессии: `/tmp/pulselib-l1-before.log`, `/tmp/pulselib-l4-before.log`, `/tmp/pulselib-l1-l4-final-tests.log`, `/tmp/pulselib-build-final.log`, `/tmp/pulselib-{fabric,neoforge}-{gltf,gecko}-final.log`, `/tmp/pulselib-final-smoke-results.jsonl`. JUnit HTML/XML находятся в `common/build/reports/tests/test`, `common/build/test-results/test`, аналогично Fabric. В build есть существующее предупреждение `ThreadDeath`; smoke также выдаёт штатные shader sampler warnings.

## Дополнение — reverse Overlay

По дополнительному запросу автора поддержана отрицательная скорость `OneShotOverlay`. Default negative speed начинает с конца; отрицательная runtime speed разворачивает воспроизведение с текущего курсора. Скорость интегрируется в clip time, который ограничен `[0, clip.length]`. Reverse завершается на нуле. Zero сохраняет позу, cursor и последнее ненулевое направление; restart/retrigger paused reverse удерживает конечный кадр до resume.

Direction change отменяет автоматический fade старого endpoint и восстанавливает вес от текущего значения за `fadeOutDuration`, затем действует fade нового endpoint. Recovery/fades идут по wall time и продолжаются при pause. Manual stop сменой направления не отменяется. Runtime instance identity и applied speed сохраняются при retrigger, включая tick завершения. Reverse использует существующий one-shot event dispatch: descending timestamps, departure excluded, arrival included; pause/resume не повторяет pivot event, повторное пересечение после разворота отправляет событие снова.

Новые regressions `POverlayReverseTest`: 14/14 PASS. Они проверяют actual end-frame TRS, смену направления и partial ticks, clamp/release на нуле, 100 000 paused ticks, 10 000 retriggers с count/identity assertions, last direction при paused restart, fades, cancellation, smooth weight recovery, signed/invalid speed values, descending event dispatch с boundary/pivot markers, shared trigger разных reverse definitions и zero-length clips. Graph RESTART numeric regression дополнительно выполняется с reverse Overlay. Исходные два reverse witnesses провалились на запрете negative speed до правки. Остальные graph state types сохранили прежние speed contracts.

Итоговые проверки reverse: новые reverse/L1/L4 suites и C1 — 87/87 PASS. `./gradlew build --no-daemon` — BUILD SUCCESSFUL, Common 120/120, Fabric 1/1, no failures/errors/skips. Повторные opt-in Fabric/glTF, Fabric/Gecko, NeoForge/glTF и NeoForge/Gecko — все PASS с `forward/reverse/paused overlay`, `PULSE TEST L1/L4 PASS`, `PULSE TEST CLIENT PASS`, normal exit 0. Проверка нормального forward и paused поведения осталась в каждом запуске.

Логи reverse: `/tmp/pulselib-reverse-before.log`, `/tmp/pulselib-reverse-tests.log`, `/tmp/pulselib-reverse-build.log`, `/tmp/pulselib-{fabric,neoforge}-{gltf,gecko}-reverse.log`, `/tmp/pulselib-reverse-smoke-results.jsonl`.

## Изменённые файлы

- [PAnimationController.java](../common/src/main/java/com/arcanc/pulselib/content/animatable/PAnimationController.java) — делегирование capture, поставщик актуального Molang context.
- [PAnimationGraphRuntime.java](../common/src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationGraphRuntime.java) — L1 storage/API/clocks; L4 sampling и frozen base source.
- [PAnimationState.java](../common/src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationState.java) — валидация Overlay speed/fade durations, прежние constructors.
- [PPose.java](../common/src/main/java/com/arcanc/pulselib/content/model/animation/PPose.java) — deep copy, TRS/channel/scalar/visibility metadata.
- [PPoseSnapshot.java](../common/src/main/java/com/arcanc/pulselib/content/model/animation/PPoseSnapshot.java) — внутренний неизменяемый источник с независимыми копиями.
- [PPoseMixer.java](../common/src/main/java/com/arcanc/pulselib/content/model/animation/PPoseMixer.java) — snapshot transition и resolved-pose blend.
- [PAnimationRuntime.java](../common/src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationRuntime.java) — общий sampling, base/overlay separation, несколько controllers.
- [PAnimationPoseResolver.java](../common/src/main/java/com/arcanc/pulselib/content/model/animation/PAnimationPoseResolver.java) — visibility и active bones из рассчитанной позы.
- [POverlayReverseTest.java](../common/src/test/java/com/arcanc/pulselib/content/model/animation/POverlayReverseTest.java) — 14 reverse regressions.
- [POverlayLifecycleTest.java](../common/src/test/java/com/arcanc/pulselib/content/model/animation/POverlayLifecycleTest.java) — 17 regressions L1.
- [PPoseMixerTransitionTest.java](../common/src/test/java/com/arcanc/pulselib/content/model/animation/PPoseMixerTransitionTest.java) — 13 regressions mixer.
- [PGraphRestartTest.java](../common/src/test/java/com/arcanc/pulselib/content/model/animation/PGraphRestartTest.java) — 18 regressions graph/совместимость.
- [PTransitionTestSupport.java](../common/src/test/java/com/arcanc/pulselib/content/model/animation/PTransitionTestSupport.java) — численные TRS fixtures/oracle.
- [AnimationLifecycleHarness.java](../tests/testmod/src/main/java/com/arcanc/pulselib/test/AnimationLifecycleHarness.java) — smoke на исходной модели idle/walk.
- [SmokeHarness.java](../tests/testmod/src/main/java/com/arcanc/pulselib/test/SmokeHarness.java) — вызов L1/L4 fixture в opt-in client smoke.
- [README.md](../tests/testmod/README.md) — команды smoke и границы проверки.
- [animatables-and-controllers.md](../wiki/animatables-and-controllers.md) — runtime API и семантика capture.
- [api-reference.md](../wiki/api-reference.md) — новые additive API.
- [animation-lifecycle-validation.md](../wiki/animation-lifecycle-validation.md) — этот отчёт.

## Критерии готовности

- [x] L1: нулевая скорость, pause/resume и удержание до ручной отмены.
- [x] L1: уникальность definition, restart существующего instance, независимые overlays.
- [x] Reverse: signed speed, запуск с конца, pause/resume, смена направления, restart и release на нуле.
- [x] Reverse: независимые fades, descending events, совместимость RESTART и обеих моделей/платформ.
- [x] L1: cancellation при нуле, independent fades, корректные event windows/dispatch.
- [x] L4: фактическая base pose, fixed deep snapshot, непрерывность и нужные каналы.
- [x] L4: overlays сохраняются и применяются один раз.
- [x] L4: snapshots/sources заменяются и освобождаются; тысячи RESTART не накапливаются.
- [x] L4: FROM_CURRENT/COMPLETE_CURRENT, cycle sync, events, root motion и multiple controllers проверены.
- [x] L4: mixer/graph согласованы; unit и Fabric/NeoForge smoke на Gecko/glTF пройдены.
