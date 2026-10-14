## 1.2.0 — MultiLoader (beta)

Полный список изменений относительно ветки `1.21.1`, составлен 14.10.2026 по итоговому diff исходников, ресурсов, сборки и документации.

| Параметр | Исходная ветка | Текущая ветка |
| --- | --- | --- |
| Ветка | `1.21.1` | `1.21.1-multiloader` |
| Коммит сравнения | `e364f608e2694106f6c14dbe6df24ecf9008b37c` | `05803231617a2f6ac76f77ef37f256e03dcec835` |
| Версия PulseLib | `1.1.5.3` | `1.2.0`, beta |
| Minecraft / Java | `1.21.1` / Java 21 | `1.21.1` / Java 21 |
| Загрузчики | NeoForge | NeoForge и Fabric |

Текущая ветка содержит 10 коммитов поверх исходной. На момент сравнения рабочее дерево было чистым. Переносы файлов и форматирование учтены отдельно от изменений поведения. Этот changelog описывает подготовленную beta; его обновление не публикует релиз или Maven-артефакты.

### MultiLoader и интеграция с загрузчиками

- Монолитный NeoForge-проект разделён на `common`, `neoforge` и `fabric`. Общие анимации, модели, рендереры, деформеры, вложения и ресурсы находятся в Common; интеграция с загрузчиками — в соответствующих модулях.
- Добавлена поддержка Fabric с отдельными main/client initializers, `fabric.mod.json`, Access Widener и адаптерами клиентского lifecycle.
- NeoForge сохраняет собственный bootstrap, события, Access Transformer и поддержку `IClientItemExtensions`.
- Общий код получает платформенные операции через `IPulsePlatform`, клиентское управление камерой через `IPulseCameraService`, а поиск плагинов через `IPulsePluginFinder`. Провайдеры выбираются через `ServiceLoader`; отсутствие или несколько провайдеров диагностируются при инициализации.
- Адаптированы регистрация шейдеров, texture atlas и runtime sprite source, загрузка моделей, установка render layers, item rendering, анимационные ticks и удаление entity managers.
- Fabric получает точки flush очереди после block entities, particles и weather, соответствующие стадиям NeoForge. Существующие first-person и GUI пути сохранены.
- Общие и платформенные Mixins разделены на `pulselib-common.mixins.json`, `pulselib-neoforge.mixins.json` и `pulselib-fabric.mixins.json`. Общие обращения к NeoForge-only методам заменены сервисами и Accessor Mixins.
- Для Fabric требуется Fabric API. Обязательная зависимость от Architectury API не добавлена.

### Автоматические плагины и регистрация расширений

- Добавлены `@PulsePlugin`, `IPulseCommonPlugin` и `IPulseClientPlugin`. Один API используется на обоих загрузчиках.
- Плагины обнаруживаются автоматически: NeoForge использует annotation scan загрузчика, Fabric — сканирование bytecode корней установленных модов. Поддерживаются JAR, каталоги разработки и корни ZIP filesystem; в Fabric учтены раздельные compiled/resources roots.
- Поиск выполняется один раз. Physical side фильтруется до загрузки класса: CLIENT-плагины не инициализируются на dedicated server. Поддержаны `CLIENT` по умолчанию, `SERVER` и `BOTH` с проверкой соответствующего интерфейса.
- Проверяются public concrete class, public constructor без аргументов, аннотация и уникальный plugin ID. Повторно найденные классы объединяются; порядок callbacks определяется отсортированным `ResourceLocation` ID.
- Добавлены отдельные callbacks `registerAnimationChannels`, `registerAnimationEvents`, `registerDeformers`, `registerAttachments`, `registerPlayerAnimations`, `registerAnimatedAttachments` и `registerModels`.
- Добавлены `PTypeRegistrar`, `PAttachmentRegistrar`, `PPlayerAnimationRegistrar`, `PAnimatedAttachmentRegistrar`, `PModelRegistrar` и общий lifecycle регистратора. Использование регистратора после завершения callback диагностируется.
- Типы каналов, событий и деформеров регистрируются на соответствующих физических сторонах через общий API. Клиентский контент регистрируется отдельными фазами.
- Одноразовые фазы выполняются в установленном порядке и не повторяются при resource reload. Plugin instances сохраняются между перезагрузками; только model declarations собираются повторно.
- Объявления типов, player animations и моделей проходят проверку перед применением batch конкретного плагина. Конфликты loader ID, texture reference и registry ID получают явную диагностику.
- `PRegistry` расширен методами `entries()`, `validateAll(...)` и `registerAll(...)`; snapshot содержимого возвращается как неизменяемая map.
- Ошибка внешнего плагина логируется с классом, модом и фазой. Ошибка одноразового callback отключает последующие callbacks этого плагина; ошибка model declarations допускает повторную попытку при reload. Здоровые плагины продолжают обработку.
- Внутренние ошибки PulseLib прерывают инициализацию. Fatal discovery сохраняет причину, не публикует частичный список плагинов и не запускает повторное сканирование. Failed одноразовая фаза блокирует дальнейшее выполнение и повтор callbacks; успешно применённые предыдущие batches не откатываются и не повторяются. Фаза моделей может восстановиться после ошибки reload.
- Повторная регистрация одной и той же living attachment definition или animated attachment renderer больше не добавляет дубликат.

