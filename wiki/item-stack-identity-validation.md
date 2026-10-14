<!--
@author ArcAnc
Created at: 14.10.2026
Copyright (c) 2026
This code is licensed under "Arc's License of Common Sense".
Details can be found in the license file in the root folder of this project.
-->

# Runtime ItemStack identity: реализация и проверка

Ветка `1.21.1-multiloader`, Minecraft 1.21.1, Java 21, Fabric Loader 0.19.5 / Fabric API 0.116.17, NeoForge 21.1.216. Версия библиотеки не изменена.

## Изменённые файлы

| Файл | Изменение |
| --- | --- |
| [AnimManagerKey.java](../common/src/main/java/com/arcanc/pulselib/content/animatable/AnimManagerKey.java) | Runtime ID вместо хеша содержимого; отдельный диапазон ключей предметов |
| [PItemStackAnimationData.java](../common/src/main/java/com/arcanc/pulselib/content/animatable/PItemStackAnimationData.java) | Доступ к полю, передача/очистка и ленивое назначение |
| [PItemStackAnimationIds.java](../common/src/main/java/com/arcanc/pulselib/content/animatable/PItemStackAnimationIds.java) | Монотонный AtomicLong без ссылок на предметы |
| [ItemStackMixin.java](../common/src/main/java/com/arcanc/pulselib/content/mixin/ItemStackMixin.java) | Transient volatile long, RETURN-инъекции copy/split |
| [pulselib-common.mixins.json](../common/src/main/resources/pulselib-common.mixins.json) | Регистрация в client-наборе |
| [PItemRenderer.java](../common/src/main/java/com/arcanc/pulselib/content/renderer/PItemRenderer.java) | Проверка непустого PItemAnimatable перед рендерингом |
| [PItemStackAnimationIdsTest.java](../common/src/test/java/com/arcanc/pulselib/content/animatable/PItemStackAnimationIdsTest.java) | JUnit: монотонность, конкурентная уникальность, исчерпание |
| [ItemStackIdentityHarness.java](../tests/testmod/src/main/java/com/arcanc/pulselib/test/ItemStackIdentityHarness.java) | Все 16 regressions на настоящем transformed ItemStack и игровые проверки |
| [multiloader-testmod.gradle](../buildSrc/src/main/groovy/multiloader-testmod.gradle) | Opt-in pulseItemIdentityTest в существующей disposable инфраструктуре |
| [DisposableTestWorlds.java](../tests/testmod/src/main/java/com/arcanc/pulselib/test/DisposableTestWorlds.java) | Скрытое окно fixture; повторный вход только в собственный marked save |
| [SmokeRenderMixin.java](../tests/testmod/src/main/java/com/arcanc/pulselib/test/mixin/SmokeRenderMixin.java) | Вызов нового режима |
| [SmokeLifecycleMixin.java](../tests/testmod/src/main/java/com/arcanc/pulselib/test/mixin/SmokeLifecycleMixin.java) | Наблюдение фактического disconnect callback |
| [README.md](../tests/testmod/README.md) | Команды и критерии проверки |
| [pulselib-items.md](pulselib-items.md) | Обновлённая семантика ключей и ограничения |
| Этот отчёт | Результаты, воспроизведение и ограничения |

`SingletonAnimationManager` проверен и оставлен без изменений: lookup создаёт менеджер и контроллеры, `cleanUp()` освобождает записи, прежний таймаут 5000 ms и проверка каждые 200 ticks сохранены. Новые Java-файлы находятся в существующих пакетах с package-info.java; дублирующие package descriptions в loader-сборках не добавлены. Исходные headers сохранены, новые используют автора проекта, текущую дату и лицензию. Пользовательский `changelog.md` не редактировался этой задачей.

## Генерация и копирование

В поле стека хранится положительный ID; ноль означает отсутствие назначения. AtomicLong выдаёт значения 1…Long.MAX_VALUE и отказывает при исчерпании, не оборачивая счётчик. Счётчик не сбрасывается при выходе из мира: живой стек может пережить очистку менеджеров. Volatile обеспечивает публикацию; повторная проверка под монитором стека гарантирует одно назначение при конкурентном первом чтении/копировании. Setter также синхронизирован. Это не делает произвольную конкурентную мутацию count/components потокобезопасной.

