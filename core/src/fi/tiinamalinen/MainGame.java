package fi.tiinamalinen;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObjects;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.physics.box2d.WorldManifold;
import com.badlogic.gdx.utils.Array;

public class MainGame extends ApplicationAdapter implements GestureDetector.GestureListener {

    final private int TILE_WIDTH = 32;
    final private float WINDOW_WIDTH = 8f;
    final private float WINDOW_HEIGHT = 4.8f;
    final private int TILEAMOUNT_WIDTH = 40;
    final private int TILEAMOUNT_HEIGHT = 50;
    final private int WORLD_HEIGHT_PIXELS = TILEAMOUNT_HEIGHT * TILE_WIDTH;
    final private int WORLD_WIDTH_PIXELS = TILEAMOUNT_WIDTH * TILE_WIDTH;
    final private float PLAYER_RADIUS = 0.3f;


    SpriteBatch batch;
    private OrthographicCamera camera;
    private TiledMapRenderer tiledMapRenderer;
    private TiledMap tiledMap;
    private Box2DDebugRenderer debugRenderer;
    private World world;

	private Texture furry;
	private Body furryBody;
    private Texture bat;
    private Body batBody;
    private Texture spider;
    private  Body spiderBody;
    private Sound crystalSound;
    private Sound jumpSound;
    private Sound gameover;
    private boolean tapping = false;
    private boolean gameContinue = true;
    private Array<Body> bodiesToRemove = new Array<Body>();
    private float enemyDirection = -1;
	
	@Override
	public void create () {
		batch = new SpriteBatch();
        debugRenderer = new Box2DDebugRenderer();
        world = new World(new Vector2(0, -9.81f), true);

        camera = new OrthographicCamera();
        camera.setToOrtho(false, WINDOW_WIDTH, WINDOW_HEIGHT);

        createPlayer();
        createEnemies();
        crystalSound = Gdx.audio.newSound(Gdx.files.internal("Twinkle-sound-effect.mp3"));;
        jumpSound = Gdx.audio.newSound(Gdx.files.internal("jump.mp3"));;
        gameover = Gdx.audio.newSound(Gdx.files.internal("gameover.wav"));;

        tiledMap = new TmxMapLoader().load("map.tmx");
        tiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap, 1/100f);
        transformWallsToBodies("ground", "ground");
        transformWallsToBodies("itemit", "items");