Подробности и примеры: [плагины и миграция](wiki/plugins.md).

### Несовместимые изменения API и миграция с 1.1.x

Source и binary compatibility со всем API `1.1.x` не гарантируются. Основные удаления и замены:

| Старый API | Новый способ |
| --- | --- |
| `PulseLibEvents.TypeRegistrationEvent` | Три соответствующих callbacks `IPulseCommonPlugin` с `PTypeRegistrar` |
| `PulseLibEvents.RegisterResourceEvent`, включая вложенный `ModelRegistration` | `IPulseClientPlugin.registerModels(PModelRegistrar)` и `PModelRegistrar.ModelRegistration` |
| `PulseLibEvents.AttachmentRegistrationEvent` и вложенный registrar | `registerAttachments(PAttachmentRegistrar)` |
| `PulseLibEvents.PlayerAnimationRegistrationEvent` и вложенный registrar | `registerPlayerAnimations(PPlayerAnimationRegistrar)` |
| `PulseLibEvents.PlayerAnimatedAttachmentRegistrationEvent` и вложенный registrar | `registerAnimatedAttachments(PAnimatedAttachmentRegistrar)` |
| `PItemAnimatable.registerClientExtension()` | `getPulseItemRenderer()`; загрузчик устанавливает renderer автоматически |
| `PLibRegistration.init(IEventBus)` | `PLibRegistration.init()`; обычные игровые объекты регистрируются через API загрузчика |
| `PLibRegistration.BlockReg`, `BETypeReg`, `ItemReg`, `EntityTypeReg` | Игровые регистрации перенесены в внешний test mod |
| `PResourceCache.postEvent()` и `register(IEventBus)` | Model declarations через plugin callback; atlas подключается адаптером загрузчика |
| `PAttachmentAnchorResolvers.init(IEventBus)` | `init()` |
| `PLibArmorHandler.register(IEventBus)` | Общая операция `addLayers(renderer)`, вызываемая адаптером загрузчика |
| `PRenderTypes.register(IEventBus)` | `ShadersProvider.registerShaders(ShaderRegistrar)` через адаптер загрузчика |
| `PRenderStagesHandler.register(IEventBus)` | `afterBlockEntities()`, `afterParticles()`, `afterWeather(destination)` |
| `PLibAnimationTicker.register(IEventBus)` | `clientTick(ClientLevel)` и `afterClientTick()` через адаптер загрузчика |
| `removeUnused(ClientTickEvent.Post)` у animation managers и `InstanceAnimationManager.removeEntityManager(EntityLeaveLevelEvent)` | `removeUnused()` и `removeManager(entity)`, вызываемые loader callbacks |

Сам `PulseLibEvents` удалён. Расширения объявляются через `@PulsePlugin`; отдельный PulseLib entrypoint в `fabric.mod.json` и ручной вызов plugin manager не требуются. Обычные Minecraft blocks/items/entities по-прежнему используют lifecycle своего загрузчика.

### Items, armor и attachments

