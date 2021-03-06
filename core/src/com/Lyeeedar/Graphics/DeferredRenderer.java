package com.Lyeeedar.Graphics;

import com.Lyeeedar.Collision.Octtree.OcttreeFrustum;
import com.Lyeeedar.Entity.Processors.RenderProcessor;
import com.Lyeeedar.Graphics.Lights.Light;
import com.Lyeeedar.Lirard.GLOBALS;
import com.Lyeeedar.Util.FileUtils;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;

public class DeferredRenderer
{
	private static final boolean DEBUG = false;
	
	protected final SpriteBatch sbatch;
	
	protected final Camera cam;
	protected final RenderProcessor processor;
	
	protected final InstanceRenderer instanceRenderer;
	
	protected final FrameBuffer gbuffer;
	protected final FrameBuffer lbuffer;
	protected final FrameBuffer fbuffer;
	protected int bufferWidth;
	protected int bufferHeight;
	
	protected ShaderProgram ssaoShader;
	
	protected final Texture blank;
	protected Matrix4 tmpMat = new Matrix4();
	
	protected final Array<Light> lightArray = new Array<Light>(false, 16);
	protected final OcttreeFrustum octtreeShape = new OcttreeFrustum();
	
	public DeferredRenderer(RenderProcessor processor)
	{
		this.processor = processor;
		this.cam = processor.cam;
		
		this.instanceRenderer = new InstanceRenderer();
		
		bufferWidth = GLOBALS.RESOLUTION[0];
		bufferHeight = GLOBALS.RESOLUTION[1];
		
		gbuffer = new FrameBuffer(Format.RGBA8888, bufferWidth, bufferHeight, 4, true);
		lbuffer = new FrameBuffer(Format.RGBA8888, bufferWidth, bufferHeight, 1, true);
		fbuffer = new FrameBuffer(Format.RGBA8888, bufferWidth, bufferHeight, 1, true, gbuffer.getDepthBufferTexture());
		
		sbatch = new SpriteBatch();
				
		setupSSAO();
		
		blank = FileUtils.loadTexture("data/textures/blank.png", true, null, null);
	}
	
