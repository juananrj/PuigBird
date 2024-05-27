package com.mygdx.bird;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.Timer;

import java.util.Iterator;
import java.util.Random;

public class GameScreen implements Screen {
    final Bird game;
    OrthographicCamera camera;
    Stage stage;
    Player player;
    boolean dead;
    Array<Pipe> obstacles;
    long lastObstacleTime;
    float score;

    boolean drunk;
    boolean pilled;

    Moneda moneda;
    Estrella estrella;
    long MonedaTime;
    long EstrellaTime;
    int LastMoneda;
    int lastEstrella;
    boolean MonedaTouch;
    boolean EstrellaTouch;
    int MonedaSuma = 25;

    Music justBurningMemory;

    public GameScreen(final Bird gam) {
        this.game = gam;
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 800, 480);

        player = new Player();
        player.setManager(game.manager);
        stage = new Stage();
        stage.getViewport().setCamera(camera);
        stage.addActor(player);

        // create the obstacles array and spawn the first obstacle
        obstacles = new Array<Pipe>();

        spawnObstacle();

        // Initial score
        score = 0;
        LastMoneda = 0;
        lastEstrella = 0;
        MonedaTouch = false;
        justBurningMemory = Gdx.audio.newMusic(Gdx.files.internal("background_music.mp3"));
        if (!justBurningMemory.isPlaying()) {
            justBurningMemory.play();
        }
    }

    @Override
    public void render(float delta) {
        dead = false;
        // clear the screen with a color
        ScreenUtils.clear(0.3f, 0.8f, 0.8f, 1);
        // tell the camera to update its matrices.
        camera.update();
        // tell the SpriteBatch to render in the
        // coordinate system specified by the camera.
        game.batch.setProjectionMatrix(camera.combined);
        // begin a new batch
        game.batch.begin();
        game.batch.draw(game.manager.get("background.png", Texture.class), 0, 0);
        game.batch.end();
        // Stage batch: Actors
        stage.getBatch().setProjectionMatrix(camera.combined);
        stage.draw();
        // process user input
        if (Gdx.input.justTouched()) {
            Random random = new Random();
            if(random.nextFloat() >= 0.75f){
            }
            player.impulso();
            player.velocidadX();
            game.manager.get("flap.wav", Sound.class).play();
        }
        stage.act();
        // Check that the player does not go off the screen.
        // If it goes off the bottom, game over.
        if (player.getBounds().y > 480 - 45){
            player.setY( 480 - 45 );
        }
        if (player.getBounds().y < 0 - 45) {
            dead = true;
        }

        if (TimeUtils.nanoTime() - lastObstacleTime >= 750000000 && LastMoneda == 3) {
            spawnMoneda();
            MonedaTouch = true;
            LastMoneda = 0;
        }

        if (TimeUtils.nanoTime() - lastObstacleTime >= 750000000 && lastEstrella == 5) {
            spawnEstrella();
            EstrellaTouch = true;
            lastEstrella = 0;
        }

        if (drunk && TimeUtils.nanoTime() >= MonedaTime) {
            drunk = false;
            player.setDrunk(false);
            player.remove();
            stage.addActor(player);
        }

        if (pilled && TimeUtils.nanoTime() >= EstrellaTime) {
            pilled = false;
            player.setPilled(false);
            player.remove();
            stage.addActor(player);
        }

        if (EstrellaTouch) {
            if (estrella.getBounds().overlaps(player.getBounds())) {
                pilled = true;
                EstrellaTouch = false;
                EstrellaTime = TimeUtils.nanoTime() + 10000000000L;
                estrella.remove();

                // Blink the player's image while "pilled"
                final float blinkDuration = 0.2f;
                final float blinkInterval = 0.1f;
                final boolean[] isPilled = { true };
                Timer.schedule(new Timer.Task() {
                    int count = 0;
                    boolean visible = true;
                    @Override
                    public void run() {
                        visible = !visible;
                        player.setVisible(visible);
                        count++;

                        if (isPilled[0] && TimeUtils.nanoTime() < EstrellaTime) {
                            if (count >= (blinkDuration / blinkInterval)) {
                                count = 0;
                            }
                        } else {
                            player.setPilled(false);
                            player.setVisible(true);
                            isPilled[0] = false;
                            this.cancel();
                        }
                    }
                }, 0, blinkInterval);
            }
        }

        if (MonedaTouch) {
            if (moneda.getBounds().overlaps(player.getBounds())) {
                drunk = true;
                score += MonedaSuma;
                MonedaTouch = false;
                MonedaTime = TimeUtils.nanoTime() + 5000000000L;
                moneda.remove();
                player.setSpeedx(player.getSpeedx() *- 124.5f);
                player.setDrunk(true);
                player.remove();
                stage.addActor(player);
                for (Pipe pipe : obstacles) {
                    pipe.setSpeed(pipe.getSpeed() * 4);
                }
            }
        }



        if (TimeUtils.nanoTime() - lastObstacleTime > 1500000000){
            spawnObstacle();
            if (!drunk){
                LastMoneda++;
            }
            if(!pilled){
                lastEstrella++;
            }
        }

        Iterator<Pipe> iter = obstacles.iterator();
        while (iter.hasNext()) {
            Pipe pipe = iter.next();
            if (pipe.getBounds().overlaps(player.getBounds()) && !pilled) {
                dead = true;
            }
        }


        iter = obstacles.iterator();
        while (iter.hasNext()) {
            Pipe pipe = iter.next();
            if (pipe.getX() < -64) {
                obstacles.removeValue(pipe, true);
            }
        }

        game.batch.begin();
        game.smallFont.draw(game.batch, "Score: " + (int)score, 10, 470);
        game.batch.end();

        score += Gdx.graphics.getDeltaTime();

        if(dead) {
            game.manager.get("fail.wav", Sound.class).play();
            game.lastScore = (int)score;
            if(game.lastScore > game.topScore){
                game.topScore = game.lastScore;
            }
            game.setScreen(new GameOverScreen(game));
            dispose();
        }
    }

    private void spawnObstacle() {
        // Calcula la alçada de l'obstacle aleatòriament
        float holey = MathUtils.random(50, 230);
        // Crea dos obstacles: Una tubería superior i una inferior
        Pipe pipe1 = new Pipe();
        pipe1.setX(800);
        pipe1.setY(holey - 230);
        pipe1.setUpsideDown(true);
        pipe1.setManager(game.manager);
        obstacles.add(pipe1);
        stage.addActor(pipe1);
        Pipe pipe2 = new Pipe();
        pipe2.setX(800);
        pipe2.setY(holey + 200);
        pipe2.setUpsideDown(false);
        pipe2.setManager(game.manager);
        if (drunk) {
            pipe1.setSpeed(pipe1.getSpeed() * 4);
            pipe2.setSpeed(pipe2.getSpeed() * 4);
        }
        obstacles.add(pipe2);
        stage.addActor(pipe2);
        lastObstacleTime = TimeUtils.nanoTime();
    }

    private void spawnMoneda() {
        float holey = MathUtils.random(50, 230);
        Moneda moneda = new Moneda();
        moneda.setX(800);
        moneda.setY(holey);
        moneda.setAssetManager(game.manager);
        stage.addActor(moneda);
        this.moneda = moneda;
    }

    private void spawnEstrella() {
        float holey = MathUtils.random(50, 230);
        Estrella lorazepam = new Estrella();
        lorazepam.setX(800);
        lorazepam.setY(holey);
        lorazepam.setAssetManager(game.manager);
        stage.addActor(lorazepam);
        this.estrella = lorazepam;
    }


    @Override
    public void resize(int width, int height) {
    }
    @Override
    public void show() {
    }
    @Override
    public void hide() {
    }
    @Override
    public void pause() {
    }
    @Override
    public void resume() {
    }
    @Override
    public void dispose() {

        justBurningMemory.stop();
        justBurningMemory.dispose();
    }
}