Назначение выполняется только для непустых PItemAnimatable. EMPTY отклоняет запись даже через интерфейс. Ни генератор, ни Mixin не обращаются к animation managers, контроллерам или глобальному реестру стеков. `AnimManagerKey.of(ItemStack)` предназначен для клиентского анимированного непустого стека; пустой/неанимированный стек не имеет animation key. На dedicated server этот client Mixin не применяется. Интерфейс и генератор сами не зависят от Minecraft client classes.

Mapped sources проверены в `common/build/moddev/artifacts/vanilla-1.21.1-20240808.144430-sources.jar` и `neoforge/build/moddev/artifacts/neoforge-21.1.216-sources.jar`. В обоих случаях copy создаёт новый ItemStack из item/count/copied components и переносит popTime; copyWithCount вызывает copy и setCount; split вызывает copyWithCount(min(amount, count)), shrink и возвращает результат. Проверены доступные cached Fabric API ItemStack Mixins: переопределения этих операций не обнаружены.

Инъекция copy на RETURN получает уже созданный результат и меняет только runtime-поле. При первом копировании назначается также ID исходника. copyWithCount наследует это поведение без собственной инъекции. split на RETURN видит окончательное состояние источника: непустой источник сохраняет ID, результат получает новый; пустой источник очищает своё поле, результат сохраняет переданный copy ID. Пустой результат пропускается. Overwrite, Redirect, повторный copy, замена return value и изменения item/count/components отсутствуют.

При объединении ваниль сохраняет ID принимающего стека; состояние анимации поглощённого стека не переносится и позднее удаляется обычным таймаутом. Runtime-поле не участвует в stacking, matches, сравнении компонентов, NBT или stream codec. Отдельные копии одного источника сознательно разделяют ID и менеджер.

Item key = Long.MIN_VALUE | runtime ID. Фабрики Entity, BlockEntity и arbitrary object возвращают неотрицательные значения. Record, constructor, key accessor и long-based equality/hashCode сохранены. Коллизий предметов с другими фабричными ключами нет. Ручной `new AnimManagerKey(long)` может нарушить это разделение; прежние коллизии между nonitem hash-based ключами не устранены. Entity сохраняет 63 бита UUID XOR вместо 64; это минимальное резервирование диапазона, не новая система идентичности всех объектов.

## Результаты

Итоговый `./gradlew build --no-daemon` с Java 21: **BUILD SUCCESSFUL**, Common **123/123**, Fabric **1/1**, ошибок и failures нет. Финальный compileTestmodJava отдельно на обеих платформах: **BUILD SUCCESSFUL**. Обычная сборка не запускает Minecraft.

| Проверка | Fabric | NeoForge |
| --- | --- | --- |
| Все 16 ItemStack regressions | PASS | PASS |
| Известная общая идентичность отдельных copies | PASS | PASS |
| Независимые managers/controllers/phases | PASS | PASS |
| Temporary copy сохраняет manager и phase | PASS | PASS |
| Реальные client menu actions + server click packets | PASS | PASS |
| Hotbar, inventory/chest, обе руки, count, split/merge, drop/pickup, reopening inventory | PASS | PASS |
| Natural disconnect, пустой кэш, collectable manager/controller | PASS после callback | PASS |
| Повторный вход и новые runtime IDs десериализованных стеков | PASS | PASS |
| Нормальное завершение игрового JVM | **HANG после Stopping!** | **HANG после Stopping!** |

Независимые стеки с ID 18/19 имели разные контроллеры с time 8/28 ticks исходного idle clip; temporary copy вернула тот же manager и time. Частичный menu split сохранил ID 18 у count 4 и назначил новый ID отделённым четырём предметам. Восемь потоков проверяли первый доступ и 8000 копий общего стека; новых менеджеров не возникло. NBT и network round trips сохранили item/count/components и получили новые runtime IDs. Проверка durability вызывает также настоящий hurtAndBreak на integrated-server thread.

Все worlds созданы под `<loader>/build/pulselib-tests/<UUID>` с run/world markers. Reentry проверяет оба markers и никогда не принимает произвольный существующий save. Для vanilla InventoryScreen fixture переключён в Survival: Creative перенаправляет этот экран в каталог с другой нумерацией слотов. Reentrant loading-screen renders защищены флагом только в test harness. Fabric cleanup ожидается после наблюдаемого callback, поскольку DISCONNECT может доставляться через очередь клиентских задач. Production lifecycle hooks не изменены.