- `PItemAnimatable.getPulseItemRenderer()` возвращает renderer через общий Minecraft API. Реализация по умолчанию возвращает `null`, позволяя не задавать custom renderer. Для custom item rendering используется JSON с `"parent": "builtin/entity"`.
- Добавлен общий `PArmorModelProvider` с `getPulseArmorModel(...)` и `setupPulseArmorAnimations(...)` для humanoid armor models на обоих загрузчиках.
- Fabric устанавливает armor model, применяет анимации/деформацию и скрывает vanilla armor через свой Mixin. NeoForge интегрирует общий provider в `PArmorClientExtensions`, сохраняя платформенные custom/generic models.
- Living и humanoid attachment layers, animated player attachment layer устанавливаются адаптерами обоих загрузчиков.
- Примеры item renderer creation вынесены в отдельные client factories, чтобы shared item classes могли загружаться на dedicated server.

### L1 — управляемый Overlay lifecycle

- Активные Overlay хранятся в `LinkedHashMap<Integer, OverlayPlayback>` по graph state index. Для одной definition существует максимум один активный экземпляр; порядок первого включения сохраняется при restart.
- Повторный trigger перезапускает существующий playback, сбрасывает clip/fade clocks и отменяет прежнее завершение. Runtime-настройка скорости и identity экземпляра сохраняются, включая retrigger в tick завершения.
- Разные definitions могут работать одновременно, в том числе с одинаковым trigger: сначала обрабатываются все совпавшие definitions, затем trigger потребляется.
- `OneShotOverlay` остаётся immutable definition. Скорость из definition используется при activation; изменяемые скорость, позиция, fade clocks и состояние отмены принадлежат runtime-экземпляру.
- Скорость интегрируется в clip time. Изменение `1 → 0 → 0.5 → 1` сохраняет текущую позицию; весь прошедший интервал не пересчитывается по новой скорости.
- При `speed == 0` сохраняются время и независимая копия вычисленной Overlay-позы. Overlay продолжает участвовать в смешивании и остаётся активным до resume/restart/явной отмены. Pause на 65% возобновляется с 65%.
- Время fade-in/fade-out отделено от времени клипа. Пауза playback не останавливает появление или исчезновение. Уже начатый natural fade может дойти до нулевого веса во время паузы, сохраняя активный экземпляр.
- Ручная отмена замораживает clip time и уменьшает текущий вес за `fadeOutDuration`; при нулевой длительности экземпляр удаляется сразу. Cancel работает при паузе; повторный cancel не перезапускает timer.
- Trigger/restart во время fade-in или fade-out использует тот же экземпляр и начинает новое проигрывание. `reset()` очищает все overlays и их frozen pose caches.
- Обычное nonzero воспроизведение завершается на endpoint своего направления и освобождает runtime state. Каждый tick использует существующий playback.
- Missing clip проверяется до activation; fail-fast сообщение содержит state name и clip name. Невидимый бессрочный Overlay не создаётся.
- `NaN` и бесконечные скорости отвергаются. Fade durations должны быть конечными и неотрицательными.

Добавлен публичный API `PAnimationGraphRuntime`:

| Метод, с overload по `String stateName` и `int stateIndex` | Результат |
| --- | --- |
| `overlaySpeed(...)` | Текущая скорость активного экземпляра |
| `setOverlaySpeed(..., float speed)` | Изменение скорости, pause, resume или смена направления |
| `restartOverlay(...)` | Restart активного экземпляра; `false`, если definition неактивна |
| `stopOverlay(...)` | Cancel с fade-out; `false`, если definition неактивна |

Строка обозначает имя graph state. Неизвестный, неоднозначный или non-overlay state вызывает `IllegalArgumentException`; неоднозначность обходится overload по index. Get/set скорости неактивного Overlay вызывает `IllegalStateException`. Методы доступны через существующий `controller.graphRuntime()` без пересоздания controller, графа или definition.

### Reverse Overlay

