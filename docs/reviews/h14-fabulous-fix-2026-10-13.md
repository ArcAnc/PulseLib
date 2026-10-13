# H14 — Fabulous OIT: подтверждение и исправление

Дата: 2026-10-13. Ветка `1.21.1-multiloader`, исходный HEAD `87052baf1c7dc281aea3d6714eafa9ac26545110`. Minecraft 1.21.1, PulseLib 1.2.0. Результат — локальный diff поверх этого HEAD и ранее разрешённых изменений H17/testmod; коммит и публикация не выполнялись.

**H14: CONFIRMED BUG — исправлен и проверен на Fabric и NeoForge в описанных сценах.** Это уточняет прежний POTENTIAL BUG / NOT REPRODUCED в [перепроверке аудита](1.21.1-multiloader-recheck-2026-10-13.md). H13/H15/H16 и остальные замечания не исправлялись в этой задаче.

## Первопричина и исходное воспроизведение

Старый afterWeather composite выбирал weather target при Fabulous, записывал цвет с выключенными depth test/depth writes. Поэтому `gl_FragDepth` существующего `oit_composite.fsh` не попадал в depth attachment. В контролируемой сцене weather depth оставался **1.0**, хотя OIT-поверхность находилась на **0.4**, перед main opaque depth **0.5**.

Проверены исходники и ресурсы именно Minecraft 1.21.1 из локальных Gradle artifacts: `LevelRenderer.renderLevel`, `PostChain`, `PostPass`, `assets/minecraft/shaders/post/transparency.json`, `shaders/program/transparency.fsh`. Vanilla сортирует main и пять прозрачных targets по depth от дальних к ближним. Main участвует с alpha **1**. Цвет weather с depth 1 оказывается позади main с depth 0.5 и полностью перекрывается. На небе main depth также 1: стабильный порядок вставки позволяет weather остаться видимым. Это объясняет оба симптома пользователя.

Первый GPU-прогон выполнен **до изменения production OIT**: настоящий PulseLib mesh проходил через queue/compiler/executor и настоящую vanilla `transparency.json`. Вход: красная поверхность alpha 128/255, depth 0.4; слева opaque RGB (0.2,0.2,0.2), depth 0.5; справа sky RGB (0.1,0.25,0.5), depth 1.

| Измерение финального framebuffer | Исходная версия | После исправления |
| --- | --- | --- |
| Fancy, поверхность перед opaque фоном | (0.600,0.098,0.098) | (0.600,0.098,0.098) |
| Fabulous, та же поверхность перед фоном | **(0.200,0.200,0.200)** — OIT потерян | **(0.600,0.098,0.098)** |
| Fabulous, поверхность на фоне неба | (0.549,0.125,0.247) | (0.549,0.125,0.247) |
| Weather до vanilla composite, исходная версия | RGB (0.502,0,0), alpha 0.502, **depth 1.0** | OIT больше не записывает weather |

[Исходный Fancy](evidence/h14/fancy-before.png), [исходный Fabulous](evidence/h14/fabulous-before.png), [исправленный Fabulous](evidence/h14/fabric-initial-front-fabulous.png). Исходный Fabulous показывает красную поверхность справа, но теряет её слева. [Измерения и PASS-маркеры](evidence/h14/checks.log) включают этот первоначальный прогон, а не только имитацию старого routing на исправленном коде.

## Путь одного меша и targets

`PEntityRenderer.submitBone` получает RenderType через `material.resolveRenderType`. `PRenderQueue.submitEntityMesh` регистрирует geometry/pipeline/instance и отправляет command в `PFrameCompiler`. После группировки `GlDrawExecutor.execute` отделяет OIT-группы и выполняет depth/accumulation passes. Parsers, material resolution, batching, instancing и GPU deformers не менялись. Для изоляции H14 тест создаёт fixture непосредственно как `PGeometryData`.