Полный успех игровых запусков **не заявляется**: после всех assertions и сохранения миров оба JVM зависли в завершении Minecraft. Повторные игровые запуски остановлены согласно ограничению задачи; ItemStack не подменялся для обхода зависания. SIGTERM не завершил процессы, пришлось завершить только собственные два тестовых JVM через SIGKILL. Это не успешный exit 0. NeoForge PASS получен до финального добавления наблюдателя/ожидания callback в fixture; production identity code в этом запуске совпадает с финальной реализацией. Повторный запуск NeoForge после изменения только fixture не выполнялся из-за зависания.

Read-only диагностика: Gradle thread dump показывает ожидание своего дочернего процесса в ProcessImpl.waitFor. Игровой jcmd Thread.print не ответил за 10 s; SIGQUIT не дал dump. Serviceability Agent отказал с `ptrace(PTRACE_ATTACH): Operation not permitted`. Причина остановки игрового JVM пока не установлена; она не доказана как конфликт ItemStack Mixins. Наблюдаемые копирование и split завершились без зависаний/рекурсии. Полный GPU resource smoke после обнаруженного shutdown hang дополнительно не запускался; существующий cleanup код не изменён. Освобождение проверяемых item manager/controller подтверждено WeakReference + GC.

Воспроизведение:

```sh
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :fabric:runClient -PpulseTestMod -PpulseItemIdentityTest --no-daemon
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :neoforge:runClient -PpulseTestMod -PpulseItemIdentityTest --no-daemon
```

Логи: `/tmp/pulselib-item-build-final.log`, `/tmp/pulselib-item-testmod-final.log`, `/tmp/pulselib-item-fabric.log`, `/tmp/pulselib-item-neoforge.log`. Диагностика: `/tmp/pulselib-item-fabric-diagnostic.log`, `/tmp/pulselib-item-neoforge-gradle-threads.log`, `/tmp/pulselib-item-neoforge-game-threads.log`, `/tmp/pulselib-item-neoforge-native-threads.log`. Disposable runs: Fabric `6f84f68b-8e03-4098-8d62-e7aed731bd86`, NeoForge `ecc1f61d-bed1-4705-a6b4-fd50f4711e1e`; сохранения оставлены на месте.

## Память, стоимость и остаточные ограничения

100000 vanilla copy операций завершились за **37.635 ms Fabric / 43.506 ms NeoForge**, прирост manager cache — **0**. Это измерение времени конкретного запуска, без сравнения с uninstrumented baseline; существенный относительный overhead не измерялся. Поле добавляет 8 bytes плюс возможное выравнивание на каждый клиентский ItemStack, включая обычные предметы. Генератор хранит один AtomicLong на сессию; не создаёт объект на каждое назначение. Copy выполняет volatile read/write и monitor вход setter; первый доступ исходника выполняет atomic increment. RETURN Inject может добавлять CallbackInfoReturnable allocation, если JIT не устраняет его. Новый AnimManagerKey по-прежнему создаётся при запросе ключа. Нулевые allocations для copy не заявляются: ваниль сама выделяет стек/components, а Mixin добавляет callback. Никаких накопленных копий, сильных ссылок на ItemStack или manager/controller registries новый механизм не хранит.

ID может жить дольше manager. После таймаута или disconnect повторный lookup того же живого Java объекта создаёт новый manager/controllers и начинает анимацию заново с прежним ID. Ресурсы, принадлежащие сохранённым пользователем ссылкам на manager/model, невозможно освобождать вопреки таким внешним ссылкам; новый механизм их не создаёт.

Без постоянной сериализуемой идентичности не решаются continuity после NBT/network reconstruction, authoritative slot replacement, save/reconnect, преобразования предмета в новый объект, а также различение temporary copy и самостоятельного logical copy. Technical mods, заменяющие copy/copyWithCount/split собственными реализациями, могут обойти инъекции или изменить наблюдаемую семантику. Совместимость проверена с указанными штатными загрузчиками/Fabric API, не с произвольными сторонними модами. copyAndClear наследует ID через copy, но не имеет отдельной инъекции очистки поля: пустой источник не рендерится; его ручное повторное использование после setCount может сохранить старую идентичность, как прочие мутации того же Java объекта. Глобальное отслеживание inventory operations и эвристики не добавлены.
