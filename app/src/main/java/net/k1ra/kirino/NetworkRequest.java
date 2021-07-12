package net.k1ra.kirino;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Pair;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilderFactory;

abstract class NetworkRequest {

    static boolean debug_all = false;


    static Pair[]make_AL_query(final int type, final String in, final int page, Context... c)
    {
        //TYPES:
        //0 = standard anime/manga search
        //1 = character search
        //2 = anime/manga details fetch
        //3 = character details fetch
        //4 = login test/get UID
        //5 = anime and manga planning list fetch
        //6 = adding anime/manga to watch list. This is a special case where page is ID and "in" is PLANNING/COMPLETED/DROPPED
        //7 = deleting anime/manga from list
        //8 = just getting media list entry ID
        //9 = get episodes
        //10 = get waifu list
        //11 = get season list
        //12 = just get the ID of all anime/manga on list

        String out = "";
        switch(type)
        {
            case 0:
                out = "query ($perPage: Int) {\n" +
                        "  Page (page: "+page+", perPage: $perPage) {\n" +
                        "    pageInfo {\n" +
                        "      total\n" +
                        "      currentPage\n" +
                        "      lastPage\n" +
                        "      hasNextPage\n" +
                        "      perPage\n" +
                        "    }\n" +
                        "    media (search: \""+in+"\") {\n" +
                        "      id\n" +
                        "      type\n" +
                        "      title {\n" +
                        "        romaji\n" +
                        "      }\n" +
                        "      coverImage\n" +
                        "      {\n" +
                        "        large\n" +
                        "      }\n" +
                        "      averageScore\n" +
                        "      description(asHtml: false)\n" +
                        "      episodes \n" +
                        "      chapters \n" +
                        "      format\n" +
                        "      startDate {\n" +
                        "        year\n" +
                        "        month\n" +
                        "        day\n" +
                        "      }\n" +
                        "    }\n" +
                        "  }\n" +
                        "}";
                break;
            case 1:
                out = "query ($perPage: Int) {\n" +
                        "  Page (page: "+page+", perPage: $perPage) {\n" +
                        "    pageInfo {\n" +
                        "      total\n" +
                        "      currentPage\n" +
                        "      lastPage\n" +
                        "      hasNextPage\n" +
                        "      perPage\n" +
                        "    }\n" +
                        "    characters (search:\""+in+"\") {\n" +
                        "      id\n" +
                        "      name\n" +
                        "      {\n" +
                        "        first\n" +
                        "        last\n" +
                        "      }\n" +
                        "      description(asHtml:false)\n" +
                        "      image {\n" +
                        "        large\n" +
                        "      }\n" +
                        "    }\n" +
                        "  }\n" +
                        "}";
                break;
            case 2:
                out = "query { \n" +
                        "  Media (id: "+in+") { \n" +
                        "    id\n" +
                        "      type\n" +
                        "    title {\n" +
                        "      romaji\n" +
                        "    }\n" +
                        "    coverImage\n" +
                        "    {\n" +
                        "      large\n" +
                        "    }\n" +
                        "    format\n" +
                        "    averageScore\n" +
                        "    description(asHtml: false)\n" +
                        "    episodes\n" +
                        "      chapters \n" +
                        "    status\n" +
                        "\t\tnextAiringEpisode {\n" +
                        "\t\t  id\n" +
                        "      episode\n" +
                        "      airingAt\n" +
                        "\t\t}\n" +
                        "    startDate {\n" +
                        "      year\n" +
                        "      month\n" +
                        "      day\n" +
                        "    }\n" +
                        "    endDate {\n" +
                        "      year\n" +
                        "      month\n" +
                        "      day\n" +
                        "    }\n" +
                        "    studios {\n" +
                        "      nodes\n" +
                        "      {\n" +
                        "        name\n" +
                        "      }\n" +
                        "    }\n" +
                        "streamingEpisodes {\n" +
                        "      url\n" +
                        "      site\n" +
                        "    }"+
                        "    relations\n" +
                        "    {\n" +
                        "      nodes{\n" +
                        "      id\n" +
                        "      type\n" +
                        "      title {\n" +
                        "        romaji\n" +
                        "      }\n" +
                        "      coverImage\n" +
                        "      {\n" +
                        "        large\n" +
                        "      }\n" +
                        "      averageScore\n" +
                        "      description(asHtml: false)\n" +
                        "      episodes \n" +
                        "      chapters \n" +
                        "      format\n" +
                        "      startDate {\n" +
                        "        year\n" +
                        "        month\n" +
                        "        day\n" +
                        "      }\n" +
                        "      }\n" +
                        "      edges {\n" +
                        "        relationType\n" +
                        "      }\n" +
                        "    }\n" +
                        "    \n" +
                        " externalLinks {\n" +
                        "      id\n" +
                        "      url\n" +
                        "      site\n" +
                        "    }"+
                        "    tags {\n" +
                        "      id\n" +
                        "      name\n" +
                        "      isGeneralSpoiler\n" +
                        "      isMediaSpoiler\n" +
                        "      rank\n" +
                        "    }\n" +
                        "    \n" +
                        "    genres\n" +
                        "    \n" +
                        "    characters(sort:ROLE) {\n" +
                        "      nodes{\n" +
                        "        id\n" +
                        "        image {\n" +
                        "          large\n" +
                        "        }\n" +
                        "        name {\n" +
                        "          first\n" +
                        "          last\n" +
                        "        }\n" +
                        "      }\n" +
                        "      edges\n" +
                        "      {\n" +
                        "        role\n" +
                        "      }\n" +
                        "\n" +
                        "      \n" +
                        "     \n" +
                        "    }\n" +
                        "    \n" +
                        "  }\n" +
                        "}";
                break;
            case 3:
                out = "query { \n" +
                        "Character(id:"+in+")\n" +
                        "  {id\n" +
                        "    name {\n" +
                        "      first\n" +
                        "      last\n" +
                        "    }\n" +
                        "    description(asHtml: false)\n" +
                        "    \n" +
                        "    image\n" +
                        "    {\n" +
                        "      large\n" +
                        "    }\n" +
                        "    \n" +
                        "    media\n" +
                        "    {\n" +
                        "      nodes\n" +
                        "      {\n" +
                        "        id\n" +
                        "      type\n" +
                        "        title\n" +
                        "        {\n" +
                        "          romaji\n" +
                        "        }\n" +
                        "        \n" +
                        "        coverImage\n" +
                        "        {\n" +
                        "          large\n" +
                        "        }\n" +
                        "        averageScore\n" +
                        "        description(asHtml: false)\n" +
                        "        episodes\n" +
                        "      chapters \n" +
                        "        format\n" +
                        "        startDate {\n" +
                        "          year\n" +
                        "          month\n" +
                        "          day\n" +
                        "        }\n" +
                        "        \n" +
                        "      }\n" +
                        "    }\n" +
                        "    \n" +
                        "    \n" +
                        "  }\n" +
                        "}";
                break;
            case 4:
                out = "query{\n" +
                        "Viewer {\n" +
                        "id\n" +
                        "name\n" +
                        "}\n" +
                        "}\n";
                break;
            case 5:
                out = "query{\n" +
                        "  Page(page: "+page+") {\n" +
                        "    pageInfo {\n" +
                        "      total\n" +
                        "      currentPage\n" +
                        "      lastPage\n" +
                        "      hasNextPage\n" +
                        "      perPage\n" +
                        "    }\n" +
                        "    mediaList(userId: "+c[0].getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).getString("AL_uid","")+", status: PLANNING) {\n" +
                        "score\n" +
                        "      progress\n" +
                        "      progressVolumes\n" +
                        "      repeat\n" +
                        "      startedAt {\n" +
                        "        year\n" +
                        "        month\n" +
                        "        day\n" +
                        "      }\n" +
                        "      completedAt\n" +
                        "      {\n" +
                        "        year\n" +
                        "        month\n" +
                        "        day\n" +
                        "      }"+
                        "      media {\n" +
                        "        id\n" +
                        "        type\n" +
                        "        status\n" +
                        "        title {\n" +
                        "          romaji\n" +
                        "        }\n" +
                        "        coverImage {\n" +
                        "          large\n" +
                        "        }\n" +
                        "        nextAiringEpisode\n" +
                        "        {\n" +
                        "          airingAt\n" +
                        "        }\n" +
                        "        externalLinks\n" +
                        "        {\n" +
                        "          url\n" +
                        "          site\n" +
                        "        }\n" +
                        "        averageScore\n" +
                        "        description(asHtml: false)\n" +
                        "        episodes\n" +
                        "      chapters \n" +
                        "        format\n" +
                        "        startDate {\n" +
                        "          year\n" +
                        "          month\n" +
                        "          day\n" +
                        "        }\n" +
                        "      }\n" +
                        "    }\n" +
                        "  }\n" +
                        "}";
                break;
            case 6:
                out = "mutation\n" +
                        "{\n" +
                        "  SaveMediaListEntry(mediaId:"+page+", status:"+in+")\n" +
                        "  {\n" +
                        "    id\n" +
                        "  }\n" +
                        "}";
                break;
            case 7:
                out = "mutation\n" +
                        "{\n" +
                        "  DeleteMediaListEntry(id:"+in+"){" +
                        "deleted" +
                        "}\n" +
                        "}";
                break;
            case 8:
                out = "query{\n" +
                        "  Page(page: "+page+") {\n" +
                        "    pageInfo {\n" +
                        "      total\n" +
                        "      currentPage\n" +
                        "      lastPage\n" +
                        "      hasNextPage\n" +
                        "      perPage\n" +
                        "    }\n" +
                        "    mediaList(userId: "+c[0].getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).getString("AL_uid","")+", mediaId: "+in+") {\n" +
                        "id\n" +
                        "    }\n" +
                        "  }\n" +
                        "}";
                break;
            case 9:
                out = "query {\n" +
                        "Media (id: "+in+") { duration \n" +
                        "streamingEpisodes {\n" +
                        "title\n" +
                        "thumbnail\n" +
                        "url\n" +
                        "site\n" +
                        "}\n" +
                        "}\n" +
                        "}";
                break;
            case 10:
                out = "query{\n" +
                        "  Viewer {\n" +
                        "    id\n" +
                        "    favourites {\n" +
                        "      characters(page:"+page+") {\n" +
                        "        nodes {\n" +
                        "          id\n" +
                        "          name {\n" +
                        "            first\n" +
                        "            last\n" +
                        "          }\n" +
                        "          description(asHtml:false)\n" +
                        "          image{\n" +
                        "            large\n" +
                        "          }\n" +
                        "          media\n" +
                        "          {\n" +
                        "            nodes\n" +
                        "            {\n" +
                        "              id\n" +
                        "              type\n" +
                        "              title\n" +
                        "              {\n" +
                        "                romaji\n" +
                        "              }\n" +
                        "              coverImage\n" +
                        "              {\n" +
                        "                large\n" +
                        "              }\n" +
                        "              averageScore\n" +
                        "              description(asHtml:false)\n" +
                        "              episodes\n" +
                        "      chapters \n" +
                        "              format\n" +
                        "              startDate {\n" +
                        "                year\n" +
                        "                month\n" +
                        "                day\n" +
                        "              }\n" +
                        "              \n" +
                        "            }\n" +
                        "          }\n" +
                        "        }\n" +
                        "      }\n" +
                        "    }\n" +
                        "  }\n" +
                        "}";
                break;
            case 11:
                out = "query {\n" +
                        "  Page(page: "+page+", perPage: 50) {\n" +
                        "    pageInfo {\n" +
                        "      total\n" +
                        "      currentPage\n" +
                        "      lastPage\n" +
                        "      hasNextPage\n" +
                        "      perPage\n" +
                        "    }\n" +
                        "    media(season: "+in.split("\\|")[0]+", seasonYear: "+in.split("\\|")[1]+") {\n" +
                        "      id\n" +
                        "      type\n" +
                        "      title {\n" +
                        "        romaji\n" +
                        "      }\n" +
                        "      coverImage {\n" +
                        "        large\n" +
                        "      }\n" +
                        "      format\n" +
                        "      averageScore\n" +
                        "      description(asHtml: false)\n" +
                        "      episodes\n" +
                        "      chapters \n" +
                        "      status\n" +
                        "      nextAiringEpisode {\n" +
                        "        id\n" +
                        "        episode\n" +
                        "        airingAt\n" +
                        "      }\n" +
                        "      startDate {\n" +
                        "        year\n" +
                        "        month\n" +
                        "        day\n" +
                        "      }\n" +
                        "      endDate {\n" +
                        "        year\n" +
                        "        month\n" +
                        "        day\n" +
                        "      }\n" +
                        "      studios {\n" +
                        "        nodes {\n" +
                        "          name\n" +
                        "        }\n" +
                        "      }\n" +
                        "      streamingEpisodes {\n" +
                        "        url\n" +
                        "        site\n" +
                        "      }\n" +
                        "      relations {\n" +
                        "        nodes {\n" +
                        "          id\n" +
                        "          type\n" +
                        "          title {\n" +
                        "            romaji\n" +
                        "          }\n" +
                        "          coverImage {\n" +
                        "            large\n" +
                        "          }\n" +
                        "          averageScore\n" +
                        "          description(asHtml: false)\n" +
                        "          episodes\n" +
                        "      chapters \n" +
                        "          format\n" +
                        "          startDate {\n" +
                        "            year\n" +
                        "            month\n" +
                        "            day\n" +
                        "          }\n" +
                        "        }\n" +
                        "        edges {\n" +
                        "          relationType\n" +
                        "        }\n" +
                        "      }\n" +
                        "      externalLinks {\n" +
                        "        id\n" +
                        "        url\n" +
                        "        site\n" +
                        "      }\n" +
                        "      tags {\n" +
                        "        id\n" +
                        "        name\n" +
                        "        isGeneralSpoiler\n" +
                        "        isMediaSpoiler\n" +
                        "        rank\n" +
                        "      }\n" +
                        "      genres\n" +
                        "      characters(sort: ROLE) {\n" +
                        "        nodes {\n" +
                        "          id\n" +
                        "          image {\n" +
                        "            large\n" +
                        "          }\n" +
                        "          name {\n" +
                        "            first\n" +
                        "            last\n" +
                        "          }\n" +
                        "        }\n" +
                        "        edges {\n" +
                        "          role\n" +
                        "        }\n" +
                        "      }\n" +
                        "    }\n" +
                        "  }\n" +
                        "}";
                break;
            case 12:
                out = "query{\n" +
                        "  Page(page: "+page+") {\n" +
                        "    pageInfo {\n" +
                        "      total\n" +
                        "      currentPage\n" +
                        "      lastPage\n" +
                        "      hasNextPage\n" +
                        "      perPage\n" +
                        "    }\n" +
                        "    mediaList(userId: "+c[0].getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).getString("AL_uid","")+") {\n" +
                        "      media {\n" +
                        "        id\n" +
                        "      }\n" +
                        "    }\n" +
                        "  }\n" +
                        "}";
                break;
        }

        return new Pair[]{Pair.create("query", out)};
    }

    static class Parameters
    {
        String url;
        boolean strict = true;
        boolean AL_authenticated = false;
        boolean CR_authenticated = false;
        boolean append_CR_params = false;
        boolean MR_img = false;

        public Parameters(final String u)
        {
            url = u;
        }
        Parameters noStrict()
        {
            strict = false;
            return this;
        }
        Parameters Strict()
        {
            strict = true;
            return this;
        }
        Parameters Authenticated()
        {
            AL_authenticated = true;
            return this;
        }
        Parameters notAuthenticated()
        {
            AL_authenticated = false;
            return this;
        }
        Parameters CR_Authenticated()
        {
            CR_authenticated = true;
            return this;
        }
        Parameters append_CR()
        {
            append_CR_params = true;
            return this;
        }
        Parameters is_MR_img()
        {
            MR_img = true;
            return this;
        }
    }

    abstract static class CR_params {
        final static String access_token = "Scwg9PRRZ19iVwD";
        final static String device_type = "com.crunchyroll.crunchyroid";
        final static String device_id = UUID.randomUUID().toString();
        final static String version = "2313.8";
        final static String locale = "enUS";
    }

    final static Parameters AniList = new Parameters("https://graphql.anilist.co");
    final static Parameters TagQuery = new Parameters("https://danbooru.donmai.us/tags.json");
    final static Parameters Gelbooru = new Parameters("https://gelbooru.com/index.php");

    final static Parameters CR_login = new Parameters("https://api.crunchyroll.com/login.0.json").append_CR();
    final static Parameters CR_start_session = new Parameters("https://api.crunchyroll.com/start_session.0.json").append_CR();
    final static Parameters CR_batch = new Parameters("https://api.crunchyroll.com/batch.0.json").append_CR().CR_Authenticated();
    final static Parameters CR_info = new Parameters("https://api.crunchyroll.com/info.0.json").append_CR().CR_Authenticated();
    final static Parameters CR_log = new Parameters("https://api.crunchyroll.com/log.0.json").append_CR().CR_Authenticated();

    //use masterani.me because kissanime are massive faggots that should be eliminated
    final static Parameters MA_search = new Parameters("https://www.masterani.me/api/anime/search");
    final static Parameters MA_details = new Parameters("https://www.masterani.me/api/anime/");
    final static Parameters MA_episode = new Parameters("https://www.masterani.me/anime/watch/");

    //MangaRock
    final static Parameters MR_search = new Parameters("https://api.mangarockhd.com/query/web400/mrs_search?country=Canada"); //Blame Canada!
    final static Parameters MR_meta = new Parameters("https://api.mangarockhd.com/meta");
    final static Parameters MR_info = new Parameters("https://api.mangarockhd.com/query/web400/info");
    final static Parameters MR_chapter = new Parameters("https://api.mangarockhd.com/query/web400/pages");

    //to update overrides
    final static Parameters kirino_update = new Parameters("http://www.kirino.app/update.php");

    static void make(final NetworkResponse out, final Parameters p, final boolean POST, final boolean ErrorPage, final Pair[] params, final Context c, final Runnable onSuccess, final Runnable onError, final boolean... man_debug)
    {
        final boolean debug = man_debug.length > 0 && man_debug[0] || debug_all;

        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    String url_s = p.url;
                    if (p == MA_details)
                    {
                        url_s += params[0].second.toString()+"/detailed";
                    }
                    else if (p == MA_episode)
                    {
                        url_s += params[0].second.toString();
                    }

                    if (params != null && !POST && p != MA_details && p != MA_episode)
                    {
                        url_s += "?"+params[0].first+"="+ URLEncoder.encode(params[0].second.toString(), "UTF-8");
                        for (int i = 1; i < params.length; i++)
                        {
                            url_s += "&"+params[i].first+"="+ URLEncoder.encode(params[i].second.toString(), "UTF-8");
                        }
                    }

                    if (p.CR_authenticated)
                    {
                        url_s += "?&session_id="+c.getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).getString("CR_session_id","");
                    }

                    URL url = new URL(url_s);
                    if (debug) { System.out.println(url.toString()); }
                    HttpURLConnection client = (HttpURLConnection) url.openConnection();
                    client.setReadTimeout(10000);
                    client.setConnectTimeout(15000);

                    if (p == MR_search || p == MR_meta)
                    {
                        client.setRequestMethod("POST");
                        client.setDoInput(true);
                        client.setDoOutput(true);
                        client.setRequestMethod("POST");
                        client.setRequestProperty("Accept", "application/json");
                        client.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                        OutputStreamWriter writer = new OutputStreamWriter(client.getOutputStream(), "UTF-8");
                        writer.write(params[0].second.toString());
                        writer.close();
                    }
                    else if (POST)
                    {
                    client.setRequestMethod("POST");
                    client.setDoInput(true);
                    client.setDoOutput(true);



                    Uri.Builder builder = new Uri.Builder();
                    if (p.AL_authenticated && c != null)
                    {
                        client.setRequestProperty("Authorization", "Bearer "+(c.getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).getString("AL_token", "")));
                        if (man_debug.length > 1 && man_debug[1]){ System.out.println("Authorization"+"  -  "+"Bearer "+(c.getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).getString("AL_token", ""))); }
                    }

                        if (params != null) {
                            for (int i = 0; i < params.length; i++) {
                                builder.appendQueryParameter(params[i].first.toString(), params[i].second.toString());
                                if (man_debug.length > 1 && man_debug[1]) {
                                    System.out.println(params[i].first.toString() + "  -  " + params[i].second.toString());
                                }
                            }
                        }

                        if (p.append_CR_params)
                        {
                            builder.appendQueryParameter("access_token", c.getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).getString("CR_a_t", CR_params.access_token));
                            builder.appendQueryParameter("device_type", CR_params.device_type);
                            builder.appendQueryParameter("device_id", CR_params.device_id);
                            builder.appendQueryParameter("version", CR_params.version);
                            builder.appendQueryParameter("locale", CR_params.locale);
                        }

                    String query = builder.build().getEncodedQuery();

                    OutputStream os = client.getOutputStream();
                    BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(os, "UTF-8"));
                    writer.write(query);
                    writer.flush();
                    writer.close();
                    os.close();
                    }
                    out.sCode = client.getResponseCode();

                    if (out.sCode != 200) {
                        InputStream in_err = new BufferedInputStream(client.getErrorStream());
                        BufferedReader reader_err = new BufferedReader(new InputStreamReader(
                                in_err, "iso-8859-1"), 8);
                        StringBuilder sb_err = new StringBuilder();
                        String line_err = null;
                        while ((line_err = reader_err.readLine()) != null) {
                            sb_err.append(line_err + "\n");
                        }
                        if (debug) {
                            System.out.println(sb_err.toString());
                        }
                        out.err = sb_err.toString();
                    }

                    if (debug) { System.out.println("CODE: "+out.sCode); }
                    client.connect();

                    InputStream in = new BufferedInputStream(client.getInputStream());

                    BufferedReader reader = new BufferedReader(new InputStreamReader(
                            in, "iso-8859-1"), 8);
                    StringBuilder sb = new StringBuilder();
                    String line = null;
                    if (!p.MR_img) {
                        while ((line = reader.readLine()) != null) {
                            sb.append(line + "\n");
                        }
                    } else {
                        out.bytes = Tools.getBytesFromInputStream(in);
                    }

                    in.close();
                    client.disconnect();
                    if (debug) { System.out.println("OUT: "+sb.toString()); }

                    if (p != TagQuery && p != Gelbooru && p != MA_search && p != MA_episode && !p.MR_img) {
                        out.obj = new JSONObject(sb.toString());
                    } else if (!p.MR_img) { out.str = sb.toString(); }


                    if (p != TagQuery && p.strict) {Loading.dismiss();}

                        try {
                            if (onSuccess != null) {
                                if (c != null) {
                                    ((Activity) c).runOnUiThread(onSuccess);
                                } else {
                                    onSuccess.run();
                                }
                            }
                        } catch (ClassCastException e) {
                            onSuccess.run();
                        }

                } catch (IOException | JSONException | NullPointerException e) { //e.printStackTrace();
                    if (p != TagQuery && p.strict) {Loading.dismiss();}
                    if (out.sCode == 401 || out.sCode == 400) {
                        out.error = 4;

                        if (c != null) {
                            SharedPreferences.Editor editor = c.getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).edit();
                            editor.putString("AL_token", "");
                            editor.commit();
                            if (c.getClass() != sync_service.class) {
                                Errors.toast(c, c.getString(R.string.al_sess_invalid));
                            } else {onError.run();}
                        } else {
                            onError.run();
                        }
                    }
                    else {
                        if (!p.strict) {
                            if (ErrorPage) {
                                Errors.no_internet_page(c);
                            } else {
                                Errors.no_internet_toast(c);
                            }
                        } else {
                            try {
                                if (onError != null) {
                                    if (c != null) {
                                        ((Activity) c).runOnUiThread(onError);
                                    } else {
                                        onError.run();
                                    }
                                }
                            } catch (ClassCastException e2) {
                                if (onError != null) {onError.run();}
                            }
                        }
                    }
                }
            }
        };

        if (p.strict) {Loading.show(c);}
        if (p.AL_authenticated && !p.strict && c == null || p == TagQuery || p.AL_authenticated && !p.strict && c.getClass() == sync_service.class)
        {
           task.run(); //this only happens under special conditions, like Tools.fetch_AL_lists and AL_name_to_gelbooru_tag and other sequences of requests fired off sequentially
        } else {
            new Thread(task).start(); //this is what happens normally
        }
    }


}