| Этап | Fast/Fancy | Fabulous | Depth / blend |
| --- | --- | --- | --- |
| Opaque terrain | Main color + depth | Main color + depth | Vanilla states |
| Entity/block-entity queue | Существующие stage hooks и flush | Те же hooks; часть native geometry находится в itemEntity/particles targets | Opaque dispatch не изменён |
| OIT depth, слои 0…3 | Собственный FBO с main depth attachment; nearest depth хранится как **цвет** R32F | То же, depth source — main | Test on, writes off; GL_MIN nearest depth |
| OIT accumulation | RGBA16F accumulation + R16F сумма `-log(1-alpha)` | То же | Test on, writes off; additive MRT |
| Terrain translucency / particles | Main | Translucent/particles получают копию main depth и рисуются в свои targets | Native renderer states |
| Clouds / rain / border | Main | Clouds / weather | Native renderer states |
| Старый afterWeather OIT | Main | Weather — ошибочный путь H14 | Test off, writes off; alpha blend |
| Новый afterWeather | Прежний main composite | Откладывает OIT, native targets не изменяет | Fancy RGB-поведение сохранено |
| Vanilla transparency chain | Отсутствует | Main + translucent + itemEntity + particles + weather + clouds → final → main | Premultiplied color/depth sorting |
| Новый Fabulous OIT | Отсутствует | После **всей** vanilla chain, в main; читает пять native color/depth пар и текущий OIT-слой | Test off, writes off; RGB SRC_ALPHA / ONE_MINUS_SRC_ALPHA, alpha ZERO / ONE |

