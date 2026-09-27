# PulseLib Entities

Entities are the most natural fit for PulseLib: each entity already has its own lifetime, position, rotation, and tick state. The entity implements [`PAnimatable`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/animatable/PAnimatable.java), and [`PEntityRenderer`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/renderer/PEntityRenderer.java) renders the animated model.

Use entity controllers to translate gameplay state into animation state. Do not put rendering decisions into the AI goals or renderer when a controller can express the same rule cleanly.

## Entity class

This example chooses attack, walk, or idle from normal entity state. The animation names must exist in the exported model.

```java
public class RobotEntity extends PathfinderMob implements PAnimatable<RobotEntity> {
    private static final PRawAnimation WALK = PRawAnimation.begin().thenLoop("walk").build();
    private static final PRawAnimation IDLE = PRawAnimation.begin().thenLoop("idle").build();
    private static final PRawAnimation ATTACK = PRawAnimation.begin().thenPlay("attack").build();

    private final PAnimationManager<RobotEntity> manager = PLibHelper.createManager(this);

    public RobotEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @Override
    public PAnimationManager<RobotEntity> getAnimationManager(AnimManagerKey key) {
        return this.manager;
    }

    @Override
    public void registerAnimationControllers(PAnimationManager.PAnimationRegistrar<RobotEntity> registrar) {
        registrar.add("main", () -> state -> {
            RobotEntity entity = state.animatable();
            if (entity.swinging) {
                state.controller().play(ATTACK);
            } else {
                state.controller().play(entity.walkAnimation.isMoving() ? WALK : IDLE);
            }
            return ControllerState.PLAY;
        });
    }
}
```

## Renderer

The renderer is mostly declarative: model data plus render type. Living entity rotation and scaling are handled by `PEntityRenderer`.

```java
public class RobotRenderer extends PEntityRenderer<RobotEntity> {
    public RobotRenderer(EntityRendererProvider.Context context) {
        super(context,
                PModelData.entity(
                        ResourceLocation.fromNamespaceAndPath("examplemod", "robot")),
                PRenderTypes.RenderTypeProvider::trianglesSolid);
    }
}
```

Register it:

```java
@SubscribeEvent
public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
    event.registerEntityRenderer(MyEntities.ROBOT.get(), RobotRenderer::new);
}
```

## LivingEntity behavior

For `LivingEntity`, `PEntityRenderer` follows several vanilla transforms so custom models behave like normal mobs:

* Body rotation is applied from vanilla interpolation.
* Death, sleeping, upside-down, and auto spin transforms are copied from vanilla-style behavior.
* Entity scale is applied.
* If the model has a bone named `head`, head yaw and pitch are applied to it.

## Layers

Use [`PEntityRenderLayer`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/renderer/PEntityRenderLayer.java) for extra model parts attached to entity bones: armor plates, backpacks, equipment, or conditional body parts. See [Entity Render Layers](entity-render-layers.md).

Classes used:

* [`PEntityRenderer`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/renderer/PEntityRenderer.java)
* [`PModelData`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/renderer/modelData/PModelData.java)
* [`PAnimationController`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/animatable/PAnimationController.java)
