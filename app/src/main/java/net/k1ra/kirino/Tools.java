package net.k1ra.kirino;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.security.KeyPairGeneratorSpec;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spannable;
import android.text.Spanned;
import android.util.Base64;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.DatePicker;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.security.auth.x500.X500Principal;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import static android.content.Context.NOTIFICATION_SERVICE;

public abstract class Tools {

    static Picasso picasso;

    static void update_overrides(final Context c)
    {
        final NetworkResponse out = new NetworkResponse();
        NetworkRequest.make(out, NetworkRequest.kirino_update, false, false, null, c, new Runnable() {
            @Override
            public void run() {
                try {
                    SharedPreferences.Editor e = c.getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).edit();

                    e.putString("CR_a_t", out.obj.getString("CR_code"));

                    JSONArray fanart = out.obj.getJSONArray("fanart_override");
                    for (int i = 0; i < fanart.length(); i++)
                    {
                        e.putString("w-"+fanart.getJSONObject(i).getString("ALid"),fanart.getJSONObject(i).getString("tag"));
                    }

                    JSONArray media = out.obj.getJSONArray("media_override");
                    for (int i = 0; i < media.length(); i++)
                    {
                        e.putString("m-"+media.getJSONObject(i).getString("ALid"),media.getJSONObject(i).getString("id"));
                    }

                    e.commit();
                } catch (JSONException e) {e.printStackTrace();}
            }
        }, null);
    }

    static void fanart_storeall_fetch(final int total_pages_f, final int posts_per_page, final String tags, final Context c,  final List<FeedItem> posts, final int[] post_ids)
    {
        final boolean[] complete = new boolean[total_pages_f];
        final List<NodeList> all_pages = new ArrayList<NodeList>();
        for (int i = 0; i < complete.length; i++)
        {
            final int i_f = i;
            final NetworkResponse out = new NetworkResponse();
            NetworkRequest.make(out, NetworkRequest.Gelbooru.noStrict(), false, false, new Pair[]{
                    Pair.create("limit", String.valueOf(posts_per_page)),
                    Pair.create("pid", String.valueOf(i)),
                    Pair.create("page", "dapi"),
                    Pair.create("s", "post"),
                    Pair.create("q", "index"),
                    Pair.create("tags", tags)
            }, c, new Runnable() {
                @Override
                public void run() {
                    try {
                        Document output = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new ByteArrayInputStream(out.str.getBytes("utf-8"))));
                        output.getDocumentElement().normalize();
                        all_pages.add(output.getElementsByTagName("post"));
                        complete[i_f] = true;

                        boolean done = true;
                        for (int i = 0; i < complete.length; i++) {
                            done = done && complete[i];
                        }
                        if (done) {
                           for (int j = 0; j < post_ids.length; j++)
                           {
                               boolean unique = false;
                               Element post = null;

                               int unique_index = 0;
                               while (!unique) {
                                   try {
                                       final int page = new Random().nextInt(total_pages_f);
                                   post = (Element) all_pages.get(page).item(new Random().nextInt(all_pages.get(page).getLength()));
                                   int post_id = Integer.parseInt(post.getAttribute("id"));
                                   System.out.println("TOTAL PAGES: "+total_pages_f+" CURRENT PAGE: "+page+" PAGE LENGTH: "+all_pages.get(page).getLength()+" ID: "+post_id);
                                   boolean post_id_no_match = true;
                                   for (int i = 0; i < post_ids.length; i++) { post_id_no_match = post_id != post_ids[i]; if (!post_id_no_match) {break;} }
                                   post_ids[j] = post_id;
                                   unique = unique_index > 100 || post_id_no_match;
                                   unique_index++;
                                   } catch (Exception e) {
                                        Errors.toast(c, c.getText(R.string.gel_error));
                                        break;
                                   }
                               }
                               if (post != null) {posts.add(new FeedItem(0, post.getAttribute("file_url")));}
                           }

                            //insert
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    Feed_DB DB = Room.databaseBuilder(c,
                                            Feed_DB.class, "feed_db")
                                            .build();
                                    DB.DAO().insert_list(posts);
                                    DB.close();
                                    Intent intent = new Intent("net.k1ra.update.feed_add");
                                    intent.putExtra("type", 0);
                                    c.sendBroadcast(intent);
                                    Loading.dismiss();
                                }
                            }).start();
                        }

                    } catch (ParserConfigurationException | SAXException | IOException e) {
                        Errors.no_internet_toast(c);
                    }
                }
            }, null);
        }
    }

    //AL will still return some HTML in descriptions. Here we'll parse it and do some parsing for spoilers
    static Spanned parse_description(String in, boolean spoil)
    {
        if (spoil) {
            in = in.replace("~!", "").replace("!~", "");
        } else {
            in = in.replace("~!", "<span style=\"background-color: #ffffff\">").replace("!~", "</span>");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return  Html.fromHtml(in, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return  Html.fromHtml(in);
        }
    }

    static boolean reached_end = false;
    static int i = 0;
    static void fetch_AL_lists(final Context c)
    {
        new Thread(new Runnable() {
            @Override
            public void run() {

        final List<Media> anime = new ArrayList<Media>();
        final List<Media> manga = new ArrayList<Media>();
        final List<Character> waifu = new ArrayList<Character>();
        final SharedPreferences sharedPref = c.getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE);
        i = 1;
        reached_end = false;

        if (sharedPref.getBoolean("sync_a", true) || sharedPref.getBoolean("sync_m", true) || sharedPref.getBoolean("rem", true))
        {
            //since this is a background thread and we have time, all these network requests are executed sequentially on this thread (as opposed to spawning a new thread)
            while (!reached_end) {
                final NetworkResponse out = new NetworkResponse();
                NetworkRequest.make(out, NetworkRequest.AniList.Authenticated().noStrict(), true, false, NetworkRequest.make_AL_query(5, "", i, c), c, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            reached_end = !out.obj.getJSONObject("data").getJSONObject("Page").getJSONObject("pageInfo").getBoolean("hasNextPage");
                            JSONArray items = out.obj.getJSONObject("data").getJSONObject("Page").getJSONArray("mediaList");
                            for (int j = 0; j < items.length(); j++) {
                                String airString = "";
                                if (!items.getJSONObject(j).getJSONObject("media").getJSONObject("startDate").getString("day").equals("null")) {
                                    airString += items.getJSONObject(j).getJSONObject("media").getJSONObject("startDate").getString("day") + "/";
                                }
                                if (!items.getJSONObject(j).getJSONObject("media").getJSONObject("startDate").getString("month").equals("null")) {
                                    airString += items.getJSONObject(j).getJSONObject("media").getJSONObject("startDate").getString("month") + "/";
                                }
                                if (!items.getJSONObject(j).getJSONObject("media").getJSONObject("startDate").getString("year").equals("null")) {
                                    airString += items.getJSONObject(j).getJSONObject("media").getJSONObject("startDate").getString("year");
                                }
                                Media to_add = new Media(items.getJSONObject(j).getJSONObject("media").getString("id"),
                                        items.getJSONObject(j).getJSONObject("media").getJSONObject("coverImage").getString("large"),
                                        items.getJSONObject(j).getJSONObject("media").getJSONObject("title").getString("romaji"),
                                        items.getJSONObject(j).getJSONObject("media").getString("type"),
                                        items.getJSONObject(j).getJSONObject("media").getString("format"),
                                        items.getJSONObject(j).getJSONObject("media").getString("averageScore"),
                                        items.getJSONObject(j).getJSONObject("media").getString("episodes"),
                                        items.getJSONObject(j).getJSONObject("media").getString("description"),
                                        airString);
                                to_add.current = items.getJSONObject(j).getJSONObject("media").getString("status").equals("RELEASING");
                                to_add.has_started_airing = !items.getJSONObject(j).getJSONObject("media").getString("status").equals("NOT_YET_RELEASED");
                                if (to_add.current) {
                                    try {
                                        to_add.new_ep_day = items.getJSONObject(j).getJSONObject("media").getJSONObject("nextAiringEpisode").getLong("airingAt");
                                    } catch (JSONException e) {
                                    }
                                }

                                if (to_add.numEpisodes.equals("null")) { to_add.numEpisodes = items.getJSONObject(j).getJSONObject("media").getString("chapters"); }

                                //check for new EP and send notification
                                if (sharedPref.getBoolean("rem", true))
                                { //we only check MastAni.me for new episodes
                                    if (to_add.type.equals("ANIME")) {
                                        check_for_new_episode(c, to_add, sharedPref);
                                    } else {
                                        check_for_new_chapter(c, to_add, sharedPref);
                                    }
                                }

                                JSONArray links = items.getJSONObject(j).getJSONObject("media").getJSONArray("externalLinks");
                                for (int i = 0; i < links.length(); i++) {
                                    if (links.getJSONObject(i).getString("site").equals("Crunchyroll")) {
                                        to_add.streamType = 1;
                                        break;
                                    }
                                }

                                //catch all the things just in case, they can be "null"
                                try {
                                    to_add.p_score = (float) items.getJSONObject(j).getDouble("score");
                                } catch (JSONException e) {
                                }

                                try {
                                    to_add.progress = items.getJSONObject(j).getInt("progress");
                                } catch (JSONException e) {
                                }

                                try {
                                    to_add.progress_volumes = items.getJSONObject(j).getInt("progressVolumes");
                                } catch (JSONException e) {
                                }

                                try {
                                    to_add.repeat = items.getJSONObject(j).getInt("repeat");
                                } catch (JSONException e) {
                                }

                                try {
                                    to_add.started_year = items.getJSONObject(j).getJSONObject("startedAt").getInt("year");
                                } catch (JSONException e) {
                                }

                                try {
                                    to_add.started_month = items.getJSONObject(j).getJSONObject("startedAt").getInt("month");
                                } catch (JSONException e) {
                                }

                                try {
                                    to_add.started_day = items.getJSONObject(j).getJSONObject("startedAt").getInt("day");
                                } catch (JSONException e) {
                                }

                                try {
                                    to_add.ended_year = items.getJSONObject(j).getJSONObject("completedAt").getInt("year");
                                } catch (JSONException e) {
                                }

                                try {
                                    to_add.ended_month = items.getJSONObject(j).getJSONObject("completedAt").getInt("month");
                                } catch (JSONException e) {
                                }

                                try {
                                    to_add.ended_day = items.getJSONObject(j).getJSONObject("completedAt").getInt("day");
                                } catch (JSONException e) {
                                }

                                if (to_add.type.equals("ANIME") && sharedPref.getBoolean("sync_a", true)) {
                                    anime.add(to_add);
                                } else {
                                    manga.add(to_add);
                                }
                            }

                            i++;
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Runnable() {
                    @Override
                    public void run() {
                        SystemClock.sleep(2000);
                    }
                }, true);

            }

            i = 1;
            reached_end = false;

            //now we fetch waifus
            if (sharedPref.getBoolean("sync_w", true))
            {
                while (!reached_end) {
                    final NetworkResponse out = new NetworkResponse();
                    NetworkRequest.make(out, NetworkRequest.AniList.Authenticated().noStrict(), true, false, NetworkRequest.make_AL_query(10, "", i), c, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONArray waifus = out.obj.getJSONObject("data").getJSONObject("Viewer").getJSONObject("favourites").getJSONObject("characters").getJSONArray("nodes");

                                reached_end = waifus.length() == 0;

                                for (int i = 0; i < waifus.length(); i++) {
                                    String name_text = "";
                                    if (!waifus.getJSONObject(i).getJSONObject("name").getString("first").equals("null")) {
                                        name_text = waifus.getJSONObject(i).getJSONObject("name").getString("first");
                                    }
                                    if (!waifus.getJSONObject(i).getJSONObject("name").getString("last").equals("null")) {
                                        name_text += " " + waifus.getJSONObject(i).getJSONObject("name").getString("last");
                                    }
                                    final Character to_add = new Character(waifus.getJSONObject(i).getString("id"),
                                            waifus.getJSONObject(i).getJSONObject("image").getString("large"),
                                            name_text,
                                            waifus.getJSONObject(i).getString("description"));

                                    try {
                                        to_add.media = waifus.getJSONObject(i).getJSONObject("media").getJSONArray("nodes").toString();
                                    } catch (JSONException e) {
                                    }

                                    waifu.add(to_add);
                                }

                                i++;

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }, new Runnable() {
                        @Override
                        public void run() {
                            SystemClock.sleep(2000);
                        }
                    });
                }
            }

                //now we open the DBs, purge existing records, insert new, broadcast update and close
                if (sharedPref.getBoolean("sync_a", true)) {
                    final Media_DB DB_a = Room.databaseBuilder(c,
                            Media_DB.class, "anime_db").fallbackToDestructiveMigration()
                            .build();
                    DB_a.DAO().nuke();
                    DB_a.DAO().insert_list(anime);
                    DB_a.close();
                    c.sendBroadcast(new Intent("net.k1ra.update.anime"));
                }

                if (sharedPref.getBoolean("sync_m", true)) {
                    final Media_DB DB_m = Room.databaseBuilder(c,
                            Media_DB.class, "manga_db").fallbackToDestructiveMigration()
                            .build();
                    DB_m.DAO().nuke();
                    DB_m.DAO().insert_list(manga);
                    DB_m.close();
                    c.sendBroadcast(new Intent("net.k1ra.update.manga"));
                }

                if (sharedPref.getBoolean("sync_w", true)) {
                    final character_DB DB_w = Room.databaseBuilder(c,
                            character_DB.class, "waifu_db").fallbackToDestructiveMigration()
                            .build();
                    DB_w.DAO().nuke();
                    DB_w.DAO().insert_list(waifu);
                    DB_w.close();
                    c.sendBroadcast(new Intent("net.k1ra.update.waifu"));
                }
        }
        }
        }).start();
    }

    static void update_lists_no_AL(final Context c)
    {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final SharedPreferences sharedPref = c.getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE);
                //first update anime, then manga
                final Media_DB DB_a = Room.databaseBuilder(c,
                        Media_DB.class, "anime_db").fallbackToDestructiveMigration()
                        .build();
                final Media_DB DB_m = Room.databaseBuilder(c,
                        Media_DB.class, "manga_db").fallbackToDestructiveMigration()
                        .build();
                List<Media> list;
                for (int j = 0; j < 2; j++) {
                    if (j == 0) { list = DB_a.DAO().fetch_all(); } else { list = DB_m.DAO().fetch_all(); }
                    for (int i = 0; i < list.size(); i++) {
                        final Media editing = list.get(i);
                        final NetworkResponse out = new NetworkResponse();
                        NetworkRequest.make(out, NetworkRequest.AniList.notAuthenticated().noStrict(), true, false, NetworkRequest.make_AL_query(2, editing.ALid, 0), c, new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    final JSONObject media = out.obj.getJSONObject("data").getJSONObject("Media");
                                    String airString = "";
                                    if (!media.getJSONObject("startDate").getString("day").equals("null")) {
                                        airString += media.getJSONObject("startDate").getString("day") + "/";
                                    }
                                    if (!media.getJSONObject("startDate").getString("month").equals("null")) {
                                        airString += media.getJSONObject("startDate").getString("month") + "/";
                                    }
                                    if (!media.getJSONObject("startDate").getString("year").equals("null")) {
                                        airString += media.getJSONObject("startDate").getString("year");
                                    }
                                    editing.image_URL = media.getJSONObject("coverImage").getString("large");
                                    editing.name = media.getJSONObject("title").getString("romaji");
                                    editing.type = media.getString("type");
                                    editing.format = media.getString("format");
                                    editing.score = media.getString("averageScore");
                                    editing.numEpisodes = media.getString("episodes");
                                    if (editing.numEpisodes.equals("null")) { editing.numEpisodes = media.getString("chapters"); }
                                    editing.description = media.getString("description");
                                    editing.AirDate = airString;
                                    editing.current = media.getString("status").equals("RELEASING");
                                    editing.has_started_airing = !media.getString("status").equals("NOT_YET_RELEASED");
                                    if (editing.current) {
                                        try {
                                            editing.new_ep_day = media.getJSONObject("nextAiringEpisode").getLong("airingAt");
                                        } catch (JSONException e) { }
                                    }

                                    //check for new EP and send notification
                                    if (sharedPref.getBoolean("rem", true))
                                    { //we only check MastAni.me for new episodes
                                        if (editing.type.equals("ANIME")) {
                                            check_for_new_episode(c, editing, sharedPref);
                                        } else {
                                            check_for_new_chapter(c, editing, sharedPref);
                                        }
                                    }

                                    JSONArray links = media.getJSONArray("externalLinks");
                                    for (int i = 0; i < links.length(); i++) {
                                        if (links.getJSONObject(i).getString("site").equals("Crunchyroll")) {
                                            editing.streamType = 1;
                                            break;
                                        }
                                    }

                                    if (editing.type.equals("ANIME")) { DB_a.DAO().update(editing); } else { DB_m.DAO().update(editing); }
                                } catch (JSONException e) { }
                            }
                        }, null);
                    }
                }

                c.sendBroadcast(new Intent("net.k1ra.update.anime"));
                c.sendBroadcast(new Intent("net.k1ra.update.manga"));

                //now update waifus
                final character_DB DB_w = Room.databaseBuilder(c,
                        character_DB.class, "waifu_db").fallbackToDestructiveMigration()
                        .build();
                List<Character> waifus = DB_w.DAO().fetch_list();
                for (int i = 0; i < waifus.size(); i++)
                {
                    final Character editing = waifus.get(i);
                    final NetworkResponse out = new NetworkResponse();
                    NetworkRequest.make(out, NetworkRequest.AniList.notAuthenticated().noStrict(), true, false, NetworkRequest.make_AL_query(3, editing.ALid, 0), c, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                final JSONObject chara = out.obj.getJSONObject("data").getJSONObject("Character");
                                String name_text = "";
                                if (!chara.getJSONObject("name").getString("first").equals("null")) {
                                    name_text = chara.getJSONObject("name").getString("first");
                                }
                                if (!chara.getJSONObject("name").getString("last").equals("null")) {
                                    name_text += " " + chara.getJSONObject("name").getString("last");
                                }

                                editing.image_URL = chara.getJSONObject("image").getString("large");
                                editing.name = name_text;
                                editing.description = chara.getString("description");

                                try {
                                    editing.media = chara.getJSONObject("media").getJSONArray("nodes").toString();
                                } catch (JSONException e) { }

                                DB_w.DAO().update(editing);

                            } catch (JSONException e) { }
                        }
                    }, null);
                }
                c.sendBroadcast(new Intent("net.k1ra.update.waifu"));
            }
        }).start();
    }

    static void check_for_new_chapter(final Context c, final Media manga, final SharedPreferences sharedPref)
    {
        manga.name = manga.name.replace("☆"," ");
        final NetworkResponse out = new NetworkResponse();
        NetworkRequest.make(out, NetworkRequest.MR_search, true, false, new Pair[]{Pair.create("", "{\"type\":\"series\",\"keywords\":\"" + manga.name + "\"}")}, null, new Runnable() {
            @Override
            public void run() {
                try {
                    final NetworkResponse meta_out = new NetworkResponse();
                    NetworkRequest.make(meta_out, NetworkRequest.MR_meta, true, false, new Pair[]{Pair.create("", out.obj.getJSONArray("data").toString())}, null, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONArray results = out.obj.getJSONArray("data");

                                //use the same name-matching algorithm as used in AL_name_to_gelbooru_tag
                                int[] matching_score = new int[results.length()];

                                for (int j = 0; j < results.length(); j++) {
                                    try {
                                        if (meta_out.obj.getJSONObject("data").getJSONObject(results.getString(j)).getString("name").length() == manga.name.length()) {
                                            matching_score[j] = 1000000;
                                        }
                                        for (int q = 0; q < meta_out.obj.getJSONObject("data").getJSONObject(results.getString(j)).getString("name").length(); q++) {
                                            if (meta_out.obj.getJSONObject("data").getJSONObject(results.getString(j)).getString("name").toCharArray()[q] == manga.name.toCharArray()[q]) {
                                                matching_score[j]++;
                                            }
                                        }
                                    } catch (ArrayIndexOutOfBoundsException e) {
                                    }
                                }
                                int max_index = 0;
                                for (int j = 0; j < matching_score.length; j++) {
                                    if (matching_score[max_index] < matching_score[j]) {
                                        max_index = j;
                                    }
                                }

                                final NetworkResponse chap_out = new NetworkResponse();
                                NetworkRequest.make(chap_out, NetworkRequest.MR_info, false, false, new Pair[]{Pair.create("oid", out.obj.getJSONArray("data").getString(max_index)), Pair.create("last", "0"), Pair.create("country", "Canada")}, null, new Runnable() {
                                    @Override
                                    public void run() {
                                        try {

                                            int count = chap_out.obj.getJSONObject("data").getJSONArray("chapters").length();
                                            if (sharedPref.getInt(manga.ALid+"-COUNT", -1) < count)
                                            {
                                                if (sharedPref.getInt(manga.ALid+"-COUNT", -1) != -1) {SendNotification(c, manga);}
                                                sharedPref.edit().putInt(manga.ALid+"-COUNT", count).commit();
                                            }

                                        } catch (JSONException e) { }
                                    }
                                }, null);
                            } catch (JSONException e) {
                            }
                        }
                    }, null);
                } catch (JSONException e) {
                }
            }
        }, null);
    }

    static void check_for_new_episode(final Context c, final Media media, final SharedPreferences sharedPref)
    {//MA does not accept searches greater than 30 characters
        String search;
        if (media.name.length() > 30) {
            search = media.name.substring(0, 30).replace("☆"," ");
        } else {
            search = media.name.replace("☆"," ");
        }

        //first search
        final NetworkResponse out = new NetworkResponse();
        NetworkRequest.make(out, NetworkRequest.MA_search, false, false, new Pair[]{Pair.create("search", search)}, null, new Runnable() {
            @Override
            public void run() {
                try {
                    JSONArray results = new JSONArray(out.str);

                    //use the same name-matching algorithm as used in AL_name_to_gelbooru_tag
                    int[] matching_score = new int[results.length()];

                    for (int j = 0; j < results.length(); j++) {
                        try {
                            for (int q = 0; q < results.getJSONObject(j).getString("title").length(); q++) {
                                if (results.getJSONObject(j).getString("title").toCharArray()[q] == media.name.toCharArray()[q]) {
                                    matching_score[j]++;
                                }
                            }
                        } catch (ArrayIndexOutOfBoundsException e) { }
                    }
                    int max_index = 0;
                    for (int j = 0; j < matching_score.length; j++) {
                        if (matching_score[max_index] < matching_score[j]) {
                            max_index = j;
                        }
                    }

                    String id = results.getJSONObject(max_index).getString("id");
                    if (!sharedPref.getString("m-"+media.ALid,"NULL").equals("NULL"))
                    {
                        id = sharedPref.getString("m-"+media.ALid,"NULL");
                    }

                    //then
                    final NetworkResponse EP_out = new NetworkResponse();
                    NetworkRequest.make(EP_out, NetworkRequest.MA_details, false, false, new Pair[]{Pair.create("",id)}, null, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                int count = EP_out.obj.getJSONArray("episodes").length();
                                if (sharedPref.getInt(media.ALid+"-COUNT", -1) < count)
                                {
                                    if (sharedPref.getInt(media.ALid+"-COUNT", -1) != -1) {SendNotification(c, media);}
                                    sharedPref.edit().putInt(media.ALid+"-COUNT", count).commit();
                                }
                          } catch (JSONException e) {}
                        }
                    }, null);
                } catch (JSONException e) {}
            }
        }, null);
    }

    static void SendNotification(final Context c, final Media to_add)
    {
        int NotID = 78961769;

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(c)
                        .setSmallIcon(R.drawable.exo_controls_play)
                        .setContentTitle(to_add.name);

        if (to_add.type.equals("ANIME")) {
            mBuilder.setContentText(c.getString(R.string.ep_released));
        } else {
            mBuilder.setContentText(c.getString(R.string.chap_released));
        }

        try {
            mBuilder.setLargeIcon(Picasso.get().load(to_add.image_URL).get());
        } catch (IOException e) {
        }

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        mBuilder.setSound(alarmSound);
        mBuilder.setVibrate(new long[]{1000, 1000, 1000, 1000, 1000});

        //use primary color for LED if sufficient API version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mBuilder.setLights(c.getColor(R.color.colorPrimary), 3000, 3000);
        } else //otherwise, LED is red
        {
            mBuilder.setLights(Color.rgb(255, 0, 0), 3000, 3000);
        }


        Intent resultIntent = new Intent(c, act_main.class);
        resultIntent.putExtra("sec", 1);
        resultIntent.putExtra("notification_id", NotID + i);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        c,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        mBuilder.setContentIntent(resultPendingIntent);


        // Sets an ID for the notification
        int mNotificationId = NotID + i;
        // Gets an instance of the NotificationManager service
        NotificationManager mNotifyMgr =
                (NotificationManager) c.getSystemService(NOTIFICATION_SERVICE);

        //notification channel in android 8
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = "KIRINO_CHANNEL";// The id of the channel.
            CharSequence name = c.getString(R.string.app_name);// The user-visible name of the channel.
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mNotifyMgr.createNotificationChannel(mChannel);
            mBuilder.setChannelId(CHANNEL_ID);
        }

        mNotifyMgr.notify(mNotificationId, mBuilder.build());
    }

    static void permute(String[] in, int start, List<String> out) {
            for (int i = start; i < in.length; i++) {
                String temp = in[start];
                in[start] = in[i];
                in[i] = temp;
                permute(in, start + 1, out);
                in[i] = in[start];
                in[start] = temp;
            }
            if (start == in.length - 1) {
                String temp = in[0];
                for (int i = 1; i < in.length; i++) {
                    temp += "_" + in[i];
                }
                out.add(temp);
            }
    }

    static void download(final String in, final Context c)
    {
        DownloadManager.Request req = new DownloadManager.Request(Uri.parse(in));
        req.setTitle(c.getString(R.string.downloading));
        req.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,in.split("/")[in.split("/").length-1]);
        DownloadManager downloadManager = (DownloadManager) c.getSystemService(Context.DOWNLOAD_SERVICE);
        downloadManager.enqueue(req);
    }

    static void DatePicker(final Context c, final boolean start, final Media media, final TextView text)
    {
        AlertDialog.Builder alert = new AlertDialog.Builder(c);

        if (start) {
            alert.setTitle(c.getString(R.string.start_date));
        } else {
            alert.setTitle(c.getString(R.string.end_date));
        }

        final DatePicker DatePick = new DatePicker(c);
        alert.setView(DatePick);

        alert.setPositiveButton(c.getString(R.string.ok), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if (start)
                {
                    media.started_year = DatePick.getYear();
                    media.started_month = DatePick.getMonth()+1;
                    media.started_day = DatePick.getDayOfMonth();
                    text.setText(c.getString(R.string.start_date)+" "+media.started_year+"/"+media.started_month+"/"+media.started_day);
                } else {
                    media.ended_year = DatePick.getYear();
                    media.ended_month = DatePick.getMonth()+1;
                    media.ended_day = DatePick.getDayOfMonth();
                    text.setText(c.getString(R.string.end_date)+" "+media.ended_year+"/"+media.ended_month+"/"+media.ended_day);
                }
            }
        });

        alert.show();
    }

    static void store_CR_credentials(final String email, final String password, final Context c)
    {
        try {
            final SharedPreferences sharedPref = c.getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE);

            //we create a new key for every CR account, to give hacker-chan a very hard time
            //we store email in sharedpref as plaintext
            //we encrypt the password and store the encrypted password in sharedpref

            KeyStore keystore = KeyStore.getInstance("AndroidKeyStore");
            keystore.load(null);

            //delete entry for email if exists
            keystore.deleteEntry(email);

            //and add
            Calendar start = Calendar.getInstance();
            Calendar end = Calendar.getInstance();
            end.add(Calendar.YEAR, 10); //nobody will be using this in 10 years, will they?
            KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(c)
                    .setAlias(email)
                    .setSubject(new X500Principal("CN=Crunchyroll password, O=net.k1ra.kirino"))
                    .setSerialNumber(BigInteger.ONE)
                    .setStartDate(start.getTime())
                    .setEndDate(end.getTime())
                    .build();
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
            generator.initialize(spec);
            generator.generateKeyPair();

            //encrypt password
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keystore.getEntry(email, null);
            Cipher input = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            input.init(Cipher.ENCRYPT_MODE, privateKeyEntry.getCertificate().getPublicKey());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            CipherOutputStream cipherOutputStream = new CipherOutputStream(
                    outputStream, input);
            cipherOutputStream.write(password.getBytes("UTF-8"));
            cipherOutputStream.close();

            byte [] vals = outputStream.toByteArray();
            sharedPref.edit().putString("CR_password", Base64.encodeToString(vals, Base64.DEFAULT)).putString("CR_email", email).commit();
        } catch (Exception e) {
            Errors.toast(c, c.getString(R.string.keystore_exception));
        }
    }

    static String fetch_CR_password(final Context c)
    {
        try {
            final SharedPreferences sharedPref = c.getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE);

            KeyStore keystore = KeyStore.getInstance("AndroidKeyStore");
            keystore.load(null);

            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)keystore.getEntry(sharedPref.getString("CR_email", ""), null);

            Cipher output = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            output.init(Cipher.DECRYPT_MODE, privateKeyEntry.getPrivateKey());

            CipherInputStream cipherInputStream = new CipherInputStream(
                    new ByteArrayInputStream(Base64.decode(sharedPref.getString("CR_password", ""), Base64.DEFAULT)), output);
            ArrayList<Byte> values = new ArrayList<>();
            int nextByte;
            while ((nextByte = cipherInputStream.read()) != -1) {
                values.add((byte)nextByte);
            }

            byte[] bytes = new byte[values.size()];
            for(int i = 0; i < bytes.length; i++) {
                bytes[i] = values.get(i).byteValue();
            }

            return new String(bytes, 0, bytes.length, "UTF-8");
        } catch (Exception e) {
            Errors.toast(c, c.getString(R.string.keystore_exception));
            return null;
        }
    }

    public static byte[] getBytesFromInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[0xFFFF];
        for (int len = is.read(buffer); len != -1; len = is.read(buffer)) {
            os.write(buffer, 0, len);
        }
        return os.toByteArray();
    }

    static Bitmap decode_image(byte[] input)
    {
        if (input[0] == (byte)69) {
            byte[] buffer = new byte[input.length + 15];
            int size = input.length + 7;
            byte cipherKey = (byte)255;

            buffer[0] = 82;
            buffer[1] = 73;
            buffer[2] = 70;
            buffer[3] = 70;
            byte var5 = (byte)size;
            byte var10 = 4;
            byte var11 = (byte)(cipherKey & var5);
            buffer[var10] = var11;
            cipherKey = (byte)(size >>> 8);
            var5 = (byte)255;
            var10 = 5;
            var11 = (byte)(cipherKey & var5);
            buffer[var10] = var11;
            cipherKey = (byte)(size >>> 16);
            var5 = (byte)255;
            var10 = 6;
            var11 = (byte)(cipherKey & var5);
            buffer[var10] = var11;
            cipherKey = (byte)(size >>> 24);
            var5 = (byte)255;
            var10 = 7;
            var11 = (byte)(cipherKey & var5);
            buffer[var10] = var11;
            buffer[8] = 87;
            buffer[9] = 69;
            buffer[10] = 66;
            buffer[11] = 80;
            buffer[12] = 86;
            buffer[13] = 80;
            buffer[14] = 56;
            cipherKey = (byte)101;
            int r = 0;

            for(int var6 = input.length; r < var6; ++r) {
                int var10001 = r + 15;
                byte var8 = input[r];
                int var13 = var10001;
                var11 = (byte)(cipherKey ^ var8);
                buffer[var13] = var11;
            }

            input = buffer;
        }

        return BitmapFactory.decodeByteArray(input, 0, input.length);
    }
}
