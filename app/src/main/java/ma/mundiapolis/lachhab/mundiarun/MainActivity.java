package ma.mundiapolis.lachhab.mundiarun;

/**
 * Created by Asus Zenbook on 11/12/2017.
 */


import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetFileDescriptor;
import android.graphics.RectF;
import android.graphics.drawable.AnimationDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import android.widget.ToggleButton;


public class MainActivity extends AppCompatActivity {
    Handler h = new Handler();
    SharedPreferences sp;
    Editor ed;
    boolean isForeground = true;
    MediaPlayer mp;
    SoundPool sndpool;
    int snd_result;
    int snd_star;
    int snd_energy;
    int snd_die;
    int snd_jump;
    int score;
    int screen_width;
    int screen_height;
    int current_section = R.id.main;
    boolean first_ground;
    List<View> grounds = new ArrayList<View>();
    List<View> stars = new ArrayList<View>();
    boolean game_paused;
    AnimationDrawable anim_hero;
    float speed;
    float jump_limit;
    float ground_x;
    float star_x;
    int num_stars;
    boolean on_ground;
    final float min_speed = 4f; // hero start speed
    final float max_speed = 10f; // hero max speed
    final float run_acceleration = 0.005f; // hero run acceleration
    final float jump_acceleration = 7f; // hero jump acceleration
    final float jump_power = 110f; // hero jump power
    final float slow_down = 1f; // slow down when hit with energy
    final float grounds_interval = 100f; // interval between grounds ( problem here !!!)
    final float gravity = 6f; // gravity


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(savedInstanceState != null){}

        // fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // preferences
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        ed = sp.edit();
        // bg sound
        mp = new MediaPlayer();
        try {
            AssetFileDescriptor descriptor = getAssets().openFd("snd_bg.mp3");
            mp.setDataSource(descriptor.getFileDescriptor(), descriptor.getStartOffset(), descriptor.getLength());
            descriptor.close();
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mp.setLooping(true);
            mp.setVolume(0, 0);
            mp.prepare();
            mp.start();
        } catch (Exception e) {}

        // if mute
        if (sp.getBoolean("mute", false)) {
            ((Button) findViewById(R.id.btn_sound)).setText(getString(R.string.btn_sound));
        } else {
            mp.setVolume(0.2f, 0.2f);
        }

        // Sound
            sndpool = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
        try {
            snd_result = sndpool.load(getAssets().openFd("snd_result.mp3"), 1);
            snd_star = sndpool.load(getAssets().openFd("snd_star.mp3"), 1);
            snd_energy = sndpool.load(getAssets().openFd("snd_energy.mp3"), 1);
            snd_jump = sndpool.load(getAssets().openFd("snd_jump.mp3"), 1);
            snd_die = sndpool.load(getAssets().openFd("snd_die.mp3"), 1);
        } catch (IOException e) {}

        // add stars
        for (int i = 0; i < 15; i++) {
            ImageView star = new ImageView(this);
            ((ViewGroup) findViewById(R.id.game)).addView(star);
            star.getLayoutParams().width = (int) DpToPx(16);
            star.getLayoutParams().height = (int) DpToPx(16);
            stars.add(star);
        }

        // add grounds
        for (int i = 0; i < 5; i++) {
            ImageView ground = new ImageView(this);
            ground.setImageResource(R.drawable.ground);
            ground.setScaleType(ScaleType.CENTER_CROP);
            ((ViewGroup) findViewById(R.id.game)).addView(ground);
            ground.getLayoutParams().height = (int) DpToPx(410);
            grounds.add(ground);
        }

        // animation hero
        anim_hero = (AnimationDrawable) ((ImageView) findViewById(R.id.hero)).getDrawable();

        // index
        findViewById(R.id.hero).bringToFront();
        findViewById(R.id.txt_score).bringToFront();
        findViewById(R.id.btn_play).bringToFront();
        findViewById(R.id.mess).bringToFront();


