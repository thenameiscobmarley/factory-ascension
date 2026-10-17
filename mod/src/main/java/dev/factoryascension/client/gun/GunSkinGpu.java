package dev.factoryascension.client.gun;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.factoryascension.FactoryAscension;
import dev.factoryascension.client.FAClientConfig;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Weapon skins drawn per pixel (shaders/core/fa_gun_skin): every pixel of the gun works out its skin from where it is
 * on the gun, so patterns stay sharp up close and animate smoothly, instead of being coloured per mesh corner. GunMesh
 * hands the shader the point's position on the gun in spare vertex fields (UV0 = x, y; UV1 = z * 64 and the skin's
 * number). If the shader isn't there (failed to load, a shader pack...), GunSkins' per-corner colours are used.
 */
public final class GunSkinGpu {
    /** The skins in the shader's order (fa_gun_skin.fsh). */
    static final List<String> IDS = List.of("obsidian", "snow", "black_water", "torn", "gold_leaf", "lava", "frost", "neon_grid", "digital",
        "arctic", "tiger", "carbon", "damascus", "rust", "moss", "bone", "galaxy", "toxic", "blood", "chrome", "circuit", "blossom", "inferno",
        "glitch", "hazard", "ghillie", "ember", "aurora");
    static ShaderInstance shader;
    static boolean broken;
    private static final Map<GunMesh.Shader, Integer> NUMBER = new IdentityHashMap<>();
    private static final RenderType[] TYPES = new RenderType[GunMesh.CLASSES.length];

    private GunSkinGpu() {
    }

    public static boolean ready() {
        return shader != null && !broken;
    }

    /** This skin's number in the shader, or -1 to draw it the old way. */
    static int number(GunMesh.Shader s) {
        if (s == null || shader == null || broken || dev.factoryascension.client.render.AscensionLighting.shaderPackActive()) return -1;
        Integer n = NUMBER.get(s);
        if (n == null) {
            n = -1;
            for (int i = 0; i < IDS.size(); i++) {
                GunSkins.Skin k = GunSkins.byId(IDS.get(i));
                if (k != null && k.shader == s) { n = i; break; }
            }
            NUMBER.put(s, n);
        }
        return n;
    }

    static void frame(float time) {
        try {
            shader.safeGetUniform("FaSkinTime").set(time);
            shader.safeGetUniform("FaHeat").set(GunAnim.heat());
        } catch (Throwable err) {
            fail(err);
        }
    }

    static void fail(Throwable err) {
        broken = true;
        FAClientConfig.failSafe("Per-pixel weapon skins", err);
    }

    static RenderType type(int cls) {
        RenderType t = TYPES[cls];
        if (t == null) t = TYPES[cls] = Types.make(new ResourceLocation(FactoryAscension.MODID, "textures/gunmesh/" + GunMesh.CLASSES[cls] + ".png"));
        return t;
    }

    /** RenderType's builders are protected: reached from a subclass. */
    static final class Types extends RenderType {
        private Types(String n, VertexFormat f, VertexFormat.Mode m, int s, boolean a, boolean b, Runnable on, Runnable off) {
            super(n, f, m, s, a, b, on, off);
        }

        static RenderType make(ResourceLocation tex) {
            CompositeState state = CompositeState.builder()
                .setShaderState(new ShaderStateShard(() -> shader))
                .setTextureState(new TextureStateShard(tex, false, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(true);
            return create("factoryascension_gun_skin_" + tex.getPath().replace('/', '_'), DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 4096,
                true, false, state);
        }
    }

    @Mod.EventBusSubscriber(modid = FactoryAscension.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Load {
        @SubscribeEvent
        public static void shaders(RegisterShadersEvent event) {
            try {
                event.registerShader(new ShaderInstance(event.getResourceProvider(), new ResourceLocation(FactoryAscension.MODID, "fa_gun_skin"),
                    DefaultVertexFormat.NEW_ENTITY), s -> shader = s);
            } catch (Throwable err) {
                shader = null;
                com.mojang.logging.LogUtils.getLogger().error("Per-pixel weapon skins failed to load; skins are drawn per corner", err);
            }
        }
    }
}
