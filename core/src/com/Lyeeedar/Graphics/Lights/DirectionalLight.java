package com.Lyeeedar.Graphics.Lights;

import com.Lyeeedar.Collision.Octtree;
import com.Lyeeedar.Lirard.GLOBALS;
import com.badlogic.gdx.math.Vector3;

public class DirectionalLight extends Light
{
	public final boolean shadowCasting;
	public final Vector3 direction = new Vector3();
	
	public DirectionalLight()
	{
		this(GLOBALS.DEFAULT_ROTATION, new Vector3(1, 1, 1), true);
	}
	
	public DirectionalLight(Vector3 direction, Vector3 colour, boolean shadowCasting)
	{
		super(colour);
		
		this.direction.set(direction);
		this.shadowCasting = shadowCasting;
	}

	@Override
	public void createEntry(Octtree<Light> octtree)
	{
		int bitmask = shadowCasting ? Octtree.MASK_DIRECTION_LIGHT | Octtree.MASK_SHADOW_CASTING : Octtree.MASK_DIRECTION_LIGHT;
		this.entry = octtree.createEntry(this, new Vector3(), new Vector3().set(octtree.max).sub(octtree.min), bitmask);
	}
	
//	public static ShaderProgram noShadowShader;
//	public static ShaderProgram shadowShader;
//	
//	public static void setupShaders()
//	{
//		if (noShadowShader != null && shadowShader != null) return;
//		
//		String vert = Gdx.files.internal("data/shaders/deferred/directional_light.vertex.glsl").readString();
//		String frag = Gdx.files.internal("data/shaders/deferred/directional_light.fragment.glsl").readString();
//		noShadowShader = new ShaderProgram(vert, frag);
//		if (!noShadowShader.isCompiled()) System.err.println(noShadowShader.getLog());
//		
//		vert = Gdx.files.internal("data/shaders/deferred/directional_light_shadow.vertex.glsl").readString();
//		frag = Gdx.files.internal("data/shaders/deferred/directional_light_shadow.fragment.glsl").readString();
//		shadowShader = new ShaderProgram(vert, frag);
//		if (!shadowShader.isCompiled()) System.err.println(shadowShader.getLog());
//	}
//	
//	public FrameBuffer frameBuffer;
//	public Camera orthoCam;
//	public OcttreeFrustum oFrustum;
//	public Array<Entity> shadowEntities;
//	public HashMap<Class, Batch> batches;
//	public Texture shadowMap;
//	public Matrix4 biasMatrix;
//	public Matrix4 depthBiasMVP;
//	public BoundingBox bb;
//	public void enableShadowMapping()
//	{
//		frameBuffer = new FrameBuffer(Format.DEPTH, GLOBALS.RESOLUTION[0]*2, GLOBALS.RESOLUTION[1]*2, 0, true);
//		shadowMap = frameBuffer.getDepthBufferTexture();
//		orthoCam = new OrthographicCamera();
//		oFrustum = new OcttreeFrustum(orthoCam, -1);
//		shadowEntities = new Array<Entity>(false, 16);
//		batches = new HashMap<Class, Batch>();
//		bb = new BoundingBox();
//		
//		batches.put(TexturedMeshBatch.class, new TexturedMeshBatch(RenderType.SIMPLE));
//		batches.put(AnimatedModelBatch.class, new AnimatedModelBatch(12, RenderType.SIMPLE));
//		batches.put(ChunkedTerrainBatch.class, new ChunkedTerrainBatch(RenderType.SIMPLE));
//		batches.put(ModelBatcher.class, new ModelBatcher(RenderType.SIMPLE));
//		
//		biasMatrix = new Matrix4().scl(0.5f).translate(1, 1, 1);
//
//		depthBiasMVP = new Matrix4();		
//		
//		shadowMap.bind(0);
//		//Gdx.gl30.glTexParameteri(shadowMap.glTarget, GL30.GL_TEXTURE_COMPARE_MODE, GL30.GL_COMPARE_REF_TO_TEXTURE);
//		//Gdx.gl30.glTexParameteri(shadowMap.glTarget, GL30.GL_TEXTURE_COMPARE_FUNC, GL30.GL_LESS);
//	}
//	
//	@Override
//	public void computeShadowMap(Camera cam)
//	{
//		bb.min.set(Float.NaN, Float.NaN, Float.NaN);
//		bb.max.set(Float.NaN, Float.NaN, Float.NaN);
//		
//		shadowEntities.clear();
//		GLOBALS.renderTree.collectAll(shadowEntities, oFrustum, Octtree.MASK_SHADOW_CASTING, bb);
//		bb.set(bb.min, bb.max);
//		
//		Vector3 dimensions = bb.getDimensions();
//		Vector3 center = cam.position;//bb.getCenter();
//		
//		float radius = dimensions.x;
//		radius = Math.max(radius, dimensions.y);
//		radius = Math.max(radius, dimensions.z);
//		radius = 300;
//		
//		orthoCam.far = radius*2.0f;
//		orthoCam.viewportWidth = radius;
//		orthoCam.viewportHeight = radius;
//								
//		orthoCam.direction.set(direction).scl(-1);
//		orthoCam.position.set(direction).scl(GLOBALS.FOG_MAX).add(cam.position).sub(center).nor().scl(radius).add(center);
//		orthoCam.up.set(orthoCam.direction).crs(GLOBALS.DEFAULT_UP).crs(orthoCam.direction);
//		orthoCam.near = 1;
//		orthoCam.update();
//		
//		for (Entity e : shadowEntities)
//		{
//			e.queueRenderables(orthoCam, GLOBALS.LIGHTS, 0, batches, false);
//		}
//		
//		frameBuffer.begin();
//		
//		Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
//		Gdx.gl.glEnable(GL20.GL_CULL_FACE);
//		Gdx.gl.glCullFace(GL20.GL_FRONT);
//		
//		Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
//		Gdx.gl.glDepthFunc(GL20.GL_LESS);
//		Gdx.gl.glDepthMask(true);
//		
//		((TexturedMeshBatch) batches.get(TexturedMeshBatch.class)).render(orthoCam, GL20.GL_TRIANGLES, Color.WHITE);
//		((ModelBatcher) batches.get(ModelBatcher.class)).renderSolid(GLOBALS.LIGHTS, orthoCam);
//		((ModelBatcher) batches.get(ModelBatcher.class)).clear();
//		((AnimatedModelBatch) batches.get(AnimatedModelBatch.class)).render(orthoCam, GL20.GL_TRIANGLES, Color.WHITE);
//		((ChunkedTerrainBatch) batches.get(ChunkedTerrainBatch.class)).render(GLOBALS.LIGHTS, orthoCam);
//		
//		frameBuffer.end();
//		
//		shadowMap = frameBuffer.getDepthBufferTexture();
//		
//		depthBiasMVP.set(biasMatrix).mul(orthoCam.combined);		
//	}
}