	public void render()
	{
		sbatch.getProjectionMatrix().setToOrtho2D(0, 0, bufferWidth, bufferHeight);
		// Draw G Buffers
		gbuffer.begin();
		
		Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		Gdx.gl.glEnable(GL20.GL_CULL_FACE);
		Gdx.gl.glCullFace(GL20.GL_BACK);
		
		Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
		Gdx.gl.glDepthFunc(GL20.GL_LESS);
		Gdx.gl.glDepthMask(true);
		
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
		
		Gdx.gl.glDisable(GL30.GL_BLEND);
		
		instanceRenderer.render(processor.sorter, GLOBALS.LIGHTS, cam);
		processor.sorter.clear();
		
		gbuffer.end();
		
		Texture depth = gbuffer.getDepthBufferTexture();
		Texture albedo = gbuffer.getColorBufferTexture(0);
		Texture normals = gbuffer.getColorBufferTexture(1);
		Texture specular = gbuffer.getColorBufferTexture(2);
		Texture emissive = gbuffer.getColorBufferTexture(3);
		
		lbuffer.begin();
		
		Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		Gdx.gl.glDepthMask(false);
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
		Gdx.gl.glDisable(GL20.GL_CULL_FACE);
		
		// Do SSAO + ambient 
		sbatch.setShader(ssaoShader);
		sbatch.setColor(GLOBALS.LIGHTS.ambientColour.x, GLOBALS.LIGHTS.ambientColour.y, GLOBALS.LIGHTS.ambientColour.z, 1);
		sbatch.enableBlending();
		sbatch.setBlendFunction(GL30.GL_ONE, GL30.GL_ONE);
		sbatch.begin();
		
		depth.bind(1);
		ssaoShader.setUniformi("u_depth", 1);	
		normals.bind(2);
		ssaoShader.setUniformi("u_normal", 2);
		
		ssaoShader.setUniformMatrix("u_invProj", cam.invProjectionView);		
		ssaoShader.setUniformf("distanceThreshold", 5);
		ssaoShader.setUniformf("filterRadius", 10.0f/cam.viewportWidth, 10.0f/cam.viewportHeight);
		
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
		
		sbatch.draw(blank, 0, 0, lbuffer.getWidth(), lbuffer.getHeight(), 0, 0, blank.getWidth(), blank.getHeight(), false, true);
		sbatch.end();
		
		// Do Lighting
		
//		DirectionalLight sdl = null;
//		
//		octtreeShape.frustum = cam;
//		lightArray.clear();
//		GLOBALS.LIGHTS.collectAll(lightArray, octtreeShape, Octtree.MASK_DIRECTION_LIGHT | Octtree.MASK_POINT_LIGHT);
//		for (Light l : lightArray)
//		{
//			if (l instanceof DirectionalLight)
//			{
//				DirectionalLight dl = (DirectionalLight) l;
//				sdl = dl;
//				ShaderProgram shader = dl.shadowCasting ? DirectionalLight.shadowShader : DirectionalLight.noShadowShader;
//				
//				sbatch.setShader(shader);
//				sbatch.setColor(dl.colour.x, dl.colour.y, dl.colour.z, 1);
//				sbatch.begin();
//				
//				shader.setUniformf("u_dir", dl.direction);
//				normals.bind(2);
//				shader.setUniformi("u_normal", 2);
//				depth.bind(3);
//				shader.setUniformi("u_depth", 3);
//				specular.bind(1);
//				shader.setUniformi("u_specular", 1);	
//				
//				shader.setUniformMatrix("u_invProj", cam.invProjectionView);
//				shader.setUniformf("u_viewPos", cam.position);
//				
//				if (dl.shadowCasting)
//				{
//					dl.shadowMap.bind(4);
//					shader.setUniformi("u_shadowMap", 4);
//					shader.setUniformMatrix("u_depthBiasMVP", dl.depthBiasMVP);
//					shader.setUniformf("u_poisson_scale", 1.0f/dl.shadowMap.getWidth(), 1.0f/dl.shadowMap.getHeight());
//				}
//				
//				Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
//				sbatch.draw(blank, 0, 0, lbuffer.getWidth(), lbuffer.getHeight(), 0, 0, blank.getWidth(), blank.getHeight(), false, true);
//				
//				sbatch.end();
//			}
//		}
		
		lbuffer.end();
		
		Texture lighting = lbuffer.getColorBufferTexture();
		
		fbuffer.begin();
		
		Gdx.gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		Gdx.gl.glDisable(GL20.GL_CULL_FACE);
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
		Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
		Gdx.gl.glDepthMask(false);
		
		if (DEBUG)
		{
			sbatch.setColor(1, 1, 1, 1);
			sbatch.setShader(null);
			sbatch.disableBlending();
			sbatch.begin();
			
			sbatch.draw(albedo, 0, 0, fbuffer.getWidth()/2, fbuffer.getHeight()/2, 0, 0, albedo.getWidth(), albedo.getHeight(), false, true);
			sbatch.draw(depth, fbuffer.getWidth()/2, 0, fbuffer.getWidth()/2, fbuffer.getHeight()/2, 0, 0, depth.getWidth(), depth.getHeight(), false, true);
			sbatch.draw(normals, 0, fbuffer.getHeight()/2, fbuffer.getWidth()/2, fbuffer.getHeight()/2, 0, 0, normals.getWidth(), normals.getHeight(), false, true);
			sbatch.draw(lighting, fbuffer.getWidth()/2, fbuffer.getHeight()/2, fbuffer.getWidth()/2, fbuffer.getHeight()/2, 0, 0, lighting.getWidth(), lighting.getHeight(), false, true);
			
			//sbatch.draw(sdl.shadowMap, fbuffer.getWidth()/2, 0, fbuffer.getWidth()/2, fbuffer.getHeight()/2, 0, 0, sdl.shadowMap.getWidth(), sdl.shadowMap.getHeight(), false, true);
			
			sbatch.end();
		}
		else
		{		
			// Compose  image
			sbatch.setColor(1, 1, 1, 1);
			sbatch.enableBlending();
			sbatch.setShader(null);
			
			sbatch.begin();
			
			sbatch.setBlendFunction(GL30.GL_ONE, GL30.GL_ONE);
			sbatch.draw(albedo, 0, 0, fbuffer.getWidth(), fbuffer.getHeight(), 0, 0, albedo.getWidth(), albedo.getHeight(), false, true);
			sbatch.setBlendFunction(GL30.GL_ZERO, GL30.GL_SRC_COLOR);
			sbatch.draw(lighting, 0, 0, fbuffer.getWidth(), fbuffer.getHeight(), 0, 0, lighting.getWidth(), lighting.getHeight(), false, true);
			sbatch.setBlendFunction(GL30.GL_ONE, GL30.GL_ONE);
			sbatch.draw(emissive, 0, 0, fbuffer.getWidth(), fbuffer.getHeight(), 0, 0, emissive.getWidth(), emissive.getHeight(), false, true);
			sbatch.end();
		}
		
		fbuffer.end();
		
		Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT | GL30.GL_DEPTH_BUFFER_BIT);
		Gdx.gl.glDisable(GL20.GL_CULL_FACE);
		Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
		Gdx.gl.glDepthMask(false);
		
		sbatch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		sbatch.begin();
		sbatch.disableBlending();
		sbatch.draw(fbuffer.getColorBufferTexture(), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0, 0, fbuffer.getColorBufferTexture().getWidth(), fbuffer.getColorBufferTexture().getHeight(), false, true);
		sbatch.end();
	}
	
	private void setupSSAO()
	{		
		String vert = Gdx.files.internal("data/shaders/deferred/ssao.vertex.glsl").readString();
		String frag = Gdx.files.internal("data/shaders/deferred/ssao.fragment.glsl").readString();
		ssaoShader = new ShaderProgram(vert, frag);
		if (!ssaoShader.isCompiled()) System.err.println(ssaoShader.getLog());
	}
}