- `OneShotOverlay.speed` и runtime speed теперь допускают отрицательные конечные значения. Абсолютное значение задаёт скорость, знак — направление; ноль означает pause.
- Activation/restart с отрицательной скоростью начинает с конца клипа. Playback идёт к нулю, ограничивается диапазоном `[0, clip.length]` и освобождается после завершения.
- Смена знака через `setOverlaySpeed(...)` разворачивает анимацию с текущей позиции. Переход через нулевую скорость сохраняет последнее ненулевое направление; restart paused reverse удерживает конечный кадр до resume.
- Overlay, впервые объявленный со скоростью ноль, удерживает начальный кадр.
- Natural fade-out учитывает расстояние до endpoint и абсолютную скорость. При развороте старый automatic fade отменяется, вес восстанавливается от текущего значения за `fadeOutDuration`, после чего действует fade нового endpoint. Recovery продолжает работать при pause.
- Смена направления не отменяет ручной cancel; trigger/restart отменяет его как часть нового проигрывания.
- Reverse events отправляются по убыванию времени: departure исключается, arrival включается. Pause/resume не повторяет pivot event; новое пересечение marker после разворота отправляет его снова.
- Изменение speed contract относится к Overlay. Контракты скорости остальных graph state types сохранены.

### L4 — RESTART от вычисленной позы

- `PPoseMixer.Transition` и `PAnimationGraphRuntime` реализуют одинаковую семантику RESTART: фиксируют фактический результат смешивания базовых источников и используют неподвижную копию как source нового перехода.
- Исправлен переход без предыдущего active transition: source берётся из текущей вычисленной позы, вместо очистки source layers и скачка к reference/bind pose.
- В `WALK → RUN` на 40% последующий RESTART в ATTACK начинает от вычисленной смеси 60% WALK / 40% RUN. Прежние клипы не продолжают анимировать snapshot.
- Mixer захватывает `mixer.mix(reference, layers())` непосредственно при `transitionTo(...)`. Graph захватывает `sampleBasePose(...)` в `beginTransition(...)` после продвижения tick, при `partialTick = 1`, до замены transition. Кеш предыдущего render frame не используется.
- Добавлен внутренний `PPoseSnapshot` с глубокой копией translation, quaternion rotation, scale, valid/dirty flags, channel presence, scalar values/presence и visibility. При sampling выдаются копии; storage не разделяется с mutable source poses.
- Добавлен общий `PPoseMixer.blend(reference, source, target, alpha)`: translation/scale интерполируются линейно, rotation использует shortest-path quaternion slerp. Endpoints `alpha = 0` и `alpha = 1` возвращают соответственно source и target с их каналами.
- Missing channels сохраняют свою семантику и используют reference при интерполяции. Явно анимированный identity channel отличается от отсутствующего; dirty flag сам по себе не превращает отсутствующие TRS channels в identity.
- `PPose` расширен `copy()`, `scalarChannelCount()`, `hasScalarChannel()`, `hasTranslation()`, `hasRotation()`, `hasScale()`, `hasVisibility()`, `isVisible()` и setter `visibility(...)`. Копирование позы сохраняет scalar storage и metadata.
- Graph base sampling отделён от independent overlays. Snapshot не включает Overlay; их time, speed, pause/cancel и runtime identity сохраняются, а contribution применяется один раз поверх нового base transition.
- `PAnimationRuntime` и `PAnimationPoseResolver` используют рассчитанную pose для blending, visibility и active bones, включая кости, представленные только frozen source. Сохранены последовательность нескольких controllers и Molang this-values от preceding controllers.
- Snapshot локален конкретному runtime/transition, не содержит clip/model references, не двигает собственное время и не генерирует animation events.
- Повторный RESTART вычисляет актуальную смесь, заменяет предыдущий snapshot и оставляет один frozen source. Завершение transition/reset освобождает его; старые playback/source lists не растут.
- `FROM_CURRENT` сохраняет живые weighted sources, включая живой target прежнего snapshot transition; `COMPLETE_CURRENT` сохраняет блокировку прерывания. Snapshot применяется только при RESTART. Zero blend duration сразу переключает на target.

Дополнения API: `sampleBasePose(...)`, `samplePose(...)`, `layers(model, partialTick)`, `tickPrecise(...)` с snapshot sampler, `PAnimationRuntime.captureGraphBase(...)`, `PPoseMixer.blend(...)` и `PPoseMixer.Transition(mixer, reference, initial)`.

`PAnimationController.setGraphSnapshotContexts(provider)` позволяет заполнять актуальные game queries при capture. Custom renderer queries должны поступать из текущего игрового состояния через этот provider или snapshot sampler; без provider сохраняются ранее переданные custom queries с обновлением playback time/anim_time.

