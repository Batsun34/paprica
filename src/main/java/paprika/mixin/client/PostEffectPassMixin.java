package paprika.mixin.client;

import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectPipeline;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.FrameGraphBuilder;
import net.minecraft.client.util.Handle;
import net.minecraft.util.Identifier;
import paprika.PaprikaClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(PostEffectPass.class)
public class PostEffectPassMixin {
	@Shadow
	@Final
	private String id;

	@Shadow
	@Final
	@Mutable
	private List<PostEffectPipeline.Uniform> uniforms;

	@Unique
	private float paprika$baseBlurX = Float.NaN;

	@Unique
	private float paprika$baseBlurY = Float.NaN;

	@Unique
	private float paprika$lastAppliedThickness = Float.NaN;

	@Inject(method = "render", at = @At("HEAD"))
	private void paprika$applyOutlineThickness(
			FrameGraphBuilder builder,
			Map<Identifier, Handle<Framebuffer>> handles,
			Matrix4f projectionMatrix,
			CallbackInfo ci
	) {
		if (!PaprikaClient.isPlayerEspEnabled()) {
			return;
		}
		if (this.id == null || !this.id.endsWith("entity_outline_box_blur")) {
			return;
		}
		if (Float.isNaN(this.paprika$baseBlurX) || Float.isNaN(this.paprika$baseBlurY)) {
			for (PostEffectPipeline.Uniform uniform : this.uniforms) {
				if ("BlurDir".equals(uniform.name()) && uniform.values().size() >= 2) {
					this.paprika$baseBlurX = uniform.values().get(0);
					this.paprika$baseBlurY = uniform.values().get(1);
					break;
				}
			}
		}

		if (Float.isNaN(this.paprika$baseBlurX) || Float.isNaN(this.paprika$baseBlurY)) {
			return;
		}

		float thickness = PaprikaClient.getOutlineThickness();
		if (Math.abs(this.paprika$lastAppliedThickness - thickness) < 0.0001F) {
			return;
		}

		List<PostEffectPipeline.Uniform> updatedUniforms = new ArrayList<>(this.uniforms.size());
		for (PostEffectPipeline.Uniform uniform : this.uniforms) {
			if ("BlurDir".equals(uniform.name()) && uniform.values().size() >= 2) {
				updatedUniforms.add(new PostEffectPipeline.Uniform(
						"BlurDir",
						List.of(this.paprika$baseBlurX * thickness, this.paprika$baseBlurY * thickness)
				));
			} else {
				updatedUniforms.add(uniform);
			}
		}

		this.uniforms = updatedUniforms;
		this.paprika$lastAppliedThickness = thickness;
	}
}