        moveCamera();
        Gdx.input.setInputProcessor(new GestureDetector(this));
        collisionDetection();
	}

	@Override
	public void render () {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        tiledMapRenderer.setView(camera);
        batch.setProjectionMatrix(camera.combined);
        debugRenderer.render(world, camera.combined);

        if (gameContinue) {
            doPhysicsStep(Gdx.graphics.getDeltaTime());
            userInput();
            moveEnemies();
        }
        tiledMapRenderer.render();
        camera.translate(0,0);

        camera.update();
        moveCamera();

		batch.begin();
        batch.draw(furry,
                furryBody.getPosition().x - PLAYER_RADIUS,
                furryBody.getPosition().y - PLAYER_RADIUS,
                PLAYER_RADIUS,                   // originX
                PLAYER_RADIUS,                   // originY
                PLAYER_RADIUS * 2,               // width
                PLAYER_RADIUS * 2,               // height
                1.0f,                          // scaleX
                1.0f,                          // scaleY
                furryBody.getTransform().getRotation() * MathUtils.radiansToDegrees,
                0,                             // Start drawing from x = 0
                0,                             // Start drawing from y = 0
                furry.getWidth(),       // End drawing x
                furry.getHeight(),      // End drawing y
                false,                         // flipX
                false);                        // flipY
        batch.draw(bat, batBody.getPosition().x, batBody.getPosition().y, PLAYER_RADIUS *3, PLAYER_RADIUS *1.5f);
        batch.draw(spider, spiderBody.getPosition().x, spiderBody.getPosition().y, PLAYER_RADIUS *2, PLAYER_RADIUS *1.5f);
		batch.end();

        removeBodies();

	}
	
	@Override
	public void dispose () {
		batch.dispose();
		furry.dispose();
		bat.dispose();
		spider.dispose();
		world.dispose();
	}

    private void moveCamera() {
        camera.position.set(furryBody.getPosition().x, furryBody.getPosition().y, 0);

        // left
        if (camera.position.x < WINDOW_WIDTH / 2) {
            camera.position.x = WINDOW_WIDTH / 2;
        }
        // up
        if (camera.position.y > WORLD_HEIGHT_PIXELS - WINDOW_HEIGHT / 2) {
            camera.position.y = WORLD_HEIGHT_PIXELS - WINDOW_HEIGHT / 2;
        }
        // down
        if (camera.position.y < WINDOW_HEIGHT / 2) {
            camera.position.y = WINDOW_HEIGHT / 2;
        }
        // right
        if (camera.position.x > WORLD_WIDTH_PIXELS / 2f) {
            camera.position.x = WORLD_WIDTH_PIXELS / 2f;
        }
    }

    public void createPlayer() {
        furry = new Texture(Gdx.files.internal("furry.png"));

        BodyDef myBodyDef = new BodyDef();
        myBodyDef.type = BodyDef.BodyType.DynamicBody;

        myBodyDef.position.set(6, 8);

        furryBody = world.createBody(myBodyDef);
        furryBody.setUserData("player");
        FixtureDef playerFixtureDef = new FixtureDef();

        playerFixtureDef.density     = 2;
        playerFixtureDef.restitution = 0.9f;
        playerFixtureDef.friction    = 0.5f;

        CircleShape circleshape = new CircleShape();
        circleshape.setRadius(PLAYER_RADIUS);

        playerFixtureDef.shape = circleshape;
        furryBody.createFixture(playerFixtureDef);
    }

    public void createEnemies() {
        bat = new Texture(Gdx.files.internal("bat.png"));
        spider = new Texture(Gdx.files.internal("spider.png"));

        BodyDef batBodyDef = new BodyDef();
        BodyDef spiderBodyDef = new BodyDef();
        batBodyDef.type = BodyDef.BodyType.DynamicBody;
        spiderBodyDef.type = BodyDef.BodyType.DynamicBody;
        batBodyDef.position.set(5, 14);
        spiderBodyDef.position.set(4.5f, 3f);
        batBody = world.createBody(batBodyDef);
        spiderBody = world.createBody(spiderBodyDef);
        batBody.setUserData("enemy");
        spiderBody.setUserData("enemy");

        FixtureDef batFixtureDef = new FixtureDef();
        FixtureDef spiderFixtureDef = new FixtureDef();

        batFixtureDef.density     = 1;
        batFixtureDef.restitution = 0.5f;
        batFixtureDef.friction    = 0.5f;
        spiderFixtureDef.density     = 0;
        spiderFixtureDef.restitution = 0.5f;
        spiderFixtureDef.friction    = 0.5f;

        CircleShape circleshape = new CircleShape();
        circleshape.setRadius(PLAYER_RADIUS);

        batFixtureDef.shape = circleshape;
        spiderFixtureDef.shape = circleshape;
        batBody.createFixture(batFixtureDef);
        spiderBody.createFixture(batFixtureDef);
    }

    private double accumulator = 0;
    private float TIME_STEP = 1/60f;
    private void doPhysicsStep(float deltaTime) {
        float frameTime = deltaTime;
        if (deltaTime > 1/4f) {
            frameTime = 1/4f;
        }
        accumulator += frameTime;
        while (accumulator >= TIME_STEP) {
            world.step(TIME_STEP, 8, 3);
            accumulator -= TIME_STEP;
        }
    }

    private void userInput() {
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || tapping) {
            furryBody.applyLinearImpulse(new Vector2(0f, 1f), furryBody.getWorldCenter(), true);
            tapping = false;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT) || Gdx.input.getAccelerometerY() < -5) {
            furryBody.applyForceToCenter(new Vector2(-2f, 0f), true);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT) || Gdx.input.getAccelerometerY() > 5) {
            furryBody.applyForceToCenter(new Vector2(2f, 0f), true);
        }
    }

    private void moveEnemies() {
        if (batBody.getPosition().x <= 0) {
            enemyDirection = 1;
        }
        else if (batBody.getPosition().x >= WINDOW_WIDTH - PLAYER_RADIUS) {
            enemyDirection = -1;
        }
        batBody.applyLinearImpulse(new Vector2(enemyDirection/10, 0.001f), batBody.getWorldCenter(), true);
    }

    private void removeBodies() {
        for (Body body : bodiesToRemove) {
            world.destroyBody(body);
        }
        bodiesToRemove = new Array<Body>();
    }

    private void transformWallsToBodies(String layer, String userData) {
        // Let's get the collectable rectangles layer
        MapLayer collisionObjectLayer = tiledMap.getLayers().get(layer);

        // All the rectangles of the layer
        MapObjects mapObjects = collisionObjectLayer.getObjects();

        // Cast it to RectangleObjects array
        Array<RectangleMapObject> rectangleObjects = mapObjects.getByType(RectangleMapObject.class);

        // Iterate all the rectangles
        for (RectangleMapObject rectangleObject : rectangleObjects) {
            Rectangle tmp = rectangleObject.getRectangle();

            // SCALE given rectangle down if using world dimensions!
            Rectangle rectangle = scaleRect(tmp, 1 / 100f);

            createStaticBody(rectangle, userData);
        }
    }

    private Rectangle scaleRect(Rectangle r, float scale) {
        Rectangle rectangle = new Rectangle();
        rectangle.x      = r.x * scale;
        rectangle.y      = r.y * scale;
        rectangle.width  = r.width * scale;
        rectangle.height = r.height * scale;
        return rectangle;
    }

    public void createStaticBody(Rectangle rect, String userData) {
        BodyDef myBodyDef = new BodyDef();
        myBodyDef.type = BodyDef.BodyType.StaticBody;

        float x = rect.getX();
        float y = rect.getY();
        float width = rect.getWidth();
        float height = rect.getHeight();

        float centerX = width/2 + x;
        float centerY = height/2 + y;

        myBodyDef.position.set(centerX, centerY);

        Body wall = world.createBody(myBodyDef);

        wall.setUserData(userData);
        // Create shape
        PolygonShape groundBox = new PolygonShape();

        // Real width and height is 2 X this!
        groundBox.setAsBox(width / 2 , height / 2 );

        wall.createFixture(groundBox, 0.0f);
    }


    @Override
    public boolean touchDown(float x, float y, int pointer, int button) {
        return false;
    }

    @Override
    public boolean tap(float x, float y, int count, int button) {
        tapping = true;
        return true;
    }

    @Override
    public boolean longPress(float x, float y) {
        return false;
    }

    @Override
    public boolean fling(float velocityX, float velocityY, int button) { return false; }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY) {
        return false;
    }

    @Override
    public boolean panStop(float x, float y, int pointer, int button) {
        return false;
    }

    @Override
    public boolean zoom(float initialDistance, float distance) {
        return false;
    }

    @Override
    public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
        return false;
    }

    @Override
    public void pinchStop() {

    }

    private void collisionDetection() {
        world.setContactListener(new ContactListener() {
            @Override
            public void beginContact(Contact contact) {
                Body bodyA = contact.getFixtureA().getBody();
                Body bodyB = contact.getFixtureB().getBody();
                String userDataA = (String) (bodyA.getUserData());
                String userDataB = (String) (bodyB.getUserData());
                if (userDataA.equals("items") && userDataB.equals("player")) {
                    contact.setEnabled(false);
                    crystalSound.play();
                    bodiesToRemove.add(contact.getFixtureA().getBody());
                    clearCollectable( bodyA.getPosition().x, bodyA.getPosition().y);
                }
                if (userDataA.equals("player") && userDataB.equals("items")) {
                    contact.setEnabled(false);
                    crystalSound.play();
                    bodiesToRemove.add(contact.getFixtureB().getBody());
                    clearCollectable( bodyB.getPosition().x, bodyB.getPosition().y);
                }
                if ((userDataA.equals("enemy") && userDataB.equals("player")) || (userDataA.equals("player") && userDataB.equals("enemy"))) {
                    gameContinue = false;
                    gameover.play();
                }
                if ((userDataA.equals("enemy") && userDataB.equals("ground")) || (userDataA.equals("ground") && userDataB.equals("enemy"))) {
                    enemyDirection = enemyDirection * -1;
                }

            }

            @Override
            public void endContact(Contact contact) {
                Body bodyA = contact.getFixtureA().getBody();
                Body bodyB = contact.getFixtureB().getBody();
                String userDataA = (String) (bodyA.getUserData());
                String userDataB = (String) (bodyB.getUserData());
                if ((userDataA.equals("ground") && userDataB.equals("player")) || (userDataA.equals("ground") && userDataB.equals("player"))) {
                    jumpSound.play();
                }
            }

            @Override
            public void preSolve(Contact contact, Manifold oldManifold) {
            }

            @Override
            public void postSolve(Contact contact, ContactImpulse impulse) {
            }
        });
    }

    private void clearCollectable(float xCoord, float yCoord) {
        int indexX = (int) (xCoord * 3.1);
        int indexY = (int) (yCoord * 3.1);
        TiledMapTileLayer itemCells = (TiledMapTileLayer) tiledMap.getLayers().get("Tile layer 1");

        System.out.println(xCoord);
        System.out.println(yCoord);

        itemCells.setCell( indexX, indexY, null);

    }
}