Существующие constructors `OneShotOverlay`, `PAnimationGraph` и `PPoseMixer.Transition(initial)`, прежние tick/layers/controller signatures и формы прежних `Layer`/`EventTrack` сохранены. Для внешней reference pose, hierarchy и mesh-space/additive capture предусмотрен explicit constructor mixer transition. Custom consumers, вычисляющие pose только из старого `layers()`, должны перейти на `samplePose()` для RESTART: frozen pose не представляется animation clip.

### C1 — события циклических анимаций

- Общий `PCyclicAnimationEvents` заменяет обход границ через float cursor на integer cycle indices и double time. FMA сохраняет local residual при больших абсолютных временах.
- Raw controller и graph dispatch используют общий обход, поддерживающий forward/reverse, переход через несколько циклов за tick, дробную длину клипа и отрицательные времена.
- Маркеры на `0` и `length` сохраняются как разные события и отправляются один раз в порядке воспроизведения при seam crossing. Вход в raw reverse stage отдельно исключает исходный end marker.
- Stationary windows, zero-length clips и отсутствие событий не запускают обход. Неконечные значения, непредставимые cycle ranges и dispatch более `1 << 20` событий получают диагностику до callbacks, без молчаливого обрезания потока.
- Добавлены `PAnimation.forEachCyclicEvent(double, double, Consumer)` и overload с `includeReverseStart`, а также graph `tickPrecise(...)` / `PreciseEventTrack` с double event boundaries. Прежний float `tick(...)` / `EventTrack` сохранён.

### H14 — прозрачность в Fabulous и состояние OpenGL

- Исправлено исчезновение/неверное перекрытие OIT meshes при композиции Fabulous: слои PulseLib сохраняются до завершения vanilla transparency chain и затем композируются в итоговый target.
- Новый compositor учитывает color/depth terrain translucency, item entities, particles, weather и clouds; вклады vanilla surfaces перед OIT глубиной сохраняются.
- Добавлены `oit_fabulous_composite` и `oit_fabulous_metadata` shaders и metadata texture. Схлопнутая OIT depth не записывается в vanilla translucent target; metadata texture освобождается при cleanup/пересоздании.
- Добавлены `PRenderQueue.compositeFabulousTranslucency(...)` и общий `LevelRendererTransparencyMixin`. Существующие четыре OIT layers и обычный compositor сохранены.
- NeoForge-only `GlStateBackup` заменён общим `GlStateSnapshot`. Сохраняются и восстанавливаются framebuffer/viewport, indexed blend factors/equations, depth/scissor state, shader program, VAO, buffers и texture bindings с учётом кешей Minecraft и исключений при composite.
- Добавлен fallback `ARB_instanced_arrays` для attribute divisors, когда отсутствует OpenGL 3.3 core; отсутствие обеих возможностей диагностируется при initialization.
- Закрываются временные `ByteBufferBuilder` при baking моделей, CPU/GPU-deformed geometry и texture variants, включая exceptional paths.

### Resource reload и runtime cleanup

- Model declarations собираются перед reload через общий `ResourceReloadMixin` / `PulseRuntime.beforeResourceReload()`, до texture resolution и последующего model baking.
- `PResourceCache` публикует staged declarations после успешной фазы. Конфликты текстур и loaders проверяются до применения соответствующего batch.
- `PModelCache` собирает модели из параллельных load tasks в `ConcurrentHashMap`; добавлены сообщения о loader/source и времени reload/baking.
- Gecko loader, уже существовавший в исходной ветке, включён в built-in model loader registry наряду с glTF. Парсеры Gecko/glTF сохранены.
- Ошибка регистрации PulseLib shaders в NeoForge теперь прерывает initialization с диагностикой вместо прежнего warning и продолжения с отсутствующим shader.
- Операция `PulseRuntime.disconnected()` централизует очистку render queue, GPU/deformer resources, deformation caches и instance/singleton/player animation managers на render thread.
- NeoForge вызывает общую очистку при client level unload; Fabric — при disconnect и смене `ClientLevel`. Существующая очистка NeoForge перенесена в общий код.
- Block entity removal вызывает клиентский animation manager только при наличии client level; server-side removal больше не обращается к этому клиентскому пути.
- Удаление entity managers и очистка неиспользуемых managers выполняются loader callbacks, без NeoForge annotations в Common.

