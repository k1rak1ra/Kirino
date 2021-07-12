package net.k1ra.kirino;

import android.Manifest;
import android.app.Dialog;
import android.app.DownloadManager;
import android.arch.persistence.room.Room;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;

import com.google.firebase.dynamiclinks.DynamicLink;
import com.google.firebase.dynamiclinks.FirebaseDynamicLinks;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;

public class frag_home extends Fragment {
    //recyclerview for feed
    RecyclerView list;
    //DB for feed
    Feed_DB DB;
    //list for feed
    List<FeedItem> feedItems = new ArrayList<FeedItem>();
    //adapter for feed
    feed_adapter feed_adapter = new feed_adapter();
    //for link transfer in case of permisison request
    String link = "";

    //reciever for adding to feed
    BroadcastReceiver feed_add_reciever;


    public static Fragment newInstance() {
        return new frag_home();
    }

    @Override
    public void onDestroyView()
    {
        //dispose of DB connection and reciever
        DB.close();
        getActivity().unregisterReceiver(feed_add_reciever);

        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.frag_home, container, false);
        final EditText search_string = rootView.findViewById(R.id.search_string);
        final ImageButton search_btn = rootView.findViewById(R.id.search_btn);
        final ImageButton current = rootView.findViewById(R.id.view_seasonal);
        final ImageButton random = rootView.findViewById(R.id.view_random);
        list = rootView.findViewById(R.id.frag_home_list);


        //init DB
        DB = Room.databaseBuilder(getActivity(),
                Feed_DB.class, "feed_db")
                .build();