        // jump touch listener
        findViewById(R.id.game).setOnTouchListener(new OnTouchListener() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // jump
                if (current_section == R.id.game && on_ground && !game_paused && event.getAction() == MotionEvent.ACTION_DOWN) {
                    on_ground = false;
                    jump_limit = findViewById(R.id.hero).getY() - DpToPx(jump_power);
                    findViewById(R.id.hero).setRotation(1);

                    // sound
                    if (!sp.getBoolean("mute", false) && isForeground)
                        sndpool.play(snd_jump, 0.1f, 0.1f, 0, 0, 1);
                }
                return false;
            }
        });

        SCALE();

    }

    // SCALE ÉCHELLE
    void SCALE()
    {
        // buttons text
        ((TextView) findViewById(R.id.btn_sound)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(22));
        ((TextView) findViewById(R.id.btn_start)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(30));
        ((TextView) findViewById(R.id.btn_exit)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(22));
        ((TextView) findViewById(R.id.btn_home)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(24));
        ((TextView) findViewById(R.id.btn_start2)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(24));

        // text result
        ((TextView) findViewById(R.id.txt_result)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(60));
        ((TextView) findViewById(R.id.txt_high_result)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(30));

        // text mess
        ((TextView) findViewById(R.id.mess)).setTextSize(TypedValue.COMPLEX_UNIT_PX, DpToPx(30));
    }

    void START() {

        show_section(R.id.game);
        score = 0;
        speed = DpToPx(min_speed);
        findViewById(R.id.mess).setVisibility(View.GONE);
        game_paused = false;
        on_ground = true;
        num_stars = 0;
        ((TextView) findViewById(R.id.txt_score)).setText(getString(R.string.score) + " " + score);
        findViewById(R.id.btn_play).setVisibility(View.VISIBLE);
        findViewById(R.id.hero).setEnabled(true);
        findViewById(R.id.hero).setRotation(0);
        ((ToggleButton) findViewById(R.id.btn_play)).setChecked(true);
        anim_hero.stop();
        anim_hero.start();

        // screen size
        screen_width = Math.max(findViewById(R.id.all).getWidth(), findViewById(R.id.all).getHeight());
        screen_height = Math.min(findViewById(R.id.all).getWidth(), findViewById(R.id.all).getHeight());

        // random stars
        star_x = 0;
        for (int i = 0; i < stars.size(); i++) {
            random_star(i);
        }

        // random grounds
        ground_x = 0;
        first_ground = true;
        for (int i = 0; i < grounds.size(); i++) {
            random_ground(i);
        }

        // random cloud
        random_cloud();

        // hero start position
        findViewById(R.id.hero).setX(DpToPx(100));
        findViewById(R.id.hero).setScaleX(-1);
        findViewById(R.id.hero).setY(grounds.get(0).getY() - findViewById(R.id.hero).getHeight());

        MOVE.run();
    }

    // random_ground
    void random_ground(int i) {
        grounds.get(i).setX(ground_x);

        if (first_ground) {
            grounds.get(i).getLayoutParams().width = (int) DpToPx(600);
            ground_x += ((int) DpToPx(600) + DpToPx(grounds_interval));
        } else {
            switch ((int) Math.round(Math.random() * 10)) {
                case 0:
                    grounds.get(i).getLayoutParams().width = (int) DpToPx(200);
                    ground_x += ((int) DpToPx(200) + DpToPx(grounds_interval));
                    break;
                case 1:
                    grounds.get(i).getLayoutParams().width = (int) DpToPx(300);
                    ground_x += ((int) DpToPx(300) + DpToPx(grounds_interval));
                    break;
                case 2:
                    grounds.get(i).getLayoutParams().width = (int) DpToPx(400);
                    ground_x += ((int) DpToPx(400) + DpToPx(grounds_interval));
                    break;
                case 3:
                    grounds.get(i).getLayoutParams().width = (int) DpToPx(500);
                    ground_x += ((int) DpToPx(500) + DpToPx(grounds_interval));
                    break;
                case 4:
                    grounds.get(i).getLayoutParams().width = (int) DpToPx(600);
                    ground_x += ((int) DpToPx(600) + DpToPx(grounds_interval));
                    break;
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                case 10:
                    grounds.get(i).getLayoutParams().width = (int) DpToPx(200);
                    ground_x += ((int) DpToPx(200) + DpToPx(grounds_interval));
                    break;
            }
        }
        if (Math.random() > 0.5)
            grounds.get(i).setY(screen_height * 0.5f);
        else
            grounds.get(i).setY(screen_height * 0.5f + findViewById(R.id.hero).getHeight());
        first_ground = false;
    }

    // random_star
    void random_star(int i) {
        num_stars++;

        // star or energy
        if (num_stars == 10) {
            num_stars = 0;
            stars.get(i).setBackgroundResource(R.drawable.energy);
            stars.get(i).setTag(1);
        } else {
            stars.get(i).setBackgroundResource(R.drawable.star);
            stars.get(i).setTag(0);
        }
        stars.get(i).setX(star_x);
        star_x += (stars.get(i).getWidth() * 4);
        stars.get(i).setY((float) (screen_height * 0.5f - findViewById(R.id.hero).getHeight() - stars.get(i).getHeight() - Math.random() * findViewById(R.id.hero).getHeight()));
        stars.get(i).setRotation((float) (Math.random() * 360));
        stars.get(i).setAlpha(1);
        stars.get(i).setScaleX(1);
        stars.get(i).setScaleY(1);
        stars.get(i).setEnabled(true);
    }
    // random_cloud
    void random_cloud() {
        ((ImageView) findViewById(R.id.cloud)).setImageResource(getResources().getIdentifier("cloud" + Math.round(Math.random() * 2), "drawable", getPackageName()));
        findViewById(R.id.cloud).setX(screen_width);
        findViewById(R.id.cloud).setY((float) (Math.random() * (screen_height * 0.25f)));
    }

    // MOVE
    Runnable MOVE = new Runnable() {
        @Override
        public void run() {
            if (!game_paused) {
                // hero speed
                speed = Math.min(speed + DpToPx(run_acceleration), DpToPx(max_speed)); // get more speed
                ground_x -= speed;
                star_x -= speed;

                // jump
                if (!on_ground && findViewById(R.id.game).isPressed()) {
                    if (findViewById(R.id.hero).getY() > jump_limit)
                        findViewById(R.id.hero).setY(findViewById(R.id.hero).getY() - DpToPx(jump_acceleration));
                    else {
                        jump_limit = screen_height * 2;
                        findViewById(R.id.hero).setY(findViewById(R.id.hero).getY() + DpToPx(gravity));
                    }
                } else {
                    jump_limit = screen_height * 2;
                    findViewById(R.id.hero).setY(findViewById(R.id.hero).getY() + DpToPx(gravity));
                }

                // hero rotation
             /* if (findViewById(R.id.hero).getRotation() > 0)
                    findViewById(R.id.hero).setRotation(Math.min(360, findViewById(R.id.hero).getRotation() + 20));*/

                // hero hit rectangle
                RectF hero_rect = new RectF(findViewById(R.id.hero).getX(), findViewById(R.id.hero).getY(), findViewById(
                        R.id.hero).getX()
                        + findViewById(R.id.hero).getWidth(), findViewById(R.id.hero).getY()
                        + findViewById(R.id.hero).getHeight());

                // move cloud
                findViewById(R.id.cloud).setX(findViewById(R.id.cloud).getX() - (float) (speed * 0.5f) - DpToPx(0.3f));

                // restart cloud
                if (findViewById(R.id.cloud).getX() < -findViewById(R.id.cloud).getWidth())
                    random_cloud();

                // move grounds
                for (int i = 0; i < grounds.size(); i++) {
                    grounds.get(i).setX(grounds.get(i).getX() - speed);

                    // restart ground
                    if (grounds.get(i).getX() < -grounds.get(i).getWidth())
                        random_ground(i);
                }

                //  moove stars(M (Mundia))
                for (int i = 0; i < stars.size(); i++) {
                    stars.get(i).setX(stars.get(i).getX() - speed);
                    stars.get(i).setRotation(stars.get(i).getRotation() - 5);

                    // after hit with hero
                    if (!stars.get(i).isEnabled()) {
                        stars.get(i).setY(stars.get(i).getY() - DpToPx(5));
                        stars.get(i).setScaleX(stars.get(i).getScaleX() + 0.05f);
                        stars.get(i).setScaleY(stars.get(i).getScaleY() + 0.05f);
                        stars.get(i).setAlpha(stars.get(i).getAlpha() - 0.05f);
                    }

                    // restart star
                    if (stars.get(i).getX() < -stars.get(i).getWidth())
                        random_star(i);

                    // hit hero with star or energy
                    if (stars.get(i).isEnabled()
                            && hero_rect.intersect(new RectF(stars.get(i).getX() + stars.get(i).getWidth() * 0.5f, stars.get(i)
                            .getY() + stars.get(i).getHeight() * 0.5f, stars.get(i).getX() + stars.get(i).getWidth()
                            * 0.5f, stars.get(i).getY() + stars.get(i).getHeight() * 0.5f))) {
                        stars.get(i).setEnabled(false);

                        // star
                        if (Integer.valueOf(stars.get(i).getTag().toString()) == 0) {
                            // score
                            score += 5;
                            ((TextView) findViewById(R.id.txt_score)).setText(getString(R.string.score) + " " + score);

                            // sound
                            if (!sp.getBoolean("mute", false) && isForeground)
                                sndpool.play(snd_star, 0.2f, 0.2f, 0, 0, 1);
                        } else {
                            speed = Math.max(DpToPx(min_speed), speed - DpToPx(slow_down));

                            // sound
                            if (!sp.getBoolean("mute", false) && isForeground)
                                sndpool.play(snd_energy, 0.5f, 0.5f, 0, 0, 1);
                        }
                    }
                }

                on_ground = false;
                if (findViewById(R.id.hero).isEnabled()) {
                    // hit hero with ground
                    for (int i = 0; i < grounds.size(); i++) {
                        if (hero_rect.intersect(new RectF(grounds.get(i).getX(), grounds.get(i).getY(), grounds.get(i).getX()
                                + grounds.get(i).getWidth(), grounds.get(i).getY() + grounds.get(i).getHeight()))
                                && findViewById(R.id.hero).getY() + findViewById(R.id.hero).getHeight() <= grounds.get(i).getY()
                                + DpToPx(gravity * 2)) {
                            on_ground = true;
                            findViewById(R.id.hero).setY(grounds.get(i).getY() - findViewById(R.id.hero).getHeight());
                            break;
                        }
                    }

                    // hero die
                    if (findViewById(R.id.hero).getY() > screen_height) {
                        findViewById(R.id.hero).setEnabled(false);
                        findViewById(R.id.btn_play).setVisibility(View.GONE);

                        // message
                        findViewById(R.id.mess).setVisibility(View.VISIBLE);

                        // sound
                        if (!sp.getBoolean("mute", false) && isForeground)
                            sndpool.play(snd_die, 0.5f, 0.5f, 0, 0, 1);

                        h.postDelayed(STOP, 3000);
                    }
                }

                // slow down red flesch
                if (!findViewById(R.id.hero).isEnabled())
                    speed *= 0.95;
            }

            h.postDelayed(MOVE, 10);
        }
    };

    // STOP
    Runnable STOP = new Runnable() {
        @Override
        public void run() {
            // show result
            show_section(R.id.result);
            h.removeCallbacks(MOVE);

            // save score
            if (score > sp.getInt("score", 0)) {
                ed.putInt("score", score);
                ed.commit();
            }

            // show score
            ((TextView) findViewById(R.id.txt_result)).setText(getString(R.string.score) + " " + score);
            ((TextView) findViewById(R.id.txt_high_result)).setText(getString(R.string.high_score) + " " + sp.getInt("score", 0));

            // sound
            if (!sp.getBoolean("mute", false) && isForeground)
                sndpool.play(snd_result, 1f, 1f, 0, 0, 1);

        }
    };

    // onClick xml
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start:
            case R.id.btn_start2:
                START();
                break;
            case R.id.btn_home:
                show_section(R.id.main);
                break;
            case R.id.btn_exit:
                finish();
                break;
            case R.id.btn_sound:
                if (sp.getBoolean("mute", false)) {
                    ed.putBoolean("mute", false);
                    mp.setVolume(0.2f, 0.2f);
                    ((Button) findViewById(R.id.btn_sound)).setText(getString(R.string.btn_mute));
                } else {
                    ed.putBoolean("mute", true);
                    mp.setVolume(0, 0);
                    ((Button) findViewById(R.id.btn_sound)).setText(getString(R.string.btn_sound));
                }
                ed.commit();
                break;
            case R.id.btn_play:
                if (((ToggleButton) v).isChecked()) {
                    game_paused = false;
                    anim_hero.start();
                } else {
                    game_paused = true;
                    anim_hero.stop();
                }
                break;

        }
    }

    @Override
    public void onBackPressed() {
        switch (current_section) {
            case R.id.main:
                super.onBackPressed();
                break;
            case R.id.result:
                show_section(R.id.main);
                break;
            case R.id.game:
                show_section(R.id.main);
                h.removeCallbacks(MOVE);
                h.removeCallbacks(STOP);
                break;
        }
    }

    // show section
    void show_section(int section) {
        current_section = section;
        findViewById(R.id.main).setVisibility(View.GONE);
        findViewById(R.id.game).setVisibility(View.GONE);
        findViewById(R.id.result).setVisibility(View.GONE);
        findViewById(current_section).setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        h.removeCallbacks(MOVE);
        h.removeCallbacks(STOP);
        mp.release();
        sndpool.release();
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        isForeground = false;
        mp.setVolume(0, 0);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        isForeground = true;
        if (!sp.getBoolean("mute", false) && isForeground)
            mp.setVolume(0.2f, 0.2f);
    }

    // DpToPx
    float DpToPx(float dp) {
        return (dp * Math.max(getResources().getDisplayMetrics().widthPixels, getResources().getDisplayMetrics().heightPixels) / 540f);
    }
    
}