### M-P2 — визуальный Root Motion и иерархия attachments

- `PPlayerAnimationFrame.modelMatrix(...)` теперь вычисляет точное произведение parent matrices. Добавлены `bindMatrix(...)`, `currentRootRelativeMatrix(...)` и `currentRootRelativeTransform(...)`.
- Вложенные anchors и meshes наследуют все промежуточные translation/rotation/scale. Nonuniform scale и поворот сохраняются в matrix pipeline вместо потери affine составляющей при промежуточном TRS compose.
- Third-person attachment stack уже содержит visual ROOT; child contribution задаётся относительно текущего ROOT. Повторное применение root motion устранено.
- `rootRelativeTransform(...)` сохраняет представление относительно ROOT bind frame для consumers, которым нужен полный visual motion. Rootless rigs используют HEAD bind reference.
- First-person items, arms, custom anchors и automatic meshes используют общую camera-relative VIEW presentation. ROOT и camera transforms не добавляются повторно.
- Automatic mesh discovery учитывает inherited animation и static mesh descendants активного rig, включая сценарий анимации только камеры.
- Добавлены matrix helpers в `PPlayerAnimationSpace`, общий `visualRootMatrix(...)` с pivot и сохранение caller channel overrides поверх точной исходной affine matrix.
- Существующий root-motion extraction API сохранён. Документация уточняет визуальный контракт: извлечение delta и применение ROOT к pose не перемещают entity, hitbox или server position; физическое движение выполняет вызывающий мод.

### M-P3 — animated camera

- Camera displacement вычисляется в фактической bind camera point, с учётом ROOT, pivot и всей parent hierarchy. Использование translation affine delta в нулевой точке исправлено.
- Rotation-only channel камеры с ненулевым bind origin больше не создаёт ложное перемещение. Root rotation перемещает camera origin по соответствующей дуге.
- Учитываются rotated/scaled intermediate parents, rotated bind reference и rootless rigs; camera и first-person attachments используют согласованные transform spaces.
- Исправлено преобразование Gecko/glTF source axes перед применением camera delta. Влияние first-person activation weight при переходах сохранено.
- Добавлен `PFirstPersonCameraSpace.fromMinecraftAngles(...)`; Fabric и NeoForge используют согласованную yaw/pitch/roll convention через camera service, включая signed roll.
- Режим VANILLA camera сохраняет native положение/вращение. Animated camera остаётся визуальной и не изменяет физику игрока.

### Внешний demo/test mod

- Исходные TestBlock, TestBlockEntity/Renderer, TestEntity/Render, TestBlockItem/Renderer, TestArmor, TestTailItem, PTestArmor, TestDayTimeColor, ball toss и acrobatic player demos перенесены в `tests/testmod` с общими реализациями и небольшими loader adapters.
- Demo classes и assets исключены из release library JARs. Исходные ресурсные ZIP с demos удалены; модели, анимации и текстуры распакованы в test mod, добавлены vanilla item/blockstate descriptors.
- Demo content регистрируется в namespace `pulselibtest`. V запускает ball toss, B — acrobatics; доступны block/entity/item/armor/tail examples и выбор Gecko entity вместо glTF.
- `CommonTestPlugin` и `ClientTestPlugin` проверяют регистрацию через `@PulsePlugin`; добавлены source-set запуск, установка test JAR и Fabric `remapTestmodJar` для intermediary mappings.
- Smoke проверяет original models, actual mesh rendering, GPU deformation, solid/OIT pixels, состояние GL и удаление buffers. Добавлены explicit cleanup и natural disconnect probes; отрицательные controls различают исправный lifecycle hook и ручную очистку.
- Автоматические игровые режимы используют новый маркированный run directory и disposable world. Существующие или немаркированные saves не принимаются в качестве тестовой сцены; обычные manual demo runs сохраняют свой режим работы.
- Добавлен H14 harness с 38 pixel/state regression cases, реальной vanilla transparency chain, resource reload, resize и world/weather probes.
- Добавлены opt-in GPU timestamp profiling для Fancy/Fabulous, offscreen 1080p/1440p/4K и отдельные world measurements; отчёты включают mean/p95, pass timing, fullscreen calls и allocations.
- Добавлен player motion/camera harness на Gecko и glTF с blend modes/weights, ROOT masks, несколькими definitions, reload, observer actors и integrated-server respawn.
- Добавлен L1/L4 smoke на исходной entity rig с 11 bones и clips `idle`/`walk`: проверяет pose continuity при 40% interruption, forward/reverse/paused overlays, 1000 retriggers, instance identity/count, cancel/fade-out и reset.

