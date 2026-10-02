package com.oierbravo.createsifter.compat.jei.category.animations;

  import com.mojang.blaze3d.platform.Lighting;
  import com.mojang.blaze3d.vertex.ByteBufferBuilder;
  import com.mojang.blaze3d.vertex.PoseStack;
  import com.mojang.math.Axis;
  import com.simibubi.create.compat.jei.category.animations.AnimatedKinetics;
  import com.simibubi.create.content.kinetics.base.KineticBlock;
  import com.simibubi.create.foundation.gui.AllGuiTextures;
  import com.tterrag.registrate.util.entry.BlockEntry;
  import dev.engine_room.flywheel.lib.model.baked.PartialModel;
  import java.lang.reflect.Field;
  import net.createmod.catnip.gui.UIRenderHelper;
  import net.createmod.catnip.platform.NeoForgeCatnipServices;
  import net.minecraft.client.Minecraft;
  import net.minecraft.client.gui.GuiGraphics;
  import net.minecraft.client.renderer.MultiBufferSource;
  import net.minecraft.client.resources.model.BakedModel;
  import net.minecraft.client.resources.model.ModelResourceLocation;
  import net.minecraft.world.level.material.Fluids;

  public abstract class AbstractAnimatedSifter<SIFTER extends KineticBlock> extends AnimatedKinetics {
      private boolean isWaterlogged;

      /** Flywheel's {@code PartialModel.bakedModel} field, resolved by type so it survives remapping. */
      private static final Field BAKED_MODEL_FIELD = findBakedModelField();

      public AbstractAnimatedSifter() {
          isWaterlogged = false;
      }

      public AbstractAnimatedSifter<SIFTER> waterlogged(boolean value) {
          isWaterlogged = value;
          return this;
      }

      @Override
      public void draw(GuiGraphics guiGraphics, int xOffset, int yOffset) {
          PoseStack matrixStack = guiGraphics.pose();
          matrixStack.pushPose();
          matrixStack.translate(xOffset, yOffset, 0);
          AllGuiTextures.JEI_SHADOW.render(guiGraphics, -16, 13);
          matrixStack.translate(-2, 18, 0);
          int scale = 22;

          ensureModelReady(getCogModel());
          ensureModelReady(getMeshModel());

          if (getCogModel() != null && getCogModel().get() != null) {
              blockElement(getCogModel())
                  .atLocal(0, 0.1, 0)
                  .rotateBlock(22.5, getCurrentAngle() * 2, 0)
                  .scale(scale)
                  .render(guiGraphics);
          }

          blockElement(getSifterBlock().getDefaultState())
              .atLocal(0, 0.1, 0)
              .rotateBlock(22.5, 22.5, 0)
              .scale(scale)
              .render(guiGraphics);

          if (getMeshModel() != null && getMeshModel().get() != null) {
              blockElement(getMeshModel())
                  .atLocal(0, -1, 0)
                  .rotateBlock(22.5, 22.5, 0)
                  .scale(scale)
                  .render(guiGraphics);
          }

          if (isWaterlogged)
              renderWaterlogged(guiGraphics);

          matrixStack.popPose();
      }

      /**
       * Flywheel normally fills {@link PartialModel#get()} during model baking, but on some
       * NeoForge/Create versions that step is skipped and {@code get()} stays null, which
       * makes GuiGameElement throw a NPE. Resolve the baked model straight from the
       * ModelManager and write it back into the PartialModel so the normal render path works.
       */
      private static void ensureModelReady(PartialModel partial) {
          if (partial == null || partial.get() != null || BAKED_MODEL_FIELD == null)
              return;
          try {
              BakedModel baked = Minecraft.getInstance().getModelManager()
                  .getModel(ModelResourceLocation.standalone(partial.modelLocation()));
              if (baked != null)
                  BAKED_MODEL_FIELD.set(partial, baked);
          } catch (Throwable ignored) {
          }
      }

      private static Field findBakedModelField() {
          for (Field field : PartialModel.class.getDeclaredFields()) {
              if (field.getType() == BakedModel.class) {
                  try {
                      field.setAccessible(true);
                  } catch (Throwable ignored) {
                  }
                  return field;
              }
          }
          return null;
      }

      private void renderWaterlogged(GuiGraphics guiGraphics) {
          AnimatedKinetics.DEFAULT_LIGHTING.applyLighting();
          MultiBufferSource.BufferSource buffer =
              MultiBufferSource.immediate(new ByteBufferBuilder(1536));
          PoseStack matrixStack = guiGraphics.pose();
          matrixStack.pushPose();
          UIRenderHelper.flipForGuiRender(matrixStack);
          matrixStack.scale(22, 18, 22);
          matrixStack.translate(-0.3, -0.1, 0);
          float from = 0.0625f;
          float to = 1.125f;
          matrixStack.mulPose(Axis.XP.rotationDegrees(22.5f));
          matrixStack.mulPose(Axis.YP.rotationDegrees(22.5f));

          NeoForgeCatnipServices.FLUID_RENDERER.renderFluidBox(
              Fluids.WATER.defaultFluidState(),
              from, from, from, to, to, to,
              guiGraphics.bufferSource(), matrixStack, 15728880, false, true);
          guiGraphics.flush();
          Lighting.setupFor3DItems();
          matrixStack.popPose();
      }

      abstract PartialModel getMeshModel();

      abstract PartialModel getCogModel();

      abstract BlockEntry<SIFTER> getSifterBlock();
  }
