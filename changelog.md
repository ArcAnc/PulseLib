# PulseLib 1.1.5.3 — Minecraft 26.1.2

## Changes

* Ported PulseLib to Minecraft 26.1.2 with shared Common code and separate Fabric and NeoForge integrations.
* Adapted model, animation, attachment, armor, and player-animation APIs to the native render-state extraction and submission pipeline.
* Added Fabric integration for plugin discovery, model and atlas reloads, world rendering, GUI models, and living attachments.
* Updated animated items to use native special-model renderers and added isolated GUI rendering through `PLibHelper.renderModelInGui(...)`.
* Separated ordinary alpha blending from OIT submission policy and corrected depth compositing with vanilla geometry and weather.
* Corrected animation timing, Molang evaluation, cyclic event dispatch, and Gecko rotation and interpolation handling.
* Improved resource reload, disconnect, and GPU resource cleanup.
* Fixed incorrect third-person hand rotation and first-person armor and attachment submission order.
* Enabled Maven, Modrinth, and CurseForge publishing for both Fabric and NeoForge.
* Updated the wiki for the MultiLoader API and Minecraft 26.1.2 integration.

## API migration

* Register PulseLib content through `@PulsePlugin`, `IPulseCommonPlugin`, and `IPulseClientPlugin`. Scoped registration callbacks replace the old NeoForge `PulseLibEvents` API.
* Use `PAnimatable<T>` for animated items; `PItemAnimatable` has been removed. Item-stack animation identities remain client-local and do not alter stacking or serialized item data.
* Renderer integrations now use native render states, `SubmitNodeCollector`, `Identifier`, `RenderPipeline`, and `GuiGraphicsExtractor`. Older BEWLR and `ShaderInstance` integrations require adaptation.
* Removed obsolete `RuntimeLoader.TYPE` and `type()` aliases. Native atlas integration uses `RuntimeLoader.CODEC` and `codec()`.

## Requirements

* Minecraft 26.1.2 and Java 25.
* Install the PulseLib artifact matching your loader: Fabric or NeoForge.
* Fabric installations also require Fabric API.