Порядок проверен по исходникам обеих платформ. Fabric первоначально вызывает hooks после vanilla calls, NeoForge — через RenderLevelStageEvent. Новый [общий Mixin:32–43](../../common/src/main/java/com/arcanc/pulselib/content/mixin/LevelRendererTransparencyMixin.java#L32) оборачивает `PostChain.process`, сначала выполняет оригинал и композитит только при identity `chain == transparencyChain`. Entity outline chain не изменяется; ordinal не используется. Эта точка реально вызвана из мирового рендера на обоих загрузчиках.

## Композиция отдельных глубин

Одна depth-запись в weather не представляет четыре OIT-слоя, между которыми могут находиться вода, частицы, дождь и облака. Исправление сохраняет отдельные OIT depths и не включает GL_ALWAYS/depth writes.

Слои вставляются от дальнего к ближнему. Для текущего OIT depth shader выбирает native layers **перед** ним и сортирует как vanilla. Их premultiplied contribution обозначим `F`, transmission — `T`, остальную сцену — `B`. Уже разрешённый цвет `C = F + T*B`. Для OIT average color `c` и alpha `a` shader выдаёт RGB `F + T*c`, alpha `a`. Обычный RGB alpha blend даёт:

```
C' = (1-a)*C + a*(F + T*c)
   = F + T*((1-a)*B + a*c)
```

Native передний слой сохраняется, OIT добавляется за ним, задняя сцена ослабляется ровно один раз. Следующий OIT-слой вставляется аналогично. Native opaque particle с alpha 1 даёт T=0 и скрывает OIT за ним, даже если particle depth отсутствует в main. Фрагменты за main opaque geometry уже отсекаются исходными OIT passes. Оба пути occlusion покрыты regressions.

Первый nearest depth вычисляется GL_MIN; следующий depth pass отбрасывает фрагменты до предыдущего nearest + 1e-6. Accumulation оставляет прежнюю окрестность layer depth, weighted average color и revealage. Эти shaders, tolerances, четыре слоя и существующая аппроксимация coplanar fragments сохранены. Новый compositor использует depth **каждого** слоя.

Shader использует 12 samplers — предел texture cache `GlStateManager` Minecraft 1.21.1 независимо от GPU limit. Revealage и R32F depth слоя предварительно пакуются в переиспользуемую RG32F texture: два OIT samplers + пять native color/depth пар. Depth остаётся 32-bit float. Цена — одна 8-byte/pixel texture (около 15.8 MiB при 1920×1080), только для Fabulous, и до четырёх дополнительных fullscreen packing passes. Texture удаляется при cleanup/resize. GPU timing отдельно не измерялся.

Реализация: [GlWeightedBlendedOit:149–275](../../common/src/main/java/com/arcanc/pulselib/content/renderer/legacy/GlWeightedBlendedOit.java#L149), [Fabulous shader](../../common/src/main/resources/assets/pulselib/shaders/core/oit_fabulous_composite.fsh), [metadata shader](../../common/src/main/resources/assets/pulselib/shaders/core/oit_fabulous_metadata.fsh). Queue/executor передают targets в общий backend.

## GL state и исключения

Существующий framebuffer/viewport/depth/scissor/indexed blend snapshot расширен program, VAO, array/element buffers, active texture и затронутыми 2D bindings. [GlStateSnapshot:36–92](../../common/src/main/java/com/arcanc/pulselib/content/renderer/legacy/GlStateSnapshot.java#L36) восстанавливает actual GL и Minecraft texture cache. ShaderInstance.clear инвалидирует program cache перед raw восстановлением program. BufferUploader cache инвалидируется перед собственной fullscreen geometry и после восстановления чужого VAO. RenderSystem shader reference не изменяется.

Собственный FBO меняет только свои draw buffers/attachments; rebind исходного FBO возвращает его selectors. Units 0…11 и исходный active unit/binding восстанавливаются. Для units за пределами cache используется raw GL без обращения к отсутствующему элементу массива. Тест также использует unit 15, разные read/draw bindings, нестандартный viewport, чужие VAO/VBO/EBO, GREATER depth function, depthMask=true, scissor, отдельные indexed blend factors/equations attachment 1.

Вложенные try/finally очищают shaders, восстанавливают framebuffer/state/bindings и завершают OIT frame. Exception regression передаёт null native target после начала composite и сравнивает состояние при выходе. Main/native depth attachments сравниваются до/после. Depth writes остаются false в metadata и финальной композиции. PASS определяется сравнением состояния/пикселей, а не одним отсутствием GL error.

## Игровые проверки и пределы

Opt-in Gradle runClient с testmod: Fabric Loader 0.19.5 + Fabric API, NeoForge 21.1.216, Java 21.0.11, NVIDIA GTX 1660 Ti, OpenGL 4.6 / driver 615.71.09. На **каждой** платформе выполнены:

* **38 GPU cases**: 18 сцен + exception restoration, до и после reload/resize. Front/behind, небо, пересечение, четыре depth layers, lit/emissive, все пять native targets перед/за OIT, interleaved layers и opaque particle. Final RGB сравнивается с независимой CPU-сортировкой слоёв; без native layers — также с прежним Fancy compositor. RGB tolerance 0.025 учитывает UNORM/R16F округления.
* **15 проверок/снимков мира**: control/front/behind в Fancy/Fabulous, затем в дождь; Fabulous rain control/front/behind повторены после reload/resize. Реальный stone wall, фиксированная камера, alpha 128/255, настоящий common LevelRenderer hook. Дождь сравнивается с same-frame reference, поскольку полосы перемещаются между кадрами.
* Одинаковые ясные Fancy/Fabulous pixels: stone control (0.373,0.373,0.373), передняя красная поверхность (0.686,0.184,0.184), задняя поверхность над stone (0.373,0.373,0.373); синяя поверхность вне stone видна на небе (0.353,0.408,1.000).
* Активны настоящий дождь и облака. Native weather/cloud/particle fixtures дополнительно делают overlap детерминированным, независимо от случайного попадания дождевой полосы в sample.

[Fabric перед stone](evidence/h14/fabric-world-fabulous-front.png), [Fabric за stone](evidence/h14/fabric-world-fabulous-behind.png), [NeoForge перед stone](evidence/h14/neoforge-world-fabulous-front.png), [дождь](evidence/h14/neoforge-world-rain-fabulous-front.png), [после reload/resize](evidence/h14/neoforge-world-reloaded-resized-rain-fabulous-front.png).

Команды и markers — в [tests/testmod/README.md](../../tests/testmod/README.md#h14-fabulous-oit-regression). Regression mode делает **одну** ограниченную resource reload и resize, загружает один мир. Обычный testmod сохраняет ранее запрошенное отсутствие постоянных перезагрузок/переключений мира. CI остаётся `./gradlew build --no-daemon`.

Стандартная команда `./gradlew build --no-daemon` выполнена после исправления: **BUILD SUCCESSFUL in 20s**. Ни игровые задачи, ни зависимости testmod не включены в основной build/CI. Версия и настройки публикации не изменены.

Не проверялись другие GPU/драйверы, shader mods и произвольные замены vanilla chain. Particle fixtures используют настоящий particles target/vanilla chain, но не исчерпывают все ParticleEngine render types. Длительное GPU profiling не проводилось. Native targets по-прежнему представляют геометрию одним color/depth pixel; исправление сохраняет vanilla контракт и не восстанавливает потерянные внутри native target fragments.

## Диагностический запуск IDE

Первый запуск IDE оказался без testmod и ошибочно открыл Fabric save `Test`. Он остановлен; agent logpoint удалён, пользовательские breakpoints сохранены. В log есть unknown testmod registry entries / skipped entities, поэтому сохранность объектов testmod в этом save **не подтверждена**. Копии level/playerdata перед остановкой лежат в `/tmp/pulselib-h14/ide-save-backup`; они уже после начала ошибочного запуска и **не являются гарантированной исходной резервной копией**. Непроверенные данные мира попыткой восстановления не перезаписывались.

Все последующие мировые прогоны явно направлены в одноразовые `pulselib-smoke` копии NeoForge test save с подключённым testmod. IDE logpoint не дал показаний и не является доказательством исправления. Диагноз основан на exact-version vanilla sources, baseline depth/color measurements и независимых final-pixel regressions.
