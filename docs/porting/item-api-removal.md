# Удаление отдельного item animation interface — 28.10.2026

> Архив миграции. При очистке проекта 12.11.2026 удалены временные smoke/harness fixtures, скрипты сверок, raw evidence и API-inventory dumps. Этот документ сохраняет историю и архитектурные решения; старые команды запусков больше не являются текущими инструкциями. Удалённые файлы доступны в истории Git до очистки: `git show bf0a24f:<путь>`.

**Статус: выполнено; отдельная API-контрольная точка перед завершением depth fix.** Источник: `26.1.2-multiloader`, начальный HEAD `4195074dd9d42a7b7ae66d642b821214d7de748d`. Пользователь явно потребовал полностью удалить `PItemAnimatable`, перенести проверку `ItemStackMixin` на `PAnimatable`, затем закончить исправление глубины. Это осознанное исключение из исходного требования сохранять все публичные типы MultiLoader.

## API и реализация

- `com.arcanc.pulselib.content.animatable.PItemAnimatable` и `getPulseItemRenderer()` **удалены полностью**, без deprecated alias или shim. В production/fixture Java и актуальной items wiki не осталось обращений к ним.
- Item классы реализуют обычный `PAnimatable<T>`. `ItemStackMixin` лениво назначает transient runtime ID непустым стекам таких предметов. `AnimManagerKey.of(ItemStack)` сохраняет тип и семантику ключа, но больше не упоминает удалённый marker в exception message. `PLibHelper.getManager(ItemStack)` уже использовал `PAnimatable`, поэтому теперь selector и manager lookup согласованы.
- Сохранены copy/copyWithCount sharing, независимые IDs у отдельных стеков, частичный/full split, vanilla stacking/component equality, отсутствие ID в NBT/components/network и lazy controller creation. Регистрация `SpecialModelRenderer` и item JSON остаётся отдельной; getter больше не участвует в контракте предмета.
- Test fixture `TestBlockItem` теперь реализует **только `PAnimatable<TestBlockItem>`**. Реальные игровые identity/regression проверки тем самым подтверждают работу без прежнего item interface.
- [approved-api-changes.json](approved-api-changes.json) фиксирует единственное разрешённое удаление и replacement. Immutable baseline / class migration inventory и старые historical evidence не переписываются; упоминание удалённого API в них является исторической записью, не работающим кодом.
- `verify_manager_api.py` проверяет отсутствие разрешённого к удалению типа и сохранение полных declarations остальных **12** canonical types. Устаревший special-renderer adaptation удалён, существующий GUI helper теперь проверяется целиком с адаптацией `GuiGraphics → GuiGraphicsExtractor`, без прежнего stage-4 debt. Не создавались заглушки и не отключались проверки.

## Проверки

Full build / stage2Evidence: Common, NeoForge, Fabric — PASS. **230 JUnit: Common 229 + Fabric 1, failures/errors/skipped 0**, NeoForge NO-SOURCE. Исчезновение старого class file проверено в точных task-produced **26.1.2 / 1.1.5.3 JARs** всех трёх модулей; classfile major 69. Canonical manager/helper audit: **12 preserved types / 1 explicitly approved removal**, full contracts, baseline checksums неизменны.

**NeoForge client/server — exit 0.** Прежний `pulseStage31Probe` прошёл **16 identity cases**, включая count/damage/components, обычные items/EMPTY, lazy copy/copyWithCount, partial/full split, восемь concurrent readers, NBT/network round trips, vanilla merge и 100000 copies без создания managers. Дополнительно проверены независимые managers/controllers/phases, общий manager у copy и shutdown cleanup singleton/instance caches при сохранении живого runtime ID. Assertions fixture не менялись. Dedicated trace не содержит client class loads; source/client-specific renderer accessor больше отсутствует в общем item contract.

```bash
./gradlew :common:test --rerun :fabric:test --rerun build :neoforge:stage1SmokeClasses -PpulseStage1Smoke stage2Evidence -I /tmp/pulselib-item-api-2oeze_yt/projection.gradle -I docs/porting/tools/stage2-evidence.gradle --offline --no-daemon --console=plain
./gradlew :neoforge:runServer -PpulseStage1Smoke -PpulseStage31Probe -PpulseStage2Probe -PpulseVerificationDir=item-api-runs -I /tmp/pulselib-item-api-2oeze_yt/projection.gradle --offline --no-daemon --console=plain
./gradlew :neoforge:runClient -PpulseStage1Smoke -PpulseStage31Probe -PpulseStage2Probe -PpulseVerificationDir=item-api-runs -I /tmp/pulselib-item-api-2oeze_yt/projection.gradle --offline --no-daemon --console=plain
# После server Done, перед первым client: forceload add -12 -8 12 17 в console своего disposable world.
# После client: stop. Никаких say/chat сообщений не отправлялось.
```

Свежий named world **`item-api-smoke`**, `neoforge/build/item-api-runs/{server,client}`, loopback 127.0.0.1:25576, offline local development. Скопированы только EULA/settings/options, не saves. Final compile/JAR pass после приведения изменённого single-statement guard к AGENTS.md выполнен без принудительного повторения tests; функциональных изменений после игрового run не было. Пользовательские timestamps/license headers сохранены, новых Java packages нет. Из исходной dirty formatting правки в intended API diff принят только необходимый brace removal изменённого guard; остальные чужие formatting edits не включены.

Backup/initial patch/hashes, isolated API projection, exact artifact metadata и accepted logs: **`/tmp/pulselib-item-api-2oeze_yt`**. На старте **52 tracked edits / 3 untracked depth evidence**, из них 49 прежних пользовательских правок и незавершённый depth candidate. Compile inputs спроецированы из HEAD + только item API changes; три файла незакоммиченного depth candidate для этого checkpoint взяты из HEAD. Исходные пользовательские изменения и depth candidate сохранены в worktree. Protected `common/logs` / `fabric/runs` и source refs не менялись; push/publish не выполнялись.

**Следующее действие:** закончить depth fix и H14 (main migration journal, pending section 39). Эта API-контрольная точка не означает завершения этапов 4–5. Local commit: `refactor: remove redundant item animatable contract`; SHA в Git history/отчёте. Evidence (`docs/porting/evidence/item-api-removal.json`, архив Git).