Команды и границы проверок: [test mod README](tests/testmod/README.md). Minecraft smoke, GPU profiling и world tests остаются opt-in и не входят в обязательный CI.

### Сборка, зависимости и публикация

- Gradle переведён на multi-project layout с convention plugins `multiloader-common`, `multiloader-loader`, `multiloader-publishing` и `multiloader-testmod` в `buildSrc`.
- Loader builds компилируют общие sources и включают shared resources в собственные JARs. Common служит development module и не публикуется как отдельный installable mod/Maven artifact.
- Сохраняются Minecraft 1.21.1, Java 21, Parchment `2024.11.17`, NeoForge `21.1.216` и ModDevGradle `2.0.140`. Добавлены NeoForm `1.21.1-20240808.144430`, Fabric Loader `0.19.5`, Fabric API `0.116.17+1.21.1` и Loom `1.12.7`.
- Версии runtime-библиотек jgltf и Jackson сохранены и заданы явно; они встраиваются через NeoForge JarJar и Fabric include. Common использует compile-only JSR-305, Sponge Mixin и MixinExtras; добавлены JUnit Jupiter `5.11.4` и тестовое использование ASM. Minotaur закреплён на `2.10.0` вместо диапазона `2.+`.
- Heap Gradle увеличен с 1 GiB до 3 GiB; configuration cache отключён. Gradle wrapper сохранён.
- Общая команда `./gradlew build --no-daemon` собирает все три модуля и запускает JUnit. Loader client/server runs вызываются как `:<loader>:runClient` / `:<loader>:runServer`.
- Старые root NeoForge `data` / `gameTestServer` run configurations и подключение root `src/generated/resources` удалены при переносе сборки. Новый opt-in testmod workflow описан отдельно.
- Game JARs получают loader suffix: `pulselib-1.21.1-neoforge-1.2.0.jar` и `pulselib-1.21.1-fabric-1.2.0.jar`; предусмотрены sources/Javadoc artifacts. Fabric game JAR проходит remap.
- NeoForge Maven artifactId `pulselib-neoforge` сохранён; добавлен `pulselib-fabric`. Координаты beta: `com.arcanc.pulselib:pulselib-neoforge:1.21.1-1.2.0` и `com.arcanc.pulselib:pulselib-fabric:1.21.1-1.2.0`.
- Maven publication и workflow публикуют оба loader modules. Modrinth/CurseForge получают отдельные loader variants, beta release type и Fabric API dependency для Fabric.
- `publishMods` объединяет upload tasks обоих загрузчиков; `publishAll` также включает Maven. Отдельный прежний root `publishDiscord` task удалён. Обычный build не запускает публикацию.
- CI сведён к одному build/test job на Java 21, добавлены manual dispatch, upload обоих loader JARs и JUnit reports; Checkout обновлён до v6. Игровые запуски в CI не добавлены.

### Документация и оформление исходников

- Добавлены [plugin API и migration guide](wiki/plugins.md), [отчёт L1/L4/reverse](wiki/animation-lifecycle-validation.md); обновлены [installation](wiki/installation.md), [API reference](wiki/api-reference.md) и [controllers](wiki/animatables-and-controllers.md).
- Wiki examples моделей, ресурсов, items, armor, attachments, player animations и extension types переведены с `PulseLibEvents` на plugin callbacks. Source links обновлены на Common paths текущей ветки.
- Уточнены LOCAL/MODEL/ROOT-relative/VIEW spaces, anchor stack contract, visual Root Motion и camera displacement; обновлены README и testmod instructions.
- Перенесённые Java sources приведены к исходному стилю: TAB indentation, Allman braces, развёрнутые blocks/методы и цепочки с точкой в конце предыдущей строки.
- Сохранены авторство и существующие Created dates; добавлены отсутствующие license headers, package nonnull defaults и уточнения nullable metadata.
- Добавлены `AGENTS.md`, Java `.editorconfig` и ignore rules для runtime logs/crash diagnostics. Удалены промежуточные review documents; актуальные инструкции находятся в wiki и testmod README.

