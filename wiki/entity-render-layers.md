# Entity Render Layers

[`PEntityRenderLayer`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/renderer/PEntityRenderLayer.java) renders an additional PulseLib model on top of a [`PEntityRenderer`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/renderer/PEntityRenderer.java).

Layers are useful for armor pieces, equipment, accessories, conditional attachments, or model variants.

## Layer class

```java
public class RobotChestLayer extends PEntityRenderLayer<RobotEntity> {
    public static final PModelData MODEL = PModelData.entityLayer(
            MyEntities.ROBOT.getId(),
            ResourceLocation.fromNamespaceAndPath("examplemod", "armor"));

    public RobotChestLayer() {
        super(MODEL,
                PRenderTypes.RenderTypeProvider::trianglesSolid);
    }

    @Override
    public boolean shouldRender(RobotEntity animatable) {
        return animatable.hasChestPlate();
    }
}
```

Register the same `MODEL` object in `RegisterResourceEvent` with the layer model's material references. This gives the renderer and resource registration one canonical ID.

## Add layer to renderer

```java
public class RobotRenderer extends PEntityRenderer<RobotEntity> {
    public RobotRenderer(EntityRendererProvider.Context context) {
        super(context, ROBOT_MODEL_DATA, PRenderTypes.RenderTypeProvider::trianglesSolid);

        addRenderLayer("body",
                new RobotChestLayer()
                        .bindBone("armor_body", "body")
                        .bindBone("armor_head", "head"));
    }
}
```

`addRenderLayer(anchorBoneName, layer)` attaches the layer at the named bone on the base model.

## Bone bindings

Layer bone bindings copy base entity bone poses into layer bones:

```java
new RobotChestLayer()
        .bindBone("layer_right_arm", "right_arm")
        .bindMatchingBone("head")
        .bindMatchingBones("left_leg", "right_leg");
```

Use this when the layer model should follow animated limbs from the base model.

## Transform and appearance hooks

```java
RobotChestLayer layer = new RobotChestLayer();
layer.setOffset(new Vector3f(0.0f, 0.05f, 0.0f));
layer.setRotation(new Quaternionf().rotationXYZ(0, 0, 0));
layer.setScale(new Vector3f(1.05f, 1.05f, 1.05f));
```

Override appearance hooks:

```java
@Override
public int getColor(RobotEntity entity, PBakedBone bone, PBakedMesh mesh, int packedColor) {
    return entity.isPowered() ? 0xFF80FFFF : packedColor;
}

@Override
public int getPackedLight(RobotEntity entity, int packedLight) {
    return entity.isPowered() ? LightTexture.FULL_BRIGHT : packedLight;
}
```

Classes used:

* [`PEntityRenderLayer`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/renderer/PEntityRenderLayer.java)
* [`PModelData`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/renderer/modelData/PModelData.java)
* [`PBakedBone`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/model/baked/PBakedBone.java)
* [`PBakedMesh`](https://github.com/ArcAnc/PulseLib/blob/1.21.1/src/main/java/com/arcanc/pulselib/content/model/baked/PBakedMesh.java)