        //load feed items and delete any items that are too old to show up to save space
        Loading.show(getActivity());
        new Thread(new Runnable() {
            @Override
            public void run() {
                feedItems = DB.DAO().fetch_all();
                try { // if there's under 200 this will cause indexoutofbounds exception
                    DB.DAO().delete_old(feedItems.get(0).id - 200); //still save a little backlog just in case
                } catch (Exception e) {}
                Loading.dismiss();

                //and set adapter/LLM for list
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
                        list.setLayoutManager(llm);
                        list.setAdapter(feed_adapter);
                        list.scrollToPosition(feedItems.size()-1);
                    }
                });
            }
        }).start();


        //reciever for adding to feed
        feed_add_reciever = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, Intent intnt) {
                if (intnt.getIntExtra("type", 0) != 0) {
                ToFeed(intnt.getIntExtra("type", 0), intnt.getStringExtra("id"), null);}
                else {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            feedItems = DB.DAO().fetch_all();
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    list.setAdapter(feed_adapter);
                                    list.scrollToPosition(feedItems.size()-1);
                                }
                            });
                        }
                    }).start();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("net.k1ra.update.feed_add");
        getActivity().registerReceiver(feed_add_reciever, filter);

        //process link intents
        if (getActivity().getIntent().getDataString() != null) {
        try {
            //first we see if it's an anime/manga
            if (getActivity().getIntent().getDataString().replace("http://","").replace("resolve.php?","").replace("=","/").split("/")[1].equals("media")) {
                //if so, fetch and add to feed
                ToFeed(2, getActivity().getIntent().getDataString().replace("http://","").replace("resolve.php?","").replace("=","/").split("/")[2], null);
            }
            //then we see if it's a chara
            else if (getActivity().getIntent().getDataString().replace("http://","").replace("resolve.php?","").replace("=","/").split("/")[1].equals("chara")) {
                //if so, fetch and add to feed
                ToFeed(3, getActivity().getIntent().getDataString().replace("http://","").replace("resolve.php?","").replace("=","/").split("/")[2], null);
            }
        } catch (Exception e) {}
        }


        //search button action
        search_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String req = search_string.getText().toString();
                search_string.setText("");

                final List<Media> results_manga = new ArrayList<Media>();
                final List<Media> results_anime = new ArrayList<Media>();
                final List<Character> results_chara = new ArrayList<Character>();
                final Dialog dialog = new Dialog(getActivity());
                dialog.setContentView(R.layout.dialog_search_results);
                dialog.setCancelable(true);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                dialog.show();

                final RecyclerView results_list = dialog.findViewById(R.id.search_results_list);
                final search_list_adapter adapter = new search_list_adapter();
                results_list.setAdapter(adapter);
                LinearLayoutManager llm = new LinearLayoutManager(getActivity());
                results_list.setLayoutManager(llm);

                adapter.anime = results_anime;
                adapter.manga = results_manga;
                adapter.chara = results_chara;
                adapter.d = dialog;

                //first fetch anime/manga and characters. hard-coded 6 max pages, will usually only be 1, but will simply return nothing for empty pages and will be ignored
                //page order will also be pretty random but who cares desu, it shouldn't be a big issue
                for (int i = 1; i < 6; i++) {
                    final NetworkResponse media_out = new NetworkResponse();
                    NetworkRequest.make(media_out, NetworkRequest.AniList.notAuthenticated().Strict(), true, false, NetworkRequest.make_AL_query(0, req, i), getActivity(), new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONArray items = media_out.obj.getJSONObject("data").getJSONObject("Page").getJSONArray("media");
                                for (int i = 0; i < items.length(); i++)
                                {
                                    try {
                                        String airString = "";
                                        if (!items.getJSONObject(i).getJSONObject("startDate").getString("day").equals("null"))
                                        {
                                            airString += items.getJSONObject(i).getJSONObject("startDate").getString("day")+"/";
                                        }
                                        if (!items.getJSONObject(i).getJSONObject("startDate").getString("month").equals("null"))
                                        {
                                            airString += items.getJSONObject(i).getJSONObject("startDate").getString("month")+"/";
                                        }
                                        if (!items.getJSONObject(i).getJSONObject("startDate").getString("year").equals("null"))
                                        {
                                            airString += items.getJSONObject(i).getJSONObject("startDate").getString("year");
                                        }

                                        Media out = new Media(items.getJSONObject(i).getString("id"),
                                                items.getJSONObject(i).getJSONObject("coverImage").getString("large"),
                                                items.getJSONObject(i).getJSONObject("title").getString("romaji"),
                                                items.getJSONObject(i).getString("type"),
                                                items.getJSONObject(i).getString("format"),
                                                items.getJSONObject(i).getString("averageScore"),
                                                items.getJSONObject(i).getString("episodes"),
                                                items.getJSONObject(i).getString("description"),
                                                airString);

                                        if (out.type.equals("ANIME")) {
                                            results_anime.add(out);
                                        }
                                        else
                                        {
                                            results_manga.add(out);
                                        }
                                    } catch (JSONException e) {}
                                }

                                adapter.anime = results_anime;
                                adapter.manga = results_manga;
                                results_list.setAdapter(adapter);

                            } catch (JSONException | NullPointerException e) {}
                        }
                    }, null);

                    final NetworkResponse chara_out = new NetworkResponse();
                    NetworkRequest.make(chara_out, NetworkRequest.AniList.notAuthenticated().Strict(), true, false, NetworkRequest.make_AL_query(1, req, i), getActivity(), new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONArray items = chara_out.obj.getJSONObject("data").getJSONObject("Page").getJSONArray("characters");
                                for (int i = 0; i < items.length(); i++) {
                                    try {
                                        String name = "";
                                        if (!items.getJSONObject(i).getJSONObject("name").getString("first").equals("null")){name +=items.getJSONObject(i).getJSONObject("name").getString("first")+" ";}
                                        if (!items.getJSONObject(i).getJSONObject("name").getString("last").equals("null")){name +=items.getJSONObject(i).getJSONObject("name").getString("last");}

                                        results_chara.add(new Character(items.getJSONObject(i).getString("id"),
                                                items.getJSONObject(i).getJSONObject("image").getString("large"),
                                                name,
                                                items.getJSONObject(i).getString("description")));
                                    } catch (JSONException e) {}
                                }

                                adapter.chara = results_chara;
                                results_list.setAdapter(adapter);

                            } catch (JSONException | NullPointerException e) {}
                        }
                    }, null);
                }

            }
        });
        current.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                seasonal_dialog();
            }
        });

        random.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fetch_random();
            }
        });

        return rootView;
    }


    class search_list_adapter extends RecyclerView.Adapter<search_list_adapter.SearchViewHolder>{

        List<Media> anime;
        List<Media> manga;
        List<Character> chara;
        Dialog d;

        class SearchViewHolder extends RecyclerView.ViewHolder {
            ImageView img;
            TextView name;
            TextView air_rating;
            TextView desc;
            CardView card;

            SearchViewHolder(View itemView, final int type) {
                super(itemView);
                if (type == 0) {
                    img = itemView.findViewById(R.id.a_img);
                    name = itemView.findViewById(R.id.a_name);
                    air_rating = itemView.findViewById(R.id.a_air_rating);
                    desc = itemView.findViewById(R.id.a_desc);
                    card = itemView.findViewById(R.id.a_card_simple);
                } else
                {
                    name = itemView.findViewById(R.id.dialog_search_title);
                }
            }
        }
        @Override
        public int getItemViewType(int position) {
            return position;
        }
        @Override
        public int getItemCount() {
            //add 3 dummy items for section titles
            return 3 + anime.size()+manga.size()+chara.size();
        }
        @Override
        public SearchViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View v = null;
            int type = 0;
            //if section title, use section title layout. If card, use card layout
            if (i == 0 || i == anime.size()+1 || i == anime.size()+manga.size()+2)
            {
                v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.dialog_search_title, viewGroup, false);
                type = 1;
            }
            else {
                v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.dialog_search_card, viewGroup, false);
            }
            SearchViewHolder vh = new SearchViewHolder(v, type);
            return vh;
        }
        @Override
        public void onBindViewHolder(final SearchViewHolder vh, final int i) {
            //populate cards and section titles depending on section/content
            //no description is displayed, makes things neater
            if (i == 0)
            {
                if (anime.size() == 0) { vh.name.setText(getText(R.string.s_r_anime_none)); } else { vh.name.setText(getText(R.string.s_r_anime)); }
            }
            else if (i > 0 && i < anime.size()+1)
            {
                //fill in details
                Tools.picasso.load(anime.get(i-1).image_URL).into(vh.img);
                vh.name.setText(anime.get(i-1).name);
                if (!anime.get(i-1).numEpisodes.equals("null")) {
                    vh.desc.setText(anime.get(i - 1).format + " - " + anime.get(i - 1).numEpisodes);
                }
                else
                {
                    vh.desc.setText(anime.get(i - 1).format);
                }
                if (!anime.get(i-1).score.equals("null")) {
                    if (anime.get(i - 1).AirDate.equals(""))
                    {
                        vh.air_rating.setText(anime.get(i - 1).score);
                    }
                    else {
                        vh.air_rating.setText(anime.get(i - 1).score + " - " + anime.get(i - 1).AirDate);
                    }
                }
                else
                {
                    vh.air_rating.setText(anime.get(i - 1).AirDate);
                }

                //display on card tap
                vh.card.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ToFeed(2, anime.get(i - 1).ALid, d);
                    }
                });
            }
            else if (i == anime.size()+1 || anime.size() == 0 && i == 1)
            {
                if (manga.size() == 0) { vh.name.setText(getText(R.string.s_r_manga_none)); } else { vh.name.setText(getText(R.string.s_r_manga)); }
            }
            else if (i > anime.size()+1 && i < anime.size()+manga.size()+2)
            {
                //fill in details
                Tools.picasso.load(manga.get(i-anime.size()-2).image_URL).into(vh.img);
                vh.name.setText(manga.get(i-anime.size()-2).name);
                if (!manga.get(i-anime.size()-2).numEpisodes.equals("null")) {
                    vh.desc.setText(manga.get(i-anime.size()-2).format + " - " + manga.get(i-anime.size()-2).numEpisodes);
                }
                else
                {
                    vh.desc.setText(manga.get(i-anime.size()-2).format);
                }
                if (!manga.get(i-anime.size()-2).score.equals("null")) {
                    if (manga.get(i - anime.size() - 2).AirDate.equals("")) {
                        vh.air_rating.setText(manga.get(i - anime.size() - 2).score);
                    }
                    else
                    {
                        vh.air_rating.setText(manga.get(i - anime.size() - 2).score + " - " + manga.get(i - anime.size() - 2).AirDate);
                    }
                }
                else
                {
                    vh.air_rating.setText(manga.get(i - anime.size() - 2).AirDate);
                }

                //display on card tap
                vh.card.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ToFeed(2, manga.get(i - anime.size() - 2).ALid, d);
                    }
                });
            }
            else if ( i == anime.size()+manga.size()+2 || anime.size()+manga.size() == 0 && i == 2)
            {
                if (chara.size() == 0) { vh.name.setText(getText(R.string.s_r_chara_none)); } else { vh.name.setText(getText(R.string.s_r_chara)); }
            }
            else if (i > anime.size()+manga.size()+2)
            {
                //fill in details
                Tools.picasso.load(chara.get(i-anime.size()-manga.size()-3).image_URL).into(vh.img);
                vh.name.setText(chara.get(i-anime.size()-manga.size()-3).name);
                vh.air_rating.setVisibility(View.INVISIBLE);

                //display on card tap
                vh.card.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ToFeed(3, chara.get(i-anime.size()-manga.size()-3).ALid, d);
                    }
                });
            }

        }
        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }
    }

    void ToFeed(final int type, final String id, final Dialog d)
    {
        //fetch item, put result JSON in FeedItem object, and display
        //type is passed directly to AL query maker, see look there for type list

        final NetworkResponse out = new NetworkResponse();
        NetworkRequest.make(out, NetworkRequest.AniList.notAuthenticated().Strict(), true, false, NetworkRequest.make_AL_query(type, id, 0), getActivity(), new Runnable() {
            @Override
            public void run() {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //insert and add to local list so no reloading from DB
                        DB.DAO().insert(new FeedItem(type, out.obj.toString()));
                        feedItems.add(new FeedItem(type, out.obj.toString()));

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //and notify feed adapter of newly inserted item and scroll to position in list
                                feed_adapter.notifyItemInserted(feedItems.size());
                                list.scrollToPosition(feedItems.size()-1);

                                //finally, close dialog
                                if (d != null) {d.dismiss();}
                            }
                        });
                    }
                }).start();
            }
        }, null);
    }

    Media_DB m_DB;
    character_DB c_DB;
    boolean spoil = false;
    class feed_adapter extends RecyclerView.Adapter<feed_adapter.FeedViewHolder>{

        boolean seasonal_list = false;
        List<FeedItem> seasonal;
        Dialog d = null;
        RecyclerView l;
        TextView n;
        feed_adapter a = this;

        class FeedViewHolder extends RecyclerView.ViewHolder {
            ImageView img;
            TextView name;
            TextView score;
            TextView dates;
            TextView ep_count;
            TextView desc;
            TextView studio;
            TextView genre;
            TextView tags;
            RecyclerView related;
            RecyclerView chara;
            ImageButton share;
            ImageButton add;
            Spinner season;
            EditText year;
            Button submit;

            FeedViewHolder(View itemView, final int type) {
                super(itemView);
                switch (type)
                {
                    case 0:
                        img = itemView.findViewById(R.id.fa_img);
                        break;
                    case 2:
                        img = itemView.findViewById(R.id.m_a_img);
                        name = itemView.findViewById(R.id.m_a_title);
                        score = itemView.findViewById(R.id.m_a_score);
                        dates = itemView.findViewById(R.id.m_a_dates);
                        ep_count = itemView.findViewById(R.id.m_a_ep_count);
                        desc = itemView.findViewById(R.id.m_a_desc);
                        studio = itemView.findViewById(R.id.m_a_studio);
                        genre = itemView.findViewById(R.id.m_a_genre);
                        tags = itemView.findViewById(R.id.m_a_tags);
                        related = itemView.findViewById(R.id.m_a_related);
                        chara = itemView.findViewById(R.id.m_a_charalist);
                        share = itemView.findViewById(R.id.m_a_share);
                        add = itemView.findViewById(R.id.m_a_add);
                        break;
                    case 20:
                        season = itemView.findViewById(R.id.d_s_spin);
                        year = itemView.findViewById(R.id.d_s_year);
                        submit = itemView.findViewById(R.id.d_s_fetch);
                        break;
                    default:
                        img = itemView.findViewById(R.id.chara_img);
                        name = itemView.findViewById(R.id.chara_name);
                        desc = itemView.findViewById(R.id.chara_desc);
                        related = itemView.findViewById(R.id.chara_relation);
                        share = itemView.findViewById(R.id.chara_share);
                        add = itemView.findViewById(R.id.chara_add);
                        break;
                }
            }
        }
        @Override
        public int getItemViewType(int position) {
            return position;
        }
        @Override
        public int getItemCount() {
            if (seasonal_list) { return seasonal.size(); }
            else { return feedItems.size(); }
        }
        @Override
        public FeedViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            FeedItem item = null;
            if (seasonal_list) { item = seasonal.get(i); }
            else { item = feedItems.get(i); }
            View v = null;
            //if media, use media card and if chara, use chara card. 0 = fanart image, handle that as well. If 20, use dialog_seasonal_top
            switch (item.type)
            {
                case 0:
                    v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.feed_card_image, viewGroup, false);
                    break;
                case 2:
                    v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.feed_card_media, viewGroup, false);
                    break;
                case 20:
                    v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.dialog_seasonal_top, viewGroup, false);
                    break;
                default:
                    v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.feed_card_chara, viewGroup, false);
                    break;
            }
            FeedViewHolder vh = new FeedViewHolder(v, item.type);
            return vh;
        }
        @Override
        public void onBindViewHolder(final FeedViewHolder vh, final int i) {
            final SharedPreferences sharedPref = getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE);
            FeedItem item_t = null;
            if (seasonal_list) { item_t = seasonal.get(i); }
            else { item_t = feedItems.get(i); }
            final FeedItem item = item_t;//screw you, java
            switch (item.type)
            {
                case 0: //if image. Also perform additional scaling if too big to prevent exceptions
                    Tools.picasso.load(item.json).resize(2500, 2500)
                            .onlyScaleDown().centerInside().into(vh.img);
                    vh.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View view) {
                            final Dialog download = new Dialog(getActivity());
                            download.setCancelable(true);
                            download.setContentView(R.layout.dialog_feed_image_download);
                            download.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

                            final Button delete = download.findViewById(R.id.fa_delet_dis);
                            final Button btn = download.findViewById(R.id.fa_download);
                            btn.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {

                                    PackageManager pm = getActivity().getPackageManager();
                                    int hasPerm = pm.checkPermission(
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                            getActivity().getPackageName());
                                    if (hasPerm != PackageManager.PERMISSION_GRANTED) {
                                        link = item.json;
                                        ActivityCompat.requestPermissions(getActivity(),
                                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                3);
                                    }
                                    else
                                    {
                                        Tools.download(item.json, getActivity());
                                    }

                                    download.dismiss();
                                }
                            });
                            delete.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            DB.DAO().delete_one(feedItems.get(i).id);
                                            getActivity().runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    feedItems.remove(i);
                                                    notifyItemRemoved(i);
                                                    download.dismiss();
                                                }
                                            });
                                        }
                                    }).start();
                                }
                            });

                            download.show();
                            return true;
                        }
                    });
                    break;
                case 2: //if anime/manga
                    try {
                        JSONObject media_t;
                        if (seasonal_list) {media_t = new JSONObject(item.json);} else {media_t = new JSONObject(item.json).getJSONObject("data").getJSONObject("Media");}
                        final JSONObject media = media_t;//WHY, JAVA!?
                        Tools.picasso.load(media.getJSONObject("coverImage").getString("large")).into(vh.img);
                        try {
                            vh.name.setText(media.getJSONObject("title").getString("romaji"));
                        } catch (Exception e) {}
                        if (!media.getString("averageScore").equals("null")) {
                            vh.score.setText(media.getString("averageScore"));
                        }
                        else
                        {
                            vh.score.setText(getString(R.string.no_score_data));
                        }
                        if (media.getString("status").equals("NOT_YET_RELEASED"))
                        {
                            String ad = "";
                            if (!media.getJSONObject("startDate").getString("day").equals("null")){ ad += media.getJSONObject("startDate").getString("day")+"/"; }
                            if (!media.getJSONObject("startDate").getString("month").equals("null")){ ad += media.getJSONObject("startDate").getString("month")+"/"; }
                            if (!media.getJSONObject("startDate").getString("year").equals("null")){ ad += media.getJSONObject("startDate").getString("year"); }
                            if (media.getString("type").equals("ANIME")) {
                                vh.dates.setText(getString(R.string.will_start_anime) +" "+ ad);
                            } else {
                                vh.dates.setText(getString(R.string.will_start_manga) +" "+ ad);
                            }
                        }
                        else if (media.getString("status").equals("RELEASING"))
                        {
                            if (media.getString("type").equals("ANIME")) {
                                vh.dates.setText(getString(R.string.current_anime));
                            } else {
                                vh.dates.setText(getString(R.string.current_manga));
                            }
                        }
                        else {
                            String ad = "";
                            if (!media.getJSONObject("startDate").getString("day").equals("null")){ ad += media.getJSONObject("startDate").getString("day")+"/"; }
                            if (!media.getJSONObject("startDate").getString("month").equals("null")){ ad += media.getJSONObject("startDate").getString("month")+"/"; }
                            if (!media.getJSONObject("startDate").getString("year").equals("null")){ ad += media.getJSONObject("startDate").getString("year"); }
                            if (media.getString("type").equals("ANIME")) {
                                vh.dates.setText(getString(R.string.old_anime) + " "+ ad);
                            } else {
                                vh.dates.setText(getString(R.string.old_manga) +" "+ ad);
                            }
                        }
                        if (!media.getString("episodes").equals("null"))
                        {
                            vh.ep_count.setText(media.getString("format") + " - " + media.getString("episodes") + "EP");
                        }
                        else if (!media.getString("chapters").equals("null"))
                        {
                            vh.ep_count.setText(media.getString("format") + " - " + media.getString("chapters") + "CHAP");
                        } else {
                            vh.ep_count.setText(media.getString("format"));
                        }
                        JSONArray studios = media.getJSONObject("studios").getJSONArray("nodes");
                        String studio_string = "";
                        for (int j = 0; j < studios.length(); j++) {
                            JSONObject studio = studios.getJSONObject(j);
                            studio_string += studio.getString("name")+"\n";
                        }
                        vh.studio.setText(studio_string);
                        JSONArray genres = media.getJSONArray("genres");
                        String genre_string = genres.getString(0);
                        for (int j = 1; j < genres.length(); j++) {
                            genre_string += ", "+genres.getString(j);
                        }
                        vh.genre.setText(genre_string);

                        JSONArray tags = media.getJSONArray("tags");
                        String tag_string = "";
                        for (int j = 0; j < tags.length(); j++)
                        {
                            if (tags.getJSONObject(j).getString("isGeneralSpoiler").equals("false") && tags.getJSONObject(j).getString("isMediaSpoiler").equals("false"))
                            {
                                tag_string += tags.getJSONObject(j).getString("name")+" - "+tags.getJSONObject(j).getString("rank")+"% \n";
                            }
                        }
                        vh.tags.setText(tag_string);

                        vh.desc.setText(Tools.parse_description(media.getString("description"), false));

                        //adapter for related list
                        final card_media_adapter media_adapter = new card_media_adapter();
                        media_adapter.edges = media.getJSONObject("relations").getJSONArray("edges");
                        media_adapter.nodes = media.getJSONObject("relations").getJSONArray("nodes");
                        media_adapter.d = d;
                        vh.related.setAdapter(media_adapter);
                        LinearLayoutManager llm_m = new LinearLayoutManager(getActivity());
                        vh.related.setLayoutManager(llm_m);

                        //adapter for chara list
                        final card_chara_adapter chara_adapter = new card_chara_adapter();
                        chara_adapter.edges = media.getJSONObject("characters").getJSONArray("edges");
                        chara_adapter.nodes = media.getJSONObject("characters").getJSONArray("nodes");
                        chara_adapter.d = d;
                        vh.chara.setAdapter(chara_adapter);
                        LinearLayoutManager llm_c = new LinearLayoutManager(getActivity());
                        vh.chara.setLayoutManager(llm_c);

                        //Handle adding to list. Data from feed entry will be reused, so if the feed entry is old, the data shown in the list will also be old until next sync
                        //Perhaps it might be a good idea to sync immediately right after adding, but this seems okay for now desu
                        vh.add.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            if (media.getString("type").equals("ANIME")) {
                                                m_DB = Room.databaseBuilder(getActivity(),
                                                        Media_DB.class, "anime_db")
                                                        .build();
                                            } else {
                                                m_DB = Room.databaseBuilder(getActivity(),
                                                        Media_DB.class, "manga_db")
                                                        .build();
                                            }

                                            String airString = "";
                                            if (!media.getJSONObject("startDate").getString("day").equals("null"))
                                            {
                                                airString += media.getJSONObject("startDate").getString("day")+"/";
                                            }
                                            if (!media.getJSONObject("startDate").getString("month").equals("null"))
                                            {
                                                airString += media.getJSONObject("startDate").getString("month")+"/";
                                            }
                                            if (!media.getJSONObject("startDate").getString("year").equals("null"))
                                            {
                                                airString += media.getJSONObject("startDate").getString("year");
                                            }
                                            final Media to_add = new Media(media.getString("id"),
                                                    media.getJSONObject("coverImage").getString("large"),
                                                    media.getJSONObject("title").getString("romaji"),
                                                    media.getString("type"),
                                                    media.getString("format"),
                                                    media.getString("averageScore"),
                                                    media.getString("episodes"),
                                                    media.getString("description"),
                                                    airString);
                                            to_add.current = media.getString("status").equals("RELEASING");
                                            if (to_add.numEpisodes.equals("null")) { to_add.numEpisodes = media.getString("chapters");}
                                            to_add.has_started_airing = !media.getString("status").equals("NOT_YET_RELEASED");
                                            if (to_add.current) {
                                                try {
                                                    to_add.new_ep_day = media.getJSONObject("nextAiringEpisode").getLong("airingAt");
                                                } catch (JSONException e) { }
                                            }
                                            JSONArray links = media.getJSONArray("externalLinks");
                                            for (int i = 0; i < links.length(); i++)
                                            {
                                                if (links.getJSONObject(i).getString("site").equals("Crunchyroll"))
                                                {
                                                    to_add.streamType = 1;
                                                    break;
                                                }
                                            }

                                            //add if does not exist. Only sync with AL if logged in and sync enabled
                                            if (m_DB.DAO().fetch(media.getInt("id")) == null) {
                                                if (media.getString("type").equals("ANIME") && sharedPref.getBoolean("sync_a", true) && !sharedPref.getString("AL_token", "").equals("") ||
                                                        media.getString("type").equals("MANGA") && sharedPref.getBoolean("sync_m", true) && !sharedPref.getString("AL_token", "").equals("")) {
                                                    final NetworkResponse out = new NetworkResponse();
                                                    NetworkRequest.make(out, NetworkRequest.AniList.Authenticated().noStrict(), true, false, NetworkRequest.make_AL_query(6, "PLANNING", media.getInt("id")), getActivity(), new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            new Thread(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    m_DB.DAO().insert(to_add);
                                                                    m_DB.close();
                                                                    getActivity().runOnUiThread(new Runnable() {
                                                                        @Override
                                                                        public void run() {
                                                                            Errors.toast(getActivity(), getString(R.string.added_to_list));
                                                                            getActivity().sendBroadcast(new Intent("net.k1ra.update.anime"));
                                                                            getActivity().sendBroadcast(new Intent("net.k1ra.update.manga"));
                                                                        }
                                                                    });
                                                                }
                                                            }).start();
                                                        }
                                                    }, null, true);
                                                }
                                                else
                                                {
                                                    m_DB.DAO().insert(to_add);
                                                    m_DB.close();
                                                    getActivity().runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            Errors.toast(getActivity(), getString(R.string.added_to_list));
                                                            getActivity().sendBroadcast(new Intent("net.k1ra.update.anime"));
                                                            getActivity().sendBroadcast(new Intent("net.k1ra.update.manga"));
                                                        }
                                                    });
                                                }
                                            }
                                            else
                                            {
                                                m_DB.close();
                                                getActivity().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Errors.toast(getActivity(), getString(R.string.already_in_list));
                                                    }
                                                });
                                            }

                                        } catch (Exception e) {}
                                    }
                                }).start();
                            }
                        });

                        //make a kirino.app link to share things to other app users
                        vh.share.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                try {
                                    DynamicLink dynamicLink = FirebaseDynamicLinks.getInstance().createDynamicLink()
                                            .setLink(Uri.parse("http://kirino.app/resolve.php?media="+media.getString("id")))
                                            .setDynamicLinkDomain("kirino.page.link")
                                            .setAndroidParameters(
                                                    new DynamicLink.AndroidParameters.Builder("net.k1ra.kirino")
                                                            .setMinimumVersion(1)
                                                            .build())
                                            .buildDynamicLink();  // Or buildShortDynamicLink()

                                    System.out.println("LINK IS " + dynamicLink.getUri());

                                    Intent share = new Intent(Intent.ACTION_SEND);
                                    share.setType("text/plain");


                                    share.putExtra(Intent.EXTRA_SUBJECT, media.getJSONObject("title").getString("romaji"));
                                    share.putExtra(Intent.EXTRA_TEXT, String.valueOf(dynamicLink.getUri()));

                                    startActivity(Intent.createChooser(share, getString(R.string.share)));
                                } catch (Exception e) {}
                            }
                        });


                    } catch (JSONException e) {}
                    break;
                case 20: //if dialog_seasonal_top
                    vh.year.setText(item.json.split("\\|")[1]);
                    final String[] seasons = getResources().getStringArray(R.array.seasons);
                    for (int j = 0; j < seasons.length; j++) { if (seasons[j].equals(item.json.split("\\|")[0])) {vh.season.setSelection(j);} }
                    vh.submit.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            String out = seasons[vh.season.getSelectedItemPosition()]+"|"+vh.year.getText().toString();
                            populate_seasonal(l, n, a, out);
                        }
                    });
                    break;
                default: //if character
                    try {
                        final JSONObject chara = new JSONObject(item.json).getJSONObject("data").getJSONObject("Character");
                        Tools.picasso.load(chara.getJSONObject("image").getString("large")).into(vh.img);
                        String name_text = "";
                        if (!chara.getJSONObject("name").getString("first").equals("null")) {
                            name_text = chara.getJSONObject("name").getString("first");
                        }
                        if (!chara.getJSONObject("name").getString("last").equals("null")) {
                            name_text += " " + chara.getJSONObject("name").getString("last");
                        }
                        vh.name.setText(name_text);
                        spoil = false;
                        vh.desc.setText(Tools.parse_description(chara.getString("description"), spoil));
                        vh.desc.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                try {
                                    spoil = !spoil;
                                    vh.desc.setText(Tools.parse_description(chara.getString("description"), spoil));
                                } catch (JSONException e) {}
                            }
                        });
                        //because java is retarded
                        final String chara_name_text = name_text;

                        //adapter for related list
                        final card_media_adapter media_adapter = new card_media_adapter();
                        media_adapter.nodes = chara.getJSONObject("media").getJSONArray("nodes");
                        vh.related.setAdapter(media_adapter);
                        LinearLayoutManager llm_m = new LinearLayoutManager(getActivity());
                        vh.related.setLayoutManager(llm_m);

                        //make a kirino.app link to share things to other app users
                        vh.share.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                try {
                                    DynamicLink dynamicLink = FirebaseDynamicLinks.getInstance().createDynamicLink()
                                            .setLink(Uri.parse("http://kirino.app/resolve.php?chara="+chara.getString("id")))
                                            .setDynamicLinkDomain("kirino.page.link")
                                            .setAndroidParameters(
                                                    new DynamicLink.AndroidParameters.Builder("net.k1ra.kirino")
                                                            .setMinimumVersion(1)
                                                            .build())
                                            .buildDynamicLink();  // Or buildShortDynamicLink()

                                    System.out.println("LINK IS " + dynamicLink.getUri());

                                    Intent share = new Intent(Intent.ACTION_SEND);
                                    share.setType("text/plain");


                                    share.putExtra(Intent.EXTRA_SUBJECT, chara_name_text);
                                    share.putExtra(Intent.EXTRA_TEXT, String.valueOf(dynamicLink.getUri()));

                                    startActivity(Intent.createChooser(share, getString(R.string.share)));
                                } catch (Exception e) {}
                            }
                        });

                        //handle adding to waifu list
                        vh.add.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                        c_DB = Room.databaseBuilder(getActivity(),
                                                character_DB.class, "waifu_db")
                                                .build();

                                            final Character to_add = new Character(chara.getString("id"),
                                                    chara.getJSONObject("image").getString("large"),
                                                    chara_name_text,
                                                    chara.getString("description"));

                                            try
                                            {
                                                to_add.media = chara.getJSONObject("media").getJSONArray("nodes").toString();
                                            } catch (JSONException e) {}

                                            if (c_DB.DAO().fetch(chara.getInt("id")) == null)
                                            {
                                                if (sharedPref.getBoolean("sync_w", true) && !sharedPref.getString("AL_token", "").equals(""))
                                                {
                                                    final NetworkResponse out = new NetworkResponse();
                                                    NetworkRequest.make(out, NetworkRequest.AniList.noStrict().Authenticated(), true, false, new Pair[]{Pair.create("query","mutation {\n" +
                                                            "  ToggleFavourite(characterId:"+to_add.ALid+")\n" +
                                                            "  {\n" +
                                                            "    characters {\n" +
                                                            "     pageInfo {\n" +
                                                            "       total\n" +
                                                            "     }\n" +
                                                            "    }\n" +
                                                            "  }\n" +
                                                            "  UpdateFavouriteOrder(characterIds:"+to_add.ALid+" characterOrder:100000000)\n" +
                                                            "  {\n" +
                                                            "    characters\n" +
                                                            "    {\n" +
                                                            "      pageInfo\n" +
                                                            "      {\n" +
                                                            "        total\n" +
                                                            "      }\n" +
                                                            "    }\n" +
                                                            "  }\n" +
                                                            "}")}, getActivity(), new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            new Thread(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    c_DB.DAO().insert_single(to_add);
                                                                    c_DB.close();
                                                                    getActivity().runOnUiThread(new Runnable() {
                                                                        @Override
                                                                        public void run() {
                                                                            Errors.toast(getActivity(), getString(R.string.added_to_list));
                                                                            getActivity().sendBroadcast(new Intent("net.k1ra.update.waifu"));
                                                                        }
                                                                    });
                                                                }
                                                            }).start();
                                                        }
                                                    }, null);
                                                }
                                                else {
                                                    c_DB.DAO().insert_single(to_add);
                                                    c_DB.close();
                                                    getActivity().runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            Errors.toast(getActivity(), getString(R.string.added_to_list));
                                                            getActivity().sendBroadcast(new Intent("net.k1ra.update.waifu"));
                                                        }
                                                    });
                                                }
                                            } else {
                                                c_DB.close();

                                                getActivity().runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Errors.toast(getActivity(), getString(R.string.already_in_list));
                                                    }
                                                });
                                            }
                                        } catch (JSONException e) {}
                                    }
                                }).start();
                            }
                        });

                    } catch (Exception e) {}
                    break;
            }
        }
        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }
    }

    @Override //TODO does not run??
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 3: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Tools.download(link, getActivity());
                } else {
                    Errors.toast(getActivity(), getString(R.string.permission_denied));
                }
                return;
            }

        }
    }

    class card_chara_adapter extends RecyclerView.Adapter<card_chara_adapter.CharaViewHolder>{
        JSONArray nodes;
        JSONArray edges;
        Dialog d = null;

        class CharaViewHolder extends RecyclerView.ViewHolder {
            ImageView img;
            TextView name;
            Button btn;

            CharaViewHolder(View itemView) {
                super(itemView);
                img = itemView.findViewById(R.id.chara_image);
                name = itemView.findViewById(R.id.chara_name);
                btn = itemView.findViewById(R.id.chara_button);
            }
        }
        @Override
        public int getItemViewType(int position) {
            return position;
        }
        @Override
        public int getItemCount() {
            return edges.length();
        }
        @Override
        public CharaViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.feed_card_media_chara, viewGroup, false);
            CharaViewHolder vh = new CharaViewHolder(v);
            return vh;
        }
        @Override
        public void onBindViewHolder(final CharaViewHolder vh, final int i) {
            try {
                String name_text = "";
                if (!nodes.getJSONObject(i).getJSONObject("name").getString("first").equals("null")) {
                    name_text = nodes.getJSONObject(i).getJSONObject("name").getString("first");
                }
                if (!nodes.getJSONObject(i).getJSONObject("name").getString("last").equals("null")) {
                    name_text += " " + nodes.getJSONObject(i).getJSONObject("name").getString("last");
                }

                vh.name.setText(name_text+" ("+edges.getJSONObject(i).getString("role")+")");
                Tools.picasso.load(nodes.getJSONObject(i).getJSONObject("image").getString("large")).into(vh.img);

                vh.btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            ToFeed(3, nodes.getJSONObject(i).getString("id"), null);
                            if (d != null) {d.dismiss();}
                        } catch (Exception e) {}
                    }
                });

            } catch (JSONException e) {}
        }
        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }
    }

    class card_media_adapter extends RecyclerView.Adapter<card_media_adapter.MediaViewHolder>{
        JSONArray nodes;
        JSONArray edges = null;
        Dialog d = null;

        class MediaViewHolder extends RecyclerView.ViewHolder {
            ImageView img;
            TextView name;
            TextView air_rating;
            TextView desc;
            CardView card;

            MediaViewHolder(View itemView) {
                super(itemView);
                img = itemView.findViewById(R.id.a_img);
                name = itemView.findViewById(R.id.a_name);
                air_rating = itemView.findViewById(R.id.a_air_rating);
                desc = itemView.findViewById(R.id.a_desc);
                card = itemView.findViewById(R.id.a_card_simple);
            }
        }
        @Override
        public int getItemViewType(int position) {
            return position;
        }
        @Override
        public int getItemCount() {
            return nodes.length();
        }
        @Override
        public MediaViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View v = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.dialog_search_card, viewGroup, false); //card is re-used
            MediaViewHolder vh = new MediaViewHolder(v);
            return vh;
        }
        @Override
        public void onBindViewHolder(final MediaViewHolder vh, final int i) {
            try {
                String airString = "";
                if (!nodes.getJSONObject(i).getJSONObject("startDate").getString("day").equals("null"))
                {
                    airString += nodes.getJSONObject(i).getJSONObject("startDate").getString("day")+"/";
                }
                if (!nodes.getJSONObject(i).getJSONObject("startDate").getString("month").equals("null"))
                {
                    airString += nodes.getJSONObject(i).getJSONObject("startDate").getString("month")+"/";
                }
                if (!nodes.getJSONObject(i).getJSONObject("startDate").getString("year").equals("null"))
                {
                    airString += nodes.getJSONObject(i).getJSONObject("startDate").getString("year");
                }

                Tools.picasso.load(nodes.getJSONObject(i).getJSONObject("coverImage").getString("large")).into(vh.img);
                vh.name.setText(nodes.getJSONObject(i).getJSONObject("title").getString("romaji"));
                if (edges != null) {
                    if (!nodes.getJSONObject(i).getString("episodes").equals("null")) {
                        vh.desc.setText("[" + edges.getJSONObject(i).getString("relationType").replace("_", " ").toLowerCase() + "] " + nodes.getJSONObject(i).getString("format") + " - " + nodes.getJSONObject(i).getString("episodes"));
                    } else {
                        vh.desc.setText("[" + edges.getJSONObject(i).getString("relationType").replace("_", " ").toLowerCase() + "] " + nodes.getJSONObject(i).getString("format"));
                    }
                }
                else
                {
                    if (!nodes.getJSONObject(i).getString("episodes").equals("null")) {
                        vh.desc.setText(nodes.getJSONObject(i).getString("format") + " - " + nodes.getJSONObject(i).getString("episodes"));
                    } else {
                        vh.desc.setText(nodes.getJSONObject(i).getString("format"));
                    }
                }
                if (!nodes.getJSONObject(i).getString("averageScore").equals("null")) {
                    if (airString.equals("")) {
                        vh.air_rating.setText(nodes.getJSONObject(i).getString("averageScore"));
                    } else {
                        vh.air_rating.setText(nodes.getJSONObject(i).getString("averageScore"));
                    }
                } else {
                    vh.air_rating.setText(airString);
                }

                //display on card tap
                vh.card.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                        ToFeed(2, nodes.getJSONObject(i).getString("id"), null);
                            if (d != null) {d.dismiss();}
                        } catch (Exception e) {}
                    }
                });
            } catch (JSONException e) {}
        }
        @Override
        public void onAttachedToRecyclerView(RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
        }
    }

    void seasonal_dialog()
    {
        final Dialog dialog = new Dialog(getActivity());
        dialog.setCancelable(true);
        dialog.setContentView(R.layout.dialog_seasonal);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        final RecyclerView list = dialog.findViewById(R.id.seasonal_list);
        final TextView none_found = dialog.findViewById(R.id.seasonal_none_found);
        final LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        final feed_adapter adapter = new feed_adapter();
        adapter.seasonal_list = true;
        adapter.l = list;
        adapter.n = none_found;
        adapter.d = dialog;
        list.setLayoutManager(llm);
        list.setAdapter(adapter);

        Calendar cal = new GregorianCalendar();
        String out = "";
        //get season
        if (cal.get(Calendar.MONTH) <= 2) {out = getResources().getStringArray(R.array.seasons)[0];}
        else if (cal.get(Calendar.MONTH) >=3 && cal.get(Calendar.MONTH) <=5) {out = getResources().getStringArray(R.array.seasons)[1];}
        else if (cal.get(Calendar.MONTH) >=6 && cal.get(Calendar.MONTH) <=8) {out = getResources().getStringArray(R.array.seasons)[2];}
        else if (cal.get(Calendar.MONTH) >=9 && cal.get(Calendar.MONTH) <=11) {out = getResources().getStringArray(R.array.seasons)[3];}
        out += "|"+cal.get(Calendar.YEAR);

        dialog.show();

        populate_seasonal(list, none_found, adapter, out);
    }

    void populate_seasonal(final RecyclerView list, final TextView none_found, final feed_adapter adapter, final String in)
    {
        adapter.seasonal = new ArrayList<FeedItem>();
        adapter.seasonal.add(new FeedItem(20, in));
        none_found.setVisibility(View.VISIBLE);

        for (int i = 1; i < 5; i++) //same deal as searching
        {
            final NetworkResponse out = new NetworkResponse();
            NetworkRequest.make(out, NetworkRequest.AniList.notAuthenticated().Strict(), true, false, NetworkRequest.make_AL_query(11, in, i), getActivity(), new Runnable() {
                @Override
                public void run() {
                    try {
                        final JSONArray data = out.obj.getJSONObject("data").getJSONObject("Page").getJSONArray("media");
                        for (int i = 0; i < data.length(); i++)
                        {
                            adapter.seasonal.add(new FeedItem(2, data.get(i).toString()));
                        }
                        if (adapter.seasonal.size() > 1) {none_found.setVisibility(View.INVISIBLE);}
                        list.setAdapter(adapter);
                    } catch (JSONException e) {}
                }
            }, null);
        }
    }

    void fetch_random()
    {
        final List<Integer> already_watched = new ArrayList<Integer>();

        if (!getActivity().getSharedPreferences("net.k1ra.kirino", Context.MODE_PRIVATE).getString("AL_token", "").equals("")) {
            fetch_random_stage_2(already_watched);
        } else {
            final NetworkResponse out = new NetworkResponse();
            NetworkRequest.make(out, NetworkRequest.AniList.Authenticated().Strict(), true, false, NetworkRequest.make_AL_query(12, "", 1, getActivity()), getActivity(), new Runnable() {
                @Override
                public void run() {
                    try {
                        final int total_pages = out.obj.getJSONObject("data").getJSONObject("Page").getJSONObject("pageInfo").getInt("lastPage");
                        final boolean[] completed = new boolean[total_pages];
                        completed[0] = true;
                        final JSONArray items = out.obj.getJSONObject("data").getJSONObject("Page").getJSONArray("mediaList");

                        for (int i = 0; i < items.length(); i++) {
                            already_watched.add(items.getJSONObject(i).getJSONObject("media").getInt("id"));
                        }

                        if (total_pages > 1) {
                            for (int i = 2; i <= total_pages; i++) {
                                final int i_f = i;
                                final NetworkResponse out = new NetworkResponse();
                                NetworkRequest.make(out, NetworkRequest.AniList.Authenticated().Strict(), true, false, NetworkRequest.make_AL_query(12, "", i_f, getActivity()), getActivity(), new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            completed[i_f - 1] = true;
                                            final JSONArray items = out.obj.getJSONObject("data").getJSONObject("Page").getJSONArray("mediaList");

                                            for (int i = 0; i < items.length(); i++) {
                                                already_watched.add(items.getJSONObject(i).getJSONObject("media").getInt("id"));
                                            }

                                            boolean done = true;
                                            for (int i = 0; i < completed.length; i++) {
                                                done = done && completed[i];
                                            }

                                            if (done) {
                                                fetch_random_stage_2(already_watched);
                                            }

                                        } catch (JSONException e) {
                                        }
                                    }
                                }, null);
                            }
                        } else {
                            fetch_random_stage_2(already_watched);
                        }
                    } catch (JSONException e) {
                    }
                }
            }, null);
        }
    }

    int num = 0;
    boolean unique = false;
    void fetch_random_stage_2(final List<Integer> list)
    {
        Loading.show(getActivity());

        new Thread(new Runnable() {
            @Override
            public void run() {
                final int MAX_ALID = 104472; //current maximum AniList ID
                num = 0;
                unique = false;

                while (!unique)
                {
                    unique = true;
                    num = new Random().nextInt(MAX_ALID-1)+1;

                    final NetworkResponse out = new NetworkResponse();
                    NetworkRequest.make(out, NetworkRequest.AniList.Authenticated().noStrict(), true, false, new Pair[]{Pair.create("query", "query {\n" +
                            "Media(id:"+num+")\n" +
                            "  {\n" +
                            "    id\n" +
                            "  }\n" +
                            "}")}, null, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                unique = out.obj.getJSONObject("data").getJSONObject("Media").getInt("id") == num;
                            } catch (JSONException e) {}
                        }
                    }, null, true);

                    for (int i = 0; i < list.size(); i++)
                    {
                        if (num == list.get(i)) { unique = false; break; }
                    }
                }

                Loading.dismiss();
                ToFeed(2, String.valueOf(num), null);
            }
        }).start();
    }
}