### Regression tests и доступные результаты проверки

Добавлены следующие JUnit suites. Числа ниже подтверждены последними доступными XML reports, полученными 14.10.2026 при проверке реализации до составления этого changelog; новая документационная правка не сопровождалась повторным запуском Minecraft или Gradle.

| Suite | Тестов | Основное покрытие |
| --- | ---: | --- |
| `PulsePluginClassScannerTest` | 3 | Bytecode/side filtering, JAR/directory/ZIP roots, invalid side |
| `PulsePluginLifecycleTest` | 3 | Порядок фаз, одноразовое выполнение, failed state и model retry |
| `PulsePluginManagerTest` | 11 | Dedicated server, deduplication/IDs, failures, atomic batches, reload |
| `FabricDevelopmentRootsTest` | 1 | Раздельные development roots и исключение чужих classpath groups |
| `PCyclicAnimationEventsTest` | 25 | Forward/reverse seams, много циклов, большие времена, guards |
| `POverlayLifecycleTest` | 17 | Lifecycle/API, pause/resume, fades, shared triggers, reset, validation |
| `POverlayReverseTest` | 14 | End-frame pose, reverse cursor/events, direction changes, fades/cancel |
| `PPoseMixerTransitionTest` | 13 | Численные TRS/masks/scalars, snapshot continuity и interruption policies |
| `PGraphRestartTest` | 18 | Graph blending, overlays, contexts/controllers, cycles/events/root motion |
| `PPlayerHierarchyTest` | 8 | Parent matrices, ROOT/pivot, nonuniform scale, first-person spaces |
| `PPlayerCameraTest` | 8 | Camera bind point/hierarchy, source axes и yaw/pitch/roll |
| **Всего** | **121** | **Common: 120; Fabric: 1; failures/errors/skips: 0** |

L1/reverse stress regressions включают 100 000 paused ticks и 10 000 retriggers с одним экземпляром. L4 включает по 3000 последовательных RESTART для mixer и graph, сравнение рассчитанных translation/rotation/scale, missing/identity channels и освобождение source state. Имена/время клипов не используются как единственное доказательство корректного blending.

Последний полный `./gradlew build --no-daemon` на Java 21 завершился `BUILD SUCCESSFUL`; отдельный запуск L1/L4/reverse и C1 suites — 87/87 PASS. NeoForge использует общие regression suites; отдельной NeoForge JUnit suite в текущем наборе нет.

Последние opt-in L1/L4/reverse client smoke: Fabric/glTF, Fabric/Gecko, NeoForge/glTF и NeoForge/Gecko — все с `PULSE TEST L1/L4 PASS`, `PULSE TEST CLIENT PASS` и exit 0. Они проверяют рассчитанные poses на загруженных моделях, graphics и explicit cleanup. Результаты и команды приведены в [отчёте проверки](wiki/animation-lifecycle-validation.md).

Эти последние четыре запуска не проверяют in-world interruption pixels или сетевой multiplayer. H14 pixel/profiling, natural disconnect и player motion world checks представлены отдельными opt-in harnesses; их результаты не следует выводить из успешного L1/L4 smoke. Совместимость с произвольными shader mods и custom renderer queries требует соответствующей конфигурации и проверки.

### Что уже было в исходной ветке

Animation Graph, blend spaces, Pose Mixer, Molang, Gecko/glTF parsers, root-motion extraction, player/first-person animations, anchors/mesh attachments, GPU deformers, instanced queue и четырёхслойный OIT существовали в `1.21.1`. В этой версии они перенесены на оба загрузчика и исправлены в перечисленных выше местах.

Исправление third-person wrong hand rotation из прежнего changelog входит в исходную ветку до точки сравнения и не является новым изменением `1.2.0`.

Схема версий PulseLib сохранена: `1.1.x` — небольшие обновления и исправления, `1.2.x` — крупные изменения, включая MultiLoader и архитектуру; `2.0.x` зарезервирована для следующего этапа, определяемого автором. Несовместимость API сама по себе не определяет переход на `2.0`